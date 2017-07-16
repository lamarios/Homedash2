package com.ftpix.homedash.websocket;

import com.ftpix.homedash.models.ModuleLayout;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Created by gz on 7/16/17.
 */
@WebSocket(maxTextMessageSize = Integer.MAX_VALUE)
public class KioskWebSocket extends SingleModuleWebSocket {
    public KioskWebSocket() {
        super(ModuleLayout.KIOSK);
    }
}
