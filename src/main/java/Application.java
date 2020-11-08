import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.websocket.GameRoomServiceBase;
import service.api.UserService;
import service.websocket.WaitingRoomServiceBase;
import service.session.SessionManager;
import service.session.SessionVariables;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.Map;

import static spark.Spark.*;

public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        staticFiles.location("/public");

        /*
         * Websockets
         */
        webSocket("/waiting-room", WaitingRoomServiceBase.class);
        webSocket("/game-room", GameRoomServiceBase.class);

        /*
         * UI Routes
         */
        before((req, res) -> log(req));
        before("/", (req, res) -> {
            if (!isAuthenticated(req)) res.redirect("/login");
        });
        before("/register", (req, res) -> {
            if (isAuthenticated(req)) res.redirect("/");
        });
        before("/login", (req, res) -> {
            if (isAuthenticated(req)) res.redirect("/");
        });
        before("/game", (req, res) -> {
            if (!isAuthenticated(req)) res.redirect("/login");
        });

        get("/", (req, res) -> render(Map.of(
                SessionVariables.STARTED, SessionManager.getSessionVariable(req, SessionVariables.STARTED),
                SessionVariables.USERNAME, SessionManager.getSessionVariable(req, SessionVariables.USERNAME)
        ), "/index.hbs"));
        get("/register", (req, res) -> render(Map.of(), "/register.hbs"));
        get("/login", (req, res) -> render(Map.of(), "/login.hbs"));
        get("/game", (req, res) -> render(Map.of(
                SessionVariables.STARTED, SessionManager.getSessionVariable(req, SessionVariables.STARTED),
                SessionVariables.USERNAME, SessionManager.getSessionVariable(req, SessionVariables.USERNAME)
        ), "/gameRoom.hbs"));

        /*
         * API Routes
         */
        path("/api", () -> {
            // User API
            path("/user", () -> {
                before("/register", (req, res) -> {
                    if (isAuthenticated(req)) halt(401);
                });
                before("/login", (req, res) -> {
                    if (isAuthenticated(req)) halt(401);
                });
                before("/logout", (req, res) -> {
                    if (!isAuthenticated(req)) halt(401);
                });

                post("/register", UserService::register);
                post("/login", UserService::login);
                post("/logout", UserService::logout);
            });
        });
    }

    /**
     * Renders the handlebars view alongside the data for the view
     *
     * @param model
     * @param pathToViewFile
     * @return
     */
    private static String render(Map<Object, Object> model, String pathToViewFile) {
        return new HandlebarsTemplateEngine().render(
                new ModelAndView(model, pathToViewFile)
        );
    }

    /**
     * Verifies that there is a session for the request
     *
     * @param req
     * @return
     */
    private static boolean isAuthenticated(Request req) {
        return SessionManager.checkSessionVariableExists(req, SessionVariables.STARTED);
    }

    /**
     * Logs all requests
     *
     * @param req
     */
    private static void log(Request req) {
        LOGGER.info(String.format("[%s] %s %s", req.requestMethod(), req.url(), req.body()));
    }
}
