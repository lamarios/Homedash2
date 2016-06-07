package com.ftpix.homedash.websocket;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.app.controllers.ModuleLayoutController;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.models.WebSocketSession;
import com.ftpix.homedash.plugins.Plugin;
import com.ftpix.homedash.utils.Predicates;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.google.gson.Gson;

import io.gsonfire.GsonFireBuilder;

@WebSocket
public class MainWebSocket {

    private static final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    protected static Logger logger = LogManager.getLogger();
    private static boolean refresh = false;
    private static long time = 0;
    private static Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGson();
    private static ExecutorService exec;
    private static final int THREADS_COUNT = 1;

    @OnWebSocketConnect
    public void connected(Session session) {
        WebSocketSession client = getClientFromSession(session);
        if (client == null) {
            stopRefresh();
            client = new WebSocketSession();
            client.setSession(session);
            sessions.add(client);
            logger.info("New Client !, We now have {} clients", sessions.size());

        } else {
            logger.info("Seems that this client already exists");
        }
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        WebSocketSession client = getClientFromSession(session);
        sessions.remove(client);

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

    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        try {
            logger.info("Received Message [{}]", message);

            WebSocketSession client = getClientFromSession(session);

            if (client != null) {

                WebSocketMessage socketMessage = gson.fromJson(message, WebSocketMessage.class);

                switch (socketMessage.getCommand()) {
                    case WebSocketMessage.COMMAND_REFRESH:
                        WebSocketMessage response = refreshSingleModule(socketMessage.getModuleId(), (String) socketMessage.getMessage());
                        final String jsonResponse = gson.toJson(response);
                        client.getSession().getRemote().sendString(jsonResponse);
                        break;
                    case WebSocketMessage.COMMAND_CHANGE_PAGE:
                        client.setPage(DB.PAGE_DAO.queryForId(Double.valueOf(socketMessage.getMessage().toString()).intValue()));
                        logger.info("New page for client: [{}]", client.getPage().getName());
                        time = 0;
                        startRefresh();
                        break;
                    case WebSocketMessage.COMMAND_CHANGE_LAYOUT:
                        Layout layout = DB.LAYOUT_DAO.queryForId(Double.valueOf(socketMessage.getMessage().toString()).intValue());
                        client.setLayout(layout);
                        logger.info("New layout for client: [{}]", client.getLayout().getName());
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

    }

    /**
     * Refresh a single module
     *
     * @param moduleId
     * @return
     */
    private static WebSocketMessage refreshSingleModule(int moduleId, String size) throws Exception {
        WebSocketMessage response = new WebSocketMessage();
        response.setCommand(WebSocketMessage.COMMAND_REFRESH);
        response.setModuleId(moduleId);

        Plugin plugin = PluginModuleMaintainer.getPluginForModule(moduleId);

        try {


            response.setMessage(plugin.refreshPlugin(size));
            plugin.saveData();
            DB.MODULE_DAO.update(plugin.getModule());
        } catch (Exception e) {
            logger.error("Error while refreshing " + plugin.getDisplayName(), e);
            response.setCommand(WebSocketMessage.COMMAND_ERROR);
            response.setMessage("Error while refreshing " + plugin.getDisplayName() + ": " + e.getMessage());
        }

        return response;

    }

    /**
     * Send a command to a module
     *
     * @param session
     * @param message
     */
    private static void sendCommandToModule(WebSocketSession session, WebSocketMessage message) {
        WebSocketMessage response = new WebSocketMessage();
        Plugin plugin = null;
        try {
            plugin = PluginModuleMaintainer.getPluginForModule(message.getModuleId());
           response =  plugin.processCommand(message.getCommand(), message.getMessage().toString(), message.getExtra());
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
            logger.info("Sending response: [{}]", gsonResponse);
            session.getSession().getRemote().sendString(gson.toJson(response));
        } catch (IOException e) {
            logger.error("Errror while sending response", e);
        }


    }

    /**
     * Refresh all the modules
     */
    private static void refreshModules() {
        while (refresh) {
            logger.info("Refreshing modules");
            List<ModuleLayout> moduleLayouts = getModuleLayoutsToRefresh();

            moduleLayouts.forEach(ml -> {
                try {
                    // Getting the data to send
                    Plugin plugin = PluginModuleMaintainer.getPluginForModule(ml.getModule());
                    if (plugin.getRefreshRate() > Plugin.NEVER && time % plugin.getRefreshRate() == 0) {

                        logger.info("Refreshing plugin [{}] for layout[{}]", plugin.getId(), ml.getLayout().getName());

                        WebSocketMessage response = refreshSingleModule(ml.getModule().getId(), ml.getSize());

                        final String jsonResponse = gson.toJson(response);

                        // finding which clients to send to and sending
                        // the
                        // message
                        sessions.stream().filter(s -> {
                            try {
                                return (s.getLayout() != null)
                                        && (s.getPage() != null)
                                        && (s.getLayout().getId() == ml.getLayout().getId())
                                        && (s.getPage().getId() == ml.getModule().getPage().getId())
                                        && s.getSession().isOpen()
                                        ;
                            } catch (Exception e) {
                                logger.error("Error while checking client", e);
                                return false;
                            }
                        }).forEach(s -> {
                            try {
                                boolean done = s.getSession().getRemote().sendStringByFuture(jsonResponse).isDone();
                                logger.info("Sending to client {}, isdone ? {}", jsonResponse, done);
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
                logger.error("Error while sleeping", e);
            }

        }
    }

    /**
     * Find all the module layouts to refresh based on the clients connected
     *
     * @return
     */
    private static List<ModuleLayout> getModuleLayoutsToRefresh() {
        List<ModuleLayout> layouts = new ArrayList<>();

        sessions.stream().filter(s -> s.getLayout() != null && s.getPage() != null).forEach(s -> {
            try {
                logger.info("Getting module layout for settings page:[{}], Layout[{}]", s.getPage().getName(), s.getLayout().getName());
                layouts.addAll(ModuleLayoutController.getInstance().generatePageLayout(s.getPage(), s.getLayout()));
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
    private static void startRefresh() {
        //we will start refresh only if at least one of the clients has a page
        stopRefresh();


        long readyClients = sessions.stream().filter(s -> {
            return s.getLayout() != null && s.getPage() != null;
        }).count();

        logger.info("{}/{} clients are ready", readyClients, sessions.size());

        if (!refresh && exec == null && readyClients > 0) {
            logger.info("Start refresh of modules");
            refresh = true;

            exec = Executors.newSingleThreadExecutor();

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
    private static void stopRefresh() {
        try {
            if (exec != null) {
                refresh = false;

                logger.info("Stopping refresh of modules");
                exec.shutdown();
                logger.info("WAITING TO SHUTDOWN");
                exec.awaitTermination(10, TimeUnit.MINUTES);
                logger.info("FINALLY STOPPED");
                exec = null;
                time = 0;

            }
        }catch(Exception e){
            logger.error("Error while shutting down pool", e);
        }
    }

    /**
     * Gets a WebSocket session via the session (usually check the hash
     *
     * @param session
     * @return
     */
    private static WebSocketSession getClientFromSession(Session session) {
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
