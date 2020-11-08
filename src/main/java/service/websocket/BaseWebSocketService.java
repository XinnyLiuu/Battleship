package service.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.HttpCookie;
import java.rmi.UnexpectedException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@WebSocket
public abstract class BaseWebSocketService {
    public static final String BROADCAST_USER_TYPE = "USER";
    public static final String BROADCAST_SYSTEM_TYPE = "SYSTEM";

    public static final String JSON_MESSAGE_TYPE = "type";
    public static final String JSON_MESSAGE_TEXT = "message";
    public static final String JSON_MESSAGE_SENDER = "sender";
    public static final String JSON_MESSAGE_COLOR = "color";
    public static final String JSON_MESSAGE_USERS = "users";
    public static final String JSON_MESSAGE_GAME_ROOM_ID = "gameRoomId";

    public static final String CLIENT_CHAT_TYPE = "CHAT";

    public static final long SESSION_TIMEOUT = 3600000; // Session timeout 1 hr
    public static final String SESSION_ID = "JSESSIONID";
    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_CHAT_COLOR = "chatColor";
    public static final String SESSION_GAME_ROOM_ID = "gameRoomId";

    public final Map<String, String> sessionIdUsernameMap = new ConcurrentHashMap<>();

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
     * Returns a map of all the key - value pairs in the Session
     *
     * @param session
     * @return
     */
    public Map<String, String> getSessionCookies(Session session) {
        return session.getUpgradeRequest().getCookies().stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));
    }

    /**
     * Returns the timestamp in 2016-11-16 06:43:19 format
     *
     * @return
     */
    public String getTimestamp() {
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();
        timestamp = timestamp.substring(0, timestamp.indexOf("."));
        return timestamp;
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
