package service.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@WebSocket
public class GameRoomServiceBase extends BaseWebSocketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRoomServiceBase.class);

    static AtomicInteger gameRoomCounter = new AtomicInteger();
    static Map<Integer, Map<String, Session>> gameRoomUsersMap = new ConcurrentHashMap<>();
    static Map<Integer, List<String>> gameRoomUsernamesMap = new ConcurrentHashMap<>();
    static Map<Integer, List<String>> gameRoomMessagesMap = new ConcurrentHashMap<>(); // TODO: Persist these messages into a table or actually return this to the client, right now it's not being used.

    public GameRoomServiceBase() {
        super(LOGGER);
    }

    @Override
    public void onConnect(Session session) {
        session.setIdleTimeout(SESSION_TIMEOUT);

        Map<String, String> sessionCookies = getSessionCookies(session);

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(SESSION_USERNAME);
        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);
        int gameRoomId = Integer.parseInt(sessionCookies.get(SESSION_GAME_ROOM_ID));

        LOGGER.info("gameRoomUsersMap: " + gameRoomUsersMap.toString());
        LOGGER.info("gameRoomUsersMap Size: " + gameRoomUsersMap.size());

        if (!gameRoomUsersMap.get(gameRoomId).containsKey(sessionId)) {
            String message = String.format("(%s joined the chat)", username);

            // TODO: Websocket closes at the put method, not sure why?
//            gameRoomUsersMap.get(gameRoomId).put(sessionId, session);
//            gameRoomUsernamesMap.get(gameRoomId).add(username);
//            sessionIdUsernameMap.put(sessionId, username);
//            broadcastMessage("", message, BROADCAST_SYSTEM_TYPE, chatColor, gameRoomId);
        }
    }

    @Override
    public void onClose(Session session, int statusCode, String reason) {
        LOGGER.error(String.format("%s - %s", statusCode, reason));
//        Map<String, String> sessionCookies = getSessionCookies(session);
//
//        String sessionId = sessionCookies.get(SESSION_ID);
//        String username = sessionCookies.get(SESSION_USERNAME);
//        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);
//        int gameRoomId = Integer.parseInt(sessionCookies.get(SESSION_GAME_ROOM_ID));
//
//        String message = String.format("(%s left the chat)", username);
    }

    @Override
    public void onMessage(Session session, String message) {
    }

    /**
     * Broadcasts a message to all websocket sessions in a game room
     *
     * @param sender
     * @param message
     * @param type
     */
    private void broadcastMessage(String sender, String message, String type, String chatColor, int gameRoomId) {
        String fullMessage = type.equals(WaitingRoomServiceBase.BROADCAST_SYSTEM_TYPE) ?
                message : String.format("[%s] %s: %s", getTimestamp(), sender, message);

        gameRoomMessagesMap.get(gameRoomId).add(fullMessage);
        gameRoomUsersMap.get(gameRoomId).values().stream()
                .filter(Session::isOpen)
                .forEach(session -> sendJsonToSession(session,
                        new JSONObject()
                                .put(JSON_MESSAGE_TYPE, type)
                                .put(JSON_MESSAGE_TEXT, fullMessage)
                                .put(JSON_MESSAGE_COLOR, chatColor)
                                .put(JSON_MESSAGE_USERS, gameRoomUsernamesMap.get(gameRoomId))));
    }
}
