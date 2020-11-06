import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpCookie;
import java.sql.Timestamp;
import java.util.Map;
import java.util.stream.Collectors;

@WebSocket
public class WaitingRoomService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingRoomService.class);

    public static final String USER_BROADCAST_TYPE = "USER_TYPE";
    public static final String SYSTEM_BROADCAST_TYPE = "SYSTEM_TYPE";
    private static final long TIMEOUT = 900000;
    private static final String SESSION_ID = "JSESSIONID";
    private static final String USERNAME = "username";
    private static final String CHAT_COLOR = "chatColor";

    /**
     * Broadcasts a message to all websocket sessions
     *
     * @param sender
     * @param message
     * @param type
     */
    private static void broadcastMessage(String sender, String message, String type, String chatColor) {
        String fullMessage = type.equals(WaitingRoomService.SYSTEM_BROADCAST_TYPE) ?
                message : String.format("[%s] %s: %s", getTimestamp(), sender, message);

        Application.waitingRoomMessagesList.add(fullMessage);

        Application.waitingRoomUsersMap.values().stream()
                .filter(Session::isOpen)
                .forEach(session -> {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("message", fullMessage);
                        json.put("type", type);
                        json.put("color", chatColor);
                        json.put("users", Application.waitingRoomUsersList);

                        session.getRemote().sendString(String.valueOf(json));
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                });
    }

    /**
     * Returns the timestamp in 2016-11-16 06:43:19.77 format
     *
     * @return
     */
    private static String getTimestamp() {
        String timestamp = new Timestamp(System.currentTimeMillis()).toString();
        timestamp = timestamp.substring(0, timestamp.indexOf("."));
        return timestamp;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        session.setIdleTimeout(TIMEOUT);

        Map<String, String> sessionCookies = session.getUpgradeRequest().getCookies().stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(USERNAME);
        String chatColor = sessionCookies.get(CHAT_COLOR);

        System.out.println(session.getUpgradeRequest().getCookies());
        System.out.println(sessionCookies);

        // Check if session has connected to waiting room before
        if (!Application.waitingRoomUsersMap.containsKey(sessionId)) {
            String message = String.format("(%s joined the chat)", username);

            Application.waitingRoomUsersList.add(username);
            Application.waitingRoomUsersMap.put(sessionId, session);
            broadcastMessage("", message, SYSTEM_BROADCAST_TYPE, chatColor);
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        Map<String, String> sessionCookies = session.getUpgradeRequest().getCookies().stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(USERNAME);
        String chatColor = sessionCookies.get(CHAT_COLOR);

        String message = String.format("(%s left the chat)", username);

        Application.waitingRoomUsersList.remove(username);
        Application.waitingRoomUsersMap.remove(sessionId);
        broadcastMessage("", message, SYSTEM_BROADCAST_TYPE, chatColor);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Map<String, String> sessionCookies = session.getUpgradeRequest().getCookies().stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));

        String username = sessionCookies.get(USERNAME);
        String chatColor = sessionCookies.get(CHAT_COLOR);

        broadcastMessage(username, message, USER_BROADCAST_TYPE, chatColor);
    }
}
