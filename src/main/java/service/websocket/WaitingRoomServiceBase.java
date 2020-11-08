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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@WebSocket
public class WaitingRoomServiceBase extends BaseWebSocketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitingRoomServiceBase.class);

    private static final String CLIENT_CHALLENGE_TYPE = "CHALLENGE";
    private static final String CLIENT_ACCEPT_TYPE = "ACCEPT";
    private static final String CLIENT_DECLINE_TYPE = "DECLINE";

    private final AtomicInteger gameRoomCounter = new AtomicInteger();
    private final List<Session> pendingSenderRequests = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, Session> waitingRoomUsersMap = new ConcurrentHashMap<>();
    private final List<String> waitingRoomUsersList = Collections.synchronizedList(new ArrayList<>());
    // TODO: Persist messages into a table ?

    public WaitingRoomServiceBase() {
        super(LOGGER);
    }

    @Override
    public void onConnect(Session session) {
        session.setIdleTimeout(SESSION_TIMEOUT);

        Map<String, String> sessionCookies = getSessionCookies(session);

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(SESSION_USERNAME);
        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);

        if (!waitingRoomUsersMap.containsKey(sessionId)) {
            String message = String.format("(%s joined the chat)", username);

            waitingRoomUsersList.add(username);
            waitingRoomUsersMap.put(sessionId, session);
            sessionIdUsernameMap.put(sessionId, username);
            broadcastMessage("", message, BROADCAST_SYSTEM_TYPE, chatColor);
        }
    }

    @Override
    public void onClose(Session session, int statusCode, String reason) {
        Map<String, String> sessionCookies = getSessionCookies(session);

        String sessionId = sessionCookies.get(SESSION_ID);
        String username = sessionCookies.get(SESSION_USERNAME);
        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);

        String message = String.format("(%s left the chat)", username);

        waitingRoomUsersList.remove(username);
        waitingRoomUsersMap.remove(sessionId);
        sessionIdUsernameMap.remove(sessionId);
        broadcastMessage("", message, BROADCAST_SYSTEM_TYPE, chatColor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(Session session, String message) {
        Map<String, String> sessionCookies = getSessionCookies(session);

        String username = sessionCookies.get(SESSION_USERNAME);
        String chatColor = sessionCookies.get(SESSION_CHAT_COLOR);

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
        if (!json.keySet().containsAll(List.of(JSON_MESSAGE_TYPE, JSON_MESSAGE_TEXT))) {
            onError(new UnexpectedException("Unexpected message"));
        }

        LOGGER.info(json.toString());

        String type = json.get(JSON_MESSAGE_TYPE);
        String msg = json.get(JSON_MESSAGE_TEXT);

        switch (type) {
            case CLIENT_CHAT_TYPE:
                broadcastMessage(username, msg, BROADCAST_USER_TYPE, chatColor);
                break;
            case CLIENT_CHALLENGE_TYPE:
                checkSessionExistsForUsername(msg);
                pendingSenderRequests.add(session);
                sendChallengeInviteMessage(username, getSessionIdByUsername(msg));
                break;
            case CLIENT_ACCEPT_TYPE:
                checkSessionExistsForUsername(msg);
                sendChallengeResponseMessage(username, getSessionIdByUsername(msg), true);
                break;
            case CLIENT_DECLINE_TYPE:
                checkSessionExistsForUsername(msg);
                sendChallengeResponseMessage(username, getSessionIdByUsername(msg), false);
                break;
        }
    }

    /**
     * Broadcasts a message to all websocket sessions
     *
     * @param sender
     * @param message
     * @param type
     */
    private void broadcastMessage(String sender, String message, String type, String chatColor) {
        String fullMessage = type.equals(BROADCAST_SYSTEM_TYPE) ?
                message : String.format("[%s] %s: %s", getTimestamp(), sender, message);

        waitingRoomUsersMap.values().stream()
                .filter(Session::isOpen)
                .forEach(session -> sendJsonToSession(session,
                        new JSONObject()
                                .put(JSON_MESSAGE_TYPE, type)
                                .put(JSON_MESSAGE_TEXT, fullMessage)
                                .put(JSON_MESSAGE_COLOR, chatColor)
                                .put(JSON_MESSAGE_USERS, waitingRoomUsersList)));
    }

    /**
     * Sends a request message to the target websocket session
     *
     * @param sender
     * @param targetSessionId
     */
    private void sendChallengeInviteMessage(String sender, String targetSessionId) {
        String message = String.format("%s wants to challenge you to play a game!", sender);

        Session targetSession = waitingRoomUsersMap.get(targetSessionId);
        checkSessionOpened(targetSession);

        sendJsonToSession(targetSession,
                new JSONObject()
                        .put(JSON_MESSAGE_TYPE, CLIENT_CHALLENGE_TYPE)
                        .put(JSON_MESSAGE_TEXT, message)
                        .put(JSON_MESSAGE_SENDER, sender));
    }

    /**
     * Sends a message back to the challenge requester
     *
     * @param sender
     * @param targetSessionId
     * @param accepted
     */
    private void sendChallengeResponseMessage(String sender, String targetSessionId, boolean accepted) {
        String message = accepted ?
                String.format("%s has accepted your invite to play! Redirecting to game room now...", sender) :
                String.format("%s has declined your invite to play! Sorry :(", sender);

        Session targetSession = waitingRoomUsersMap.get(targetSessionId);
        checkSessionOpened(targetSession);

        if (accepted) {
            Session senderSession = waitingRoomUsersMap.get(getSessionIdByUsername(sender));
            checkSessionOpened(senderSession);

            // Remove challenge requester
            pendingSenderRequests.removeAll(List.of(targetSession));
            waitingRoomUsersList.remove(sessionIdUsernameMap.get(targetSessionId));
            waitingRoomUsersMap.remove(targetSessionId);
            sessionIdUsernameMap.remove(targetSessionId);

            // Remove challenge accept-er
            waitingRoomUsersList.remove(sender);
            waitingRoomUsersMap.remove(getSessionIdByUsername(sender));
            sessionIdUsernameMap.remove(getSessionIdByUsername(sender));

            LOGGER.info("After removal user list: " + waitingRoomUsersList.size());
            LOGGER.info("After removal user map: " + waitingRoomUsersMap.size());
            LOGGER.info("After removal session id user map: " + sessionIdUsernameMap.size());

            sendJsonToSession(senderSession,
                    new JSONObject()
                            .put(JSON_MESSAGE_TYPE, CLIENT_ACCEPT_TYPE)
                            .put(JSON_MESSAGE_TEXT, "Redirecting to game room now...")
                            .put(JSON_MESSAGE_GAME_ROOM_ID, gameRoomCounter.incrementAndGet()));

            GameRoomServiceBase.prepareGameRoom(gameRoomCounter.get());
        } else {
            pendingSenderRequests.remove(targetSession);
        }

        LOGGER.info("After removal pending list: " + pendingSenderRequests.size());

        JSONObject json = new JSONObject()
                .put(JSON_MESSAGE_TYPE, accepted ? CLIENT_ACCEPT_TYPE : CLIENT_DECLINE_TYPE)
                .put(JSON_MESSAGE_TEXT, message)
                .put(JSON_MESSAGE_SENDER, sender);

        if (accepted) {
            json.put(JSON_MESSAGE_GAME_ROOM_ID, gameRoomCounter.get());
        }

        sendJsonToSession(targetSession, json);
    }

    /**
     * Check that an session exists for the user, otherwise have the websocket return an error to the client
     *
     * @param username
     */
    private void checkSessionExistsForUsername(String username) {
        if (!sessionIdUsernameMap.containsValue(username)) {
            onError(new UnexpectedException("The requested user could not be found"));
        }
    }

    /**
     * Get the session id by a provided username
     *
     * @param username
     * @return Target's session id
     */
    private String getSessionIdByUsername(String username) {
        return sessionIdUsernameMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(username))
                .map(Map.Entry::getKey)
                .collect(Collectors.joining());
    }
}
