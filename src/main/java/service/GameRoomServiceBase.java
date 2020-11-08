package service;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;

@WebSocket
public class GameRoomServiceBase extends BaseWebSocketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRoomServiceBase.class);

    public GameRoomServiceBase() {
        super(LOGGER);
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

    @Override
    public void onConnect(Session session) {
    }

    @Override
    public void onClose(Session session, int statusCode, String reason) {
    }

    @Override
    public void onMessage(Session session, String message) {
    }
}
