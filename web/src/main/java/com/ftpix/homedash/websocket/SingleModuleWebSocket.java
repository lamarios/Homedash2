package com.ftpix.homedash.websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gz on 07-Jun-16.
 */
@WebSocket(maxTextMessageSize = Integer.MAX_VALUE)
public class SingleModuleWebSocket {
    private Map<Session, InnerSocketClass> sessions = new HashMap<>();
    private Logger logger = LogManager.getLogger();

    private final boolean fullScreen;

    public SingleModuleWebSocket(boolean fullScreen) {
        this.fullScreen = fullScreen;
    }


    @OnWebSocketConnect
    public void connected(Session session) {

        sessions.put(session, new InnerSocketClass(session, fullScreen));
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


}
