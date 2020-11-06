package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpCookie;
import java.rmi.UnexpectedException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@WebSocket
public class WaitingRoomService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingRoomService.class);

    private static final String BROADCAST_USER_TYPE = "USER";
    private static final String BROADCAST_SYSTEM_TYPE = "SYSTEM";

    private static final String CLIENT_CHAT_TYPE = "CHAT";
    private static final String CLIENT_REQUEST_TYPE = "REQUEST";

    private static final long TIMEOUT = 900000;
    private static final String SESSION_ID = "JSESSIONID";
    private static final String USERNAME = "username";
    private static final String CHAT_COLOR = "chatColor";

    private final Map<String, Session> waitingRoomUsersMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdToUsernameMap = new ConcurrentHashMap<>();
    private final List<String> waitingRoomUsersList = Collections.synchronizedList(new ArrayList<>());
    private final List<String> waitingRoomMessagesList = Collections.synchronizedList(new ArrayList<>()); // TODO: Persist these messages into a table

    /**
     * Broadcasts a message to all websocket sessions
     *
     * @param sender
     * @param message
     * @param type
     */
    private void broadcastMessage(String sender, String message, String type, String chatColor) {
        String fullMessage = type.equals(WaitingRoomService.BROADCAST_SYSTEM_TYPE) ?
                message : String.format("[%s] %s: %s", getTimestamp(), sender, message);

        waitingRoomMessagesList.add(fullMessage);

        waitingRoomUsersMap.values().stream()
                .filter(Session::isOpen)
                .forEach(session -> {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("type", type);
                        json.put("message", fullMessage);
                        json.put("color", chatColor);
                        json.put("users", waitingRoomUsersList);

                        session.getRemote().sendString(String.valueOf(json));
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                });
    }

    /**
     * Sends a request message to the target websocket session
     *
     * @param sender
     * @param targetSessionId
     */
    private void sendRequestMessage(String sender, String targetSessionId) {
        String message = String.format("%s wants to invite you to play a game!", sender);
        Session targetSession = waitingRoomUsersMap.get(targetSessionId);

        if (!targetSession.isOpen()) {
            onError(new UnexpectedException("The request player is not longer present"));
        }

        try {
            JSONObject json = new JSONObject();
            json.put("type", CLIENT_REQUEST_TYPE);
            json.put("message", message);
            json.put("sender", sender);

            targetSession.getRemote().sendString(String.valueOf(json));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Returns the timestamp in 2016-11-16 06:43:19 format
     *
     * @return
     */
    private String getTimestamp() {
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

        if (!waitingRoomUsersMap.containsKey(sessionId)) {
            String message = String.format("(%s joined the chat)", username);

            waitingRoomUsersList.add(username);
            waitingRoomUsersMap.put(sessionId, session);
            sessionIdToUsernameMap.put(sessionId, username);
            broadcastMessage("", message, BROADCAST_SYSTEM_TYPE, chatColor);
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

        waitingRoomUsersList.remove(username);
        waitingRoomUsersMap.remove(sessionId);
        sessionIdToUsernameMap.remove(sessionId);
        broadcastMessage("", message, BROADCAST_SYSTEM_TYPE, chatColor);
    }

    @OnWebSocketMessage
    @SuppressWarnings("unchecked")
    public void onMessage(Session session, String message) {
        Map<String, String> sessionCookies = session.getUpgradeRequest().getCookies().stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(USERNAME);
        String chatColor = sessionCookies.get(CHAT_COLOR);

        // The request will send a serialized json
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
        if (!json.keySet().containsAll(List.of("type", "message"))) {
            onError(new UnexpectedException("Unexpected message"));
        }

        String type = json.get("type");
        String msg = json.get("message");

        LOGGER.info(json.toString());

        if (type.equals(CLIENT_CHAT_TYPE)) {
            broadcastMessage(username, msg, BROADCAST_USER_TYPE, chatColor);
        } else if (type.equals(CLIENT_REQUEST_TYPE)) {
            if (!sessionIdToUsernameMap.containsValue(msg)) {
                onError(new UnexpectedException("The requested user could not be found"));
            }

            String targetSessionId = sessionIdToUsernameMap.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(msg))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining());

            sendRequestMessage(username, targetSessionId);
        }
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        LOGGER.error(error.getMessage());
    }
}
