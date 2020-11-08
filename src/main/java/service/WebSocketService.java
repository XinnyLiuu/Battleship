package service;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public abstract class WebSocketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketService.class);

    @OnWebSocketConnect
    public void onConnect(Session session) {
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        LOGGER.error(error.getMessage());
    }
}
