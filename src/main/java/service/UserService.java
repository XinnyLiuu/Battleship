package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.dao.UserDao;
import data.table.User;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.session.SessionManager;
import service.session.SessionVariables;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    /**
     * Maps to /register
     * Creates a new user for the database
     *
     * @param req
     * @param res
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String register(Request req, Response res) {
        UserDao dao = new UserDao();

        ObjectMapper mapper = new ObjectMapper();
        Optional<Map<String, String>> maybeJson = Optional.empty();

        try {
            maybeJson = Optional.of(mapper.readValue(req.body(), Map.class));
        } catch (IOException ioe) {
            LOGGER.error(ioe.getMessage());
            res.status(500);
        }

        if (maybeJson.isEmpty()) {
            res.status(500);
            return "Empty request body";
        }

        Map<String, String> json = maybeJson.get();
        if (!json.keySet().containsAll(List.of("username", "password"))) {
            res.status(500);
            return "Unexpected request body";
        }

        // Create user
        Optional<User> maybeUser = dao.register(json.get("username"), json.get("password"));
        if (maybeUser.isEmpty()) {
            res.status(500);
            return "Error registering user";
        }

        res.status(200);
        SessionManager.setSessionVariable(req, SessionVariables.ID, maybeUser.get().getId());
        SessionManager.setSessionVariable(req, SessionVariables.USERNAME, maybeUser.get().getUsername());
        SessionManager.setSessionVariable(req, SessionVariables.PASSWORD, maybeUser.get().getPassword());
        SessionManager.setSessionVariable(req, SessionVariables.STARTED, true);

        return String.valueOf(new JSONObject()
                .put("username", maybeUser.get().getUsername())
                .put("chatColor", generateRandomHexString())
        );
    }

    /**
     * Maps to /login
     * Logs in the user
     *
     * @param req
     * @param res
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String login(Request req, Response res) {
        UserDao dao = new UserDao();

        ObjectMapper mapper = new ObjectMapper();
        Optional<Map<String, String>> maybeJson = Optional.empty();

        try {
            maybeJson = Optional.of(mapper.readValue(req.body(), Map.class));
        } catch (IOException ioe) {
            LOGGER.error(ioe.getMessage());
            res.status(500);
        }

        if (maybeJson.isEmpty()) {
            res.status(500);
            return "Empty request body";
        }

        Map<String, String> json = maybeJson.get();
        if (!json.keySet().containsAll(List.of("username", "password"))) {
            res.status(500);
            return "Unexpected request body";
        }

        // Authenticate user
        Optional<User> maybeUser = dao.login(json.get("username"), json.get("password"));
        if (maybeUser.isEmpty()) {
            res.status(500);
            return "Error registering user";
        }

        res.status(200);
        SessionManager.setSessionVariable(req, SessionVariables.ID, maybeUser.get().getId());
        SessionManager.setSessionVariable(req, SessionVariables.USERNAME, maybeUser.get().getUsername());
        SessionManager.setSessionVariable(req, SessionVariables.PASSWORD, maybeUser.get().getPassword());
        SessionManager.setSessionVariable(req, SessionVariables.STARTED, true);

        return String.valueOf(new JSONObject()
                .put("username", maybeUser.get().getUsername())
                .put("chatColor", generateRandomHexString())
        );
    }

    /**
     * Maps to /logout
     * Logs out the user and removes the user's session
     *
     * @param req
     * @param res
     * @return
     */
    public static String logout(Request req, Response res) {
        UserDao dao = new UserDao();

        boolean loggedOut = dao.logout(
                SessionManager.getSessionVariable(req, SessionVariables.USERNAME).toString(),
                SessionManager.getSessionVariable(req, SessionVariables.PASSWORD).toString());

        if (!loggedOut) {
            res.status(500);
            return "Error logging out user";
        }

        res.status(200);
        SessionManager.endSession(req);

        return "Success";
    }

    private static String generateRandomHexString() {
        Random random = new Random();
        int nextInt = random.nextInt(0xffffff + 1);
        return String.format("#%06x", nextInt);
    }
}
