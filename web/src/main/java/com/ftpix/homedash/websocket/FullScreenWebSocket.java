package com.ftpix.homedash.websocket;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Created by gz on 7/16/17.
 */
@WebSocket(maxTextMessageSize = Integer.MAX_VALUE)
public class FullScreenWebSocket extends SingleModuleWebSocket {
    public FullScreenWebSocket() {
        super(true);
    }
}
