package com.ftpix.homedash.websocket;

import static com.ftpix.homedash.websocket.models.WebSocketMessage.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.app.controllers.LayoutController;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.Predicates;
import com.ftpix.homedash.websocket.models.WebSocketMessage;
import com.ftpix.homedash.websocket.models.WebSocketSession;
import com.google.gson.Gson;

import io.gsonfire.GsonFireBuilder;

@WebSocket
public class MainWebSocket {

	private List<WebSocketSession> sessions = new ArrayList<>();
	protected Logger logger = LogManager.getLogger();
	private boolean refresh = false;
	private long time = 0;
	private Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGson();
	private ExecutorService exec;
	private final int THREADS_COUNT = 1;

	@OnWebSocketConnect
	public void connected(Session session) {
		WebSocketSession client = getClientFromSession(session);
		if (client == null) {
			client = new WebSocketSession();
			client.setSession(session);
			sessions.add(client);
			logger.info("New Client !, We now have {} clients", sessions.size());

			startRefresh();
		} else {
			logger.info("Seems that this client already exists");
		}
	}

	@OnWebSocketClose
	public void closed(Session session, int statusCode, String reason) {
		WebSocketSession client = getClientFromSession(session);
		sessions.remove(client);

		if (!sessions.isEmpty()) {
			stopRefresh();
		}
		try {
			session.disconnect();
			session.close();
			logger.info("A client left, {} clients left, continue refresh ? {}", sessions.size(), refresh);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		try {
			logger.info("Received Message [{}]", message);

			WebSocketSession client = getClientFromSession(session);

			if (client != null) {

				WebSocketMessage socketMessage = gson.fromJson(message, WebSocketMessage.class);

				switch (socketMessage.getCommand()) {
				case COMMAND_CHANGE_PAGE:
					client.setPage(DB.PAGE_DAO.queryForId(Double.valueOf(socketMessage.getMessage().toString()).intValue()));
					logger.info("New page for client: [{}]", client.getPage().getName());
					time = 0;
					break;
				case COMMAND_CHANGE_LAYOUT:
					Layout layout = DB.LAYOUT_DAO.queryForId(Double.valueOf(socketMessage.getMessage().toString()).intValue());
					client.setLayout(layout);
					logger.info("New layout for client: [{}]", client.getLayout().getName());
					time = 0;
					break;
				default: // send the command to the module concerned
					sendCommandToModule(client, socketMessage);
				}
			}
		} catch (Exception e) {
			logger.error("Error while receiving command:", e);
		}

	}

	/**
	 * Send a command to a module
	 * 
	 * @param session
	 * @param message
	 */
	private void sendCommandToModule(WebSocketSession session, WebSocketMessage message) {
		WebSocketMessage response = new WebSocketMessage();
		Plugin plugin = null;
		try {
			plugin = PluginModuleMaintainer.getPluginForModule(message.getModuleId());
		} catch (Exception e) {
			logger.error("Error while processing the command", e);
			if (plugin != null) {
				response.setMessage("Error while refreshing " + plugin.getDisplayName() + ": " + e.getMessage());
			} else {
				response.setMessage("Error while processing the command:" + e);
			}
			response.setCommand(COMMAND_ERROR);
			try {
				session.getSession().getRemote().sendString(gson.toJson(response));
			} catch (IOException e1) {
				logger.error("Errror while sending response", e);
			}
		}

	}

	/**
	 * Refresh all the modules
	 */
	private void refreshModules() {
		while (refresh) {
			logger.info("Refreshing modules");
			List<ModuleLayout> moduleLayouts = getModuleLayoutsToRefresh();

			moduleLayouts.forEach(ml -> {
				try {
					// Getting the data to send
					Plugin plugin = PluginModuleMaintainer.getPluginForModule(ml.getModule());
					if (plugin.getRefreshRate() > Plugin.NEVER && time % plugin.getRefreshRate() == 0) {

						logger.info("Refreshing plugin [{}] for layout[{}]", plugin.getId(), ml.getLayout().getName());

						WebSocketMessage response = new WebSocketMessage();
						response.setCommand(COMMAND_REFRESH);
						response.setModuleId(ml.getModule().getId());
						try {
							response.setMessage(plugin.refreshPlugin(ml.getSize()));
							plugin.saveData();
							DB.MODULE_DAO.update(plugin.getModule());
						} catch (Exception e) {
							response.setCommand(COMMAND_ERROR);
							response.setMessage("Error while refreshing " + plugin.getDisplayName() + ": " + e.getMessage());
						}

						final String jsonResponse = gson.toJson(response);

						// finding which clients to send to and sending
						// the
						// message
						sessions.stream().filter(s -> {
							return s.getLayout().getId() == ml.getLayout().getId() && s.getPage().getId() == ml.getModule().getPage().getId();
						}).forEach(s -> {
							try {
								s.getSession().getRemote().sendString(jsonResponse);
								logger.info("Sending to client {}", jsonResponse);
							} catch (Exception e) {
								logger.error("Errror while sending response", e);
							}
						});

					}
				} catch (Exception e) {
					logger.error("Can't refresh module #" + ml.getModule().getId(), e);
				}
			});

			try {
				Thread.sleep(1000);
				time++;
			} catch (Exception e) {
				logger.info("Error while sleeping");
			}

		}
	}

	/**
	 * Find all the module layouts to refresh based on the clients connected
	 * 
	 * @return
	 */
	private List<ModuleLayout> getModuleLayoutsToRefresh() {
		List<ModuleLayout> layouts = new ArrayList<>();
		LayoutController controller = new LayoutController();

		sessions.stream().filter(s -> s.getLayout() != null && s.getPage() != null).forEach(s -> {
			try {
				logger.info("Getting module layout for settings page:[{}], Layout[{}]", s.getPage().getName(), s.getLayout().getName());
				layouts.addAll(controller.generatePageLayout(s.getPage(), s.getLayout()));
			} catch (Exception e) {
				logger.error("Can't get layouts for page:[" + s.getPage().getId() + "], layout [" + s.getLayout().getName() + "]", e);
			}
		});

		List<ModuleLayout> layoutsToServe = layouts.stream().filter(Predicates.distinctByKey(l -> l.getId())).collect(Collectors.toList());
		logger.info("We have {} module layouts to refresh", layoutsToServe.size());

		return layoutsToServe;
	}

	/**
	 * Start refreshing the modules
	 */
	private void startRefresh() {
		if (!refresh) {
			logger.info("Start refresh of modules");
			refresh = true;

			exec = Executors.newFixedThreadPool(THREADS_COUNT);

			exec.execute(new Runnable() {

				@Override
				public void run() {
					refreshModules();
				}
			});
		}
	}

	/**
	 * Stop the refreshing madness
	 */
	private void stopRefresh() {
		logger.info("Stopping refresh of modules");
		refresh = false;
		exec.shutdownNow();
		exec = null;
		time = 0;
	}

	/**
	 * Gets a WebSocket session via the session (usually check the hash
	 * 
	 * @param session
	 * @return
	 */
	private WebSocketSession getClientFromSession(Session session) {
		WebSocketSession client = null;
		for (WebSocketSession s : sessions) {
			if (s.equals(session)) {
				client = s;
				break;
			}
		}

		return client;
	}
}
