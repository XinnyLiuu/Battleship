import com.google.common.collect.ImmutableMap;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.Map;

import static spark.Spark.get;

public class Application {
    public static void main(String[] args) {
        get("/test", (req, res) -> {
            Map<String, Object> model = ImmutableMap.of(
                    "name", "Xin Liu",
                    "message", "Hello,"
            );

            return new HandlebarsTemplateEngine().render(
                    new ModelAndView(model, "hello.hbs")
            );
        });
    }
}
