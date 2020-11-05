import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserService;
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
        // Default routes
        get("/", (req, res) -> {
            return render(Map.of(
                    "authenticated", isAuthenticated(req)
            ), "index.hbs");
        });

        // API routes
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

    private static String render(Map<Object, Object> model, String pathToViewFile) {
        return new HandlebarsTemplateEngine().render(
                new ModelAndView(model, pathToViewFile)
        );
    }

    private static boolean isAuthenticated(Request req) {
        System.out.println(req.session().attributes());

        return SessionManager.checkSessionVariableExists(req, SessionVariables.STARTED);
    }
}
