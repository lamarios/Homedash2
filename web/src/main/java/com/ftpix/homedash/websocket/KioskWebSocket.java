package com.ftpix.homedash.websocket;

import com.ftpix.homedash.app.PluginModuleMaintainer;
import com.ftpix.homedash.models.Module;
import com.ftpix.homedash.models.ModuleLayout;
import com.ftpix.homedash.models.WebSocketMessage;
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
import sun.security.pkcs11.Secmod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.maven.doxia.sink.SinkEventAttributes.SIZE;

/**
 * Created by gz on 7/16/17.
 */

@WebSocket(maxTextMessageSize = Integer.MAX_VALUE)
public class KioskWebSocket {

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
     * Inner class, seems like spark can only have one instance per websocket instance so we create
     * a new one each session
     */
    private class InnerSocketClass extends com.ftpix.homedash.websocket.InnerSocketClass {
        private List<Plugin> kioskPlugins = new ArrayList<>();

        public InnerSocketClass(Session session) {
            super(session, ModuleLayout.KIOSK);
            this.session = session;

            try {
                kioskPlugins = PluginModuleMaintainer.getInstance().getAllPluginInstances()
                        .stream()
                        .filter(p -> Stream.of(p.getSizes()).anyMatch(s -> s.equalsIgnoreCase(ModuleLayout.KIOSK)))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Couldn't get KIOSK modules");
            }
        }





    }
}
