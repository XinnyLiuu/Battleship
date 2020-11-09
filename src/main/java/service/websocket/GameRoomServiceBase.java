package service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket
public class GameRoomServiceBase extends BaseWebSocketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRoomServiceBase.class);

    private static final String CLIENT_END_TYPE = "END";

    private static final Map<Integer, Map<String, Session>> gameRoomUsersMap = new ConcurrentHashMap<>();
    private static final Map<Integer, List<String>> gameRoomUsernamesMap = new ConcurrentHashMap<>();
    // TODO: Persist messages into a table ?

    public GameRoomServiceBase() {
        super(LOGGER);
    }

    /**
     * Prepares the data structures needed for game rooms
     *
     * @param gameRoomId
     */
    static void prepareGameRoom(int gameRoomId) {
        LOGGER.info(String.format("Creating game room id:  %s", gameRoomId));

        gameRoomUsersMap.put(gameRoomId, new ConcurrentHashMap<>());
        gameRoomUsernamesMap.put(gameRoomId, Collections.synchronizedList(new ArrayList<>()));

        LOGGER.info("Game Rooms: " + gameRoomUsersMap.toString());
    }

    @Override
    public void onConnect(Session session) {
        session.setIdleTimeout(SESSION_TIMEOUT);

        Map<String, String> sessionCookies = getSessionCookies(session);

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(SESSION_USERNAME);
        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);
        int gameRoomId = Integer.parseInt(sessionCookies.get(SESSION_GAME_ROOM_ID));

        if (!gameRoomUsersMap.containsKey(gameRoomId) && !gameRoomUsernamesMap.containsKey(gameRoomId)) {
            onError(new UnexpectedException("Game room has not been created yet!"));
        }

        if (!gameRoomUsersMap.get(gameRoomId).containsKey(sessionId)) {
            String message = String.format("(%s joined the chat)", username);

            gameRoomUsernamesMap.get(gameRoomId).add(username);
            gameRoomUsersMap.get(gameRoomId).put(sessionId, session);
            sessionIdUsernameMap.put(sessionId, username);
            broadcastMessage("", message, BROADCAST_SYSTEM_TYPE, chatColor, gameRoomId);
        }
    }

    @Override
    public void onClose(Session session, int statusCode, String reason) {
        Map<String, String> sessionCookies = getSessionCookies(session);

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(SESSION_USERNAME);
        int gameRoomId = Integer.parseInt(sessionCookies.get(SESSION_GAME_ROOM_ID));

        String message = String.format("%s left the chat, closing game room due to lack of players", username);

        gameRoomUsersMap.get(gameRoomId).remove(sessionId);
        gameRoomUsernamesMap.get(gameRoomId).remove(username);
        sessionIdUsernameMap.remove(sessionId);
        sendEndGameMessage(message, gameRoomId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(Session session, String message) {
        Map<String, String> sessionCookies = getSessionCookies(session);

        String username = sessionCookies.get(SESSION_USERNAME);
        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);
        int gameRoomId = Integer.parseInt(sessionCookies.get(SESSION_GAME_ROOM_ID));

        ObjectMapper mapper = new ObjectMapper();
        Optional<Map<String, String>> maybeJson = Optional.empty();

        try {
            maybeJson = Optional.of(mapper.readValue(message, Map.class));
        } catch (IOException ioe) {
            onError(ioe);
        }

        if (maybeJson.isEmpty()) {
            onError(new UnexpectedException("Error parsing received message"));
        }

        Map<String, String> json = maybeJson.get();
        if (!json.keySet().containsAll(List.of(JSON_MESSAGE_TYPE, JSON_MESSAGE_TEXT))) {
            onError(new UnexpectedException("Unexpected message"));
        }

        LOGGER.info(json.toString());

        String type = json.get(JSON_MESSAGE_TYPE);
        String msg = json.get(JSON_MESSAGE_TEXT);

        switch (type) {
            case CLIENT_CHAT_TYPE:
                broadcastMessage(username, msg, BROADCAST_USER_TYPE, chatColor, gameRoomId);
                break;
        }
    }

    /**
     * Broadcasts a message to all websocket sessions in a game room
     *
     * @param sender
     * @param message
     * @param type
     */
    private void broadcastMessage(String sender, String message, String type, String chatColor, int gameRoomId) {
        String fullMessage = type.equals(BROADCAST_SYSTEM_TYPE) ?
                message : String.format("[%s] %s: %s", getTimestamp(), sender, message);

        gameRoomUsersMap.get(gameRoomId).values().stream()
                .filter(Session::isOpen)
                .forEach(session -> sendJsonToSession(session,
                        new JSONObject()
                                .put(JSON_MESSAGE_TYPE, type)
                                .put(JSON_MESSAGE_TEXT, fullMessage)
                                .put(JSON_MESSAGE_COLOR, chatColor)
                                .put(JSON_MESSAGE_USERS, gameRoomUsernamesMap.get(gameRoomId))));
    }

    /**
     * A player has left the game room, notify the other player and close the game room
     *
     * @param message
     * @param gameRoomId
     */
    private void sendEndGameMessage(String message, int gameRoomId) {
        gameRoomUsersMap.get(gameRoomId).values().stream()
                .filter(Session::isOpen)
                .forEach(session -> sendJsonToSession(session,
                        new JSONObject()
                                .put(JSON_MESSAGE_TYPE, CLIENT_END_TYPE)
                                .put(JSON_MESSAGE_TEXT, message)));

        gameRoomUsersMap.remove(gameRoomId);
        gameRoomUsernamesMap.remove(gameRoomId);

        LOGGER.info("Ended game");
        LOGGER.info("gameRoomUserMap: " + gameRoomUsersMap.toString());
        LOGGER.info("gameRoomUsernamesMap: " + gameRoomUsernamesMap.toString());
        LOGGER.info("sessionIdUsernameMap: " + sessionIdUsernameMap.toString());
    }
}
