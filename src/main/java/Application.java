import route.UserRoutes;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;

public class Application {
    public static void main(String[] args) {
        get("/", (req, res) -> {
            // Check if user is authenticated
            String session = req.session().attribute("sessionKey");

            if (session == null) {
                return render(Map.of("authenticated", false), "index.hbs");
            }

            return render(Map.of(
                    "authenticated", true,
                    "name", req.session().attribute("username")
            ), "index.hbs");
        });

        post("/register", UserRoutes::register);
    }

    private static String render(Map<Object, Object> model, String pathToViewFile) {
        return new HandlebarsTemplateEngine().render(
                new ModelAndView(model, pathToViewFile)
        );
    }
}
