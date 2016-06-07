package com.ftpix.homedash.websocket;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.db.DB;
import com.ftpix.homedash.models.Layout;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.models.WebSocketSession;
import com.ftpix.homedash.plugins.Plugin;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by gz on 07-Jun-16.
 */
@WebSocket
public class FullScreenWebSocket {
    private Map<Session, InnerSocketClass> sessions = new HashMap<>();
    private Logger logger = LogManager.getLogger();

    @OnWebSocketConnect
    public void connected(Session session) {

        sessions.put(session, new InnerSocketClass(session));
        logger.info("New Client ! [{}]", session.getLocalAddress().getHostString());
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {

        sessions.get(session).stopRefresh();
        sessions.remove(sessions);

        try {
            session.disconnect();
            session.close();
            logger.info("Full screen client left");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        sessions.get(session).processMessage(message);
    }


    /**
     * Inner class, seems like spark can only have one instance per websocket instance so we create a new one each session
     */
    private class InnerSocketClass {
        private Logger logger = LogManager.getLogger();
        private Session session;
        private boolean refresh = false;
        private long time = 0;
        private Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGson();
        private ExecutorService exec;
        private final int THREADS_COUNT = 1;

        private int moduleId;


        InnerSocketClass(Session session) {
            this.session = session;
        }

        public void processMessage(String message) {
            try {
                logger.info("Received Message [{}]", message);


                WebSocketMessage socketMessage = gson.fromJson(message, WebSocketMessage.class);

                switch (socketMessage.getCommand()) {
                    case WebSocketMessage.COMMAND_REFRESH:
                        WebSocketMessage response = MainWebSocket.refreshSingleModule(socketMessage.getModuleId(), (String) socketMessage.getMessage());
                        final String jsonResponse = gson.toJson(response);
                        session.getRemote().sendString(jsonResponse);
                        break;
                    case WebSocketMessage.COMMAND_SET_MODULE:
                        this.moduleId = socketMessage.getModuleId();
                        startRefresh();
                        break;
                    default: // send the command to the module concerned
                        sendCommandToModule(socketMessage);
                }
            } catch (Exception e) {
                logger.error("Error while receiving command:", e);
            }
        }

        /**
         * Send a command to a module
         *
         * @param message
         */
        private void sendCommandToModule(WebSocketMessage message) {
            WebSocketMessage response = new WebSocketMessage();
            Plugin plugin = null;
            try {
                plugin = PluginModuleMaintainer.getPluginForModule(message.getModuleId());
                response = plugin.processCommand(message.getCommand(), message.getMessage().toString(), message.getExtra());
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
                session.getRemote().sendString(gson.toJson(response));
            } catch (IOException e) {
                logger.error("Errror while sending response", e);
            }

        }


        /**
         * Refresh all the modules
         */
        private void refreshModule() {
            while (refresh) {
                logger.info("Refreshing module");
                try {

                    try {
                        logger.info("Refreshing plugin [{}]", moduleId);

                        WebSocketMessage response = MainWebSocket.refreshSingleModule(this.moduleId, ModuleLayout.FULL_SCREEN);

                        final String jsonResponse = gson.toJson(response);
                        logger.info("session:{}, response:[{}]", session, jsonResponse);

                        boolean done = this.session.getRemote().sendStringByFuture(jsonResponse).isDone();
                        logger.info("Sending to client {}, isdone ? {}", jsonResponse, done);
                    } catch (Exception e) {
                        logger.error("Errror while sending response", e);
                    }


                } catch (Exception e) {
                    logger.error("Can't refresh module #" + moduleId, e);
                }

                try {
                    Thread.sleep(1000);
                    time++;
                } catch (Exception e) {
                    logger.error("Error while sleeping", e);
                }

            }
        }

        /**
         * Start refreshing the modules
         */
        private void startRefresh() {
            //we will start refresh only if at least one of the clients has a page
            stopRefresh();


            logger.info("clients are ready");

            if (!refresh && exec == null) {
                logger.info("Start refresh of modules");
                refresh = true;

                exec = Executors.newSingleThreadExecutor();

                exec.execute(new Runnable() {

                    @Override
                    public void run() {
                        refreshModule();
                    }
                });
            }
        }


        /**
         * Stop the refreshing madness
         */
        private void stopRefresh() {
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
            } catch (Exception e) {
                logger.error("Error while shutting down pool", e);
            }
        }
    }
}
