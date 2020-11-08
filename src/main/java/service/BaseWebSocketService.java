package service;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.rmi.UnexpectedException;

@WebSocket
public abstract class BaseWebSocketService {
    public static final String BROADCAST_USER_TYPE = "USER";
    public static final String BROADCAST_SYSTEM_TYPE = "SYSTEM";
    public static final long TIMEOUT = 3600000; // Session timeout 1 hr
    public static final String SESSION_ID = "JSESSIONID";
    public static final String USERNAME = "username";
    public static final String CHAT_COLOR = "chatColor";
    private final Logger logger;
    public BaseWebSocketService(Logger logger) {
        this.logger = logger;
    }

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
        logger.error(error.getMessage());
    }

    /**
     * Check that the session is still opened, otherwise have the websocket return an error to the client
     *
     * @param session
     */
    public void checkSessionOpened(Session session) {
        if (!session.isOpen()) {
            onError(new UnexpectedException("The requested user could not be found"));
        }
    }

    /**
     * Sends JSON back to the user
     *
     * @param session
     * @param json
     */
    public void sendJsonToSession(Session session, JSONObject json) {
        try {
            session.getRemote().sendString(String.valueOf(json));
        } catch (Exception e) {
            logger.error(e.getMessage());
            onError(e);
        }
    }
}
