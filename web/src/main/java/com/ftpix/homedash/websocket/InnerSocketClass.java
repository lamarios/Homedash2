package com.ftpix.homedash.websocket;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.WebSocketMessage;
import com.ftpix.homedash.plugins.Plugin;
import com.google.gson.Gson;
import io.gsonfire.GsonFireBuilder;
import org.eclipse.jetty.websocket.api.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Inner class, seems like spark can only have one instance per websocket instance so we create
 * a new one each session
 */
public class InnerSocketClass {
    protected Logger logger = LogManager.getLogger();
    protected Session session;
    protected boolean refresh = false;
    protected long time = 0;
    protected Gson gson = new GsonFireBuilder().enableExposeMethodResult().createGson();
    protected ExecutorService exec;
    private final String SIZE;
    private int moduleId;


    InnerSocketClass(Session session, String size) {
        this.session = session;
        SIZE = size;
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
     */
    protected void sendCommandToModule(WebSocketMessage message) {
        WebSocketMessage response = new WebSocketMessage();
        Plugin plugin = null;
        try {
            plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(message.getModuleId());
            response = plugin.processIncomingCommand(message.getCommand(), message.getMessage().toString(), message.getExtra());
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
    protected void refreshModule() {
        while (refresh) {
            logger.info("Refreshing module");
            try {

                try {
                    logger.info("Refreshing plugin [{}]", moduleId);

                    WebSocketMessage response = MainWebSocket.refreshSingleModule(this.moduleId, SIZE);

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
                Plugin plugin = PluginModuleMaintainer.INSTANCE.getPluginForModule(this.moduleId);
                Thread.sleep(plugin.getRefreshRate(SIZE) * 1000);
                time += plugin.getRefreshRate(SIZE);
            } catch (Exception e) {
                logger.error("Error while sleeping", e);
            }

        }
    }

    /**
     * Start refreshing the modules
     */
    protected void startRefresh() throws Exception {
        //we will start refresh only if at least one of the clients has a page
        stopRefresh();

        PluginModuleMaintainer.INSTANCE.getPluginForModule(moduleId).increaseClients();

        logger.info("clients are ready");

        if (!refresh && exec == null) {
            logger.info("Start refresh of modules");
            refresh = true;

            exec = Executors.newSingleThreadExecutor();

            exec.execute(this::refreshModule);
        }
    }


    /**
     * Stop the refreshing madness
     */
    protected void stopRefresh() {
        try {
            if (exec != null) {
                refresh = false;

                logger.info("Stopping refresh of modules");
                exec.shutdownNow();
                logger.info("WAITING TO SHUTDOWN");
//                exec.awaitTermination(10, TimeUnit.MINUTES);
                logger.info("FINALLY STOPPED");
                exec = null;
                time = 0;

                PluginModuleMaintainer.INSTANCE.getPluginForModule(moduleId).decreaseClients();

            }
        } catch (Exception e) {
            logger.error("Error while shutting down pool", e);
        }
    }


    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getModuleId() {
        return moduleId;
    }

    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }
}


