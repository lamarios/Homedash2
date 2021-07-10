package com.ftpix.homedash.websocket;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.app.controllers.ModuleController;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.models.WebSocketSession;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.Predicates;
import com.google.gson.Gson;
import io.gsonfire.GsonFireBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@WebSocket
public class MainWebSocket {

    private static final ExecutorService exec = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    private static final ExecutorService commandProcessor = Executors.newCachedThreadPool();

    private final List<WebSocketSession> sessions = Collections.synchronizedList(new ArrayList<>());
    private final Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE).serializeSpecialFloatingPointValues().create();
    protected Logger logger = LogManager.getLogger();
    private boolean refresh = false;
    private long time = 0;

    public MainWebSocket() {

    }

    /**
     * Refresh a single module
     */
    public static WebSocketMessage refreshSingleModule(int moduleId, boolean fullScreen) throws Exception {

        Plugin plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(moduleId);
        WebSocketMessage response = plugin.refreshPlugin(fullScreen);

        return response;

    }

    @OnWebSocketConnect
    public void connected(Session session) throws Exception {
        Optional<WebSocketSession> client = getClientFromSession(session);
        if (!client.isPresent()) {
            stopRefresh();
            WebSocketSession newClient = new WebSocketSession();
            newClient.setSession(session);
            sessions.add(newClient);
            PluginModuleMaintainer.INSTANCE.getAllPluginInstances().forEach(Plugin::increaseClients);
            logger.info("New Client !, We now have {} clients", sessions.size());

        } else {
            logger.info("Seems that this client already exists");
        }
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        getClientFromSession(session).ifPresent(client -> {
            sessions.remove(client);
            try {
                PluginModuleMaintainer.INSTANCE.getAllPluginInstances().forEach(Plugin::decreaseClients);
            } catch (Exception e) {
                logger.error("Couldn't decrease the number of clients");
            }
            if (sessions.isEmpty()) {
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
        });

    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        commandProcessor.execute(() -> {
            try {
                logger.info("Received Message [{}]", message);

                Optional<WebSocketSession> optClient = getClientFromSession(session);

                if (optClient.isPresent()) {
                    WebSocketSession client = optClient.get();
                    WebSocketMessage socketMessage = gson.fromJson(message, WebSocketMessage.class);

                    switch (socketMessage.getCommand()) {
                        case WebSocketMessage.COMMAND_REFRESH:
                            WebSocketMessage response = refreshSingleModule(socketMessage.getModuleId(), Boolean.parseBoolean((String) socketMessage.getMessage()));
                            final String jsonResponse = gson.toJson(response);
                            client.getSession().getRemote().sendString(jsonResponse);
                            break;
                        case WebSocketMessage.COMMAND_CHANGE_PAGE:
                            client.setPage(DB.PAGE_DAO.queryForId(Double.valueOf(socketMessage.getMessage().toString()).intValue()));
                            logger.info("New page for client: [{}]", client.getPage().getName());
                            time = 0;
                            startRefresh();
                            break;
                        default: // send the command to the module concerned
                            sendCommandToModule(client, socketMessage);
                    }
                }
            } catch (Exception e) {
                logger.error("Error while receiving command:", e);
            }
        });

    }

    /**
     * Send a command to a module
     */
    private void sendCommandToModule(WebSocketSession session, WebSocketMessage message) {
        WebSocketMessage response = new WebSocketMessage();
        Plugin plugin = null;
        try {
            plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(message.getModuleId());

            response = plugin.processIncomingCommand(message.getCommand(), message.getMessage().toString(), message.getExtra());
            response.setModuleId(plugin.getModule().getId());
        } catch (Exception e) {
            logger.error("Error while processing the command", e);
            if (plugin != null) {
                response.setMessage("Error while refreshing " + plugin.getDisplayName() + ": " + e.getMessage());
            } else {
                response.setMessage("Error while processing the command:" + e);
            }
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
        }

        try {
            String gsonResponse = gson.toJson(response);
            logger.info("Sending response to command");
            session.getSession().getRemote().sendString(gson.toJson(response));
        } catch (IOException e) {
            logger.error("Errror while sending response", e);
        }

    }

    /**
     * Refresh all the modules
     */
    private void refreshModules() {
        while (refresh) {

            //sometimes there are no client and it's still running
            if (sessions.size() > 0) {
                logger.info("Refreshing modules");
                List<Module> moduleLayouts = getModuleLayoutsToRefresh();

                moduleLayouts.forEach(ml -> {


                    try {
                        // Getting the data to send
                        Plugin plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(ml);
                        if (plugin.getRefreshRate(false) > Plugin.NEVER && time % plugin.getRefreshRate(false) == 0) {

                            exec.execute(() -> {
                                try {
                                    logger.info("Refreshing plugin [{}]", plugin.getId());

                                    WebSocketMessage response = refreshSingleModule(ml.getId(), false);

                                    String jsonResponse = gson.toJson(response);

                                    sendMessage(jsonResponse, ml);

                                } catch (Exception e) {
                                    logger.error("Can't refresh module #" + ml.getId(), e);
                                }

                            });

                        }

                    } catch (Exception e) {
                        logger.error("Couldn't refresh module", e);
                    }
                });

                try {
                    Thread.sleep(1000);
                    time++;
                } catch (Exception e) {
                    logger.error("Error while sleeping", e);
                }
            } else {
                stopRefresh();
            }

        }
    }

    /**
     * Find all the module layouts to refresh based on the clients connected
     */
    private List<Module> getModuleLayoutsToRefresh() {

        return sessions.stream()
                .filter(s -> s.getPage() != null)
                .flatMap(s -> {
                    try {
                        logger.info("Getting module layout for settings page:[{}]", s.getPage().getName());
                        return ModuleController.INSTANCE.getForPage(s.getPage()).stream();
                    } catch (Exception e) {
                        logger.error("Can't get layouts for page:[" + s.getPage().getId() + "]", e);
                        return new ArrayList<Module>().stream();
                    }
                })
                .filter(Predicates.distinctByKey(Module::getId))
                .collect(Collectors.toList());
    }

    /**
     * Start refreshing the modules
     */
    private void startRefresh() throws Exception {
        //we will start refresh only if at least one of the clients has a page
        stopRefresh();

        long readyClients = sessions.stream()
                .filter(s -> s.getPage() != null)
                .count();

        logger.info("{}/{} clients are ready", readyClients, sessions.size());

        if (!refresh && readyClients > 0) {
            logger.info("Start refresh of modules");
            refresh = true;

            exec.execute(this::refreshModules);
        }
    }

    private ExecutorService createExecutionPool() throws Exception {
        return Executors.newFixedThreadPool(PluginModuleMaintainer.INSTANCE.getAllPluginInstances().size() + 1);
    }

    /**
     * Stop the refreshing madness
     */
    private void stopRefresh() {
        refresh = false;

        time = 0;
    }

    /**
     * Gets a WebSocket session via the session (usually check the hash
     */
    private Optional<WebSocketSession> getClientFromSession(Session session) {

        return sessions.stream()
                .filter(s -> s.equals(session))
                .findFirst();
    }


    /**
     * Sends a message to clients
     */
    public void sendMessage(String message, Module ml) {
        sessions.stream().filter(s -> {
            try {
                return (s.getPage() != null)
                        && (s.getPage().getId() == ml.getPage().getId())
                        && s.getSession().isOpen();
            } catch (Exception e) {
                logger.error("Error while checking client", e);
                return false;
            }
        }).forEach(s -> {
            try {
                boolean done = s.getSession().getRemote().sendStringByFuture(message).isDone();
//                logger.info("Sending to client {}, isdone ? {}", message, done);
            } catch (Exception e) {
                logger.error("Errror while sending response", e);
            }
        });
    }
}
