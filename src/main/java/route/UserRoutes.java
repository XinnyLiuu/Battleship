package route;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.dao.UserDao;
import data.table.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UserRoutes {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRoutes.class);

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

        // Get request body
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

//        // Create cookie
//        Optional<String> maybeCookie = generateCookie(maybeUser.get().getUsername(), maybeUser.get().getSessionKey());
//        if (maybeCookie.isEmpty()) {
//            res.status(500);
//            return "Unexpected error";
//        }

        res.status(200);
        res.cookie("token", maybeUser.get().getSessionKey());
        req.session().attribute("id", maybeUser.get().getId());
        req.session().attribute("username", maybeUser.get().getUsername());
        req.session().attribute("sessionKey", maybeUser.get().getSessionKey());

        return "Success";
    }

//    /**
//     * Generate a cookie from the session id and a checksum
//     *
//     * @return
//     */
//    private static Optional<String> generateCookie(String username, String sessionKey) {
//        try {
//            MessageDigest md = MessageDigest.getInstance("SHA-512");
//            byte[] hash = md.digest(sessionKey.getBytes());
//
//            return Optional.of(Base64.getEncoder().encodeToString(hash));
//        } catch (NoSuchAlgorithmException nsae) {
//            LOGGER.error(nsae.getMessage());
//        }
//
//        return Optional.empty();
//    }
}
