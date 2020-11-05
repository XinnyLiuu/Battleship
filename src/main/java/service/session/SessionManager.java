package service.session;

import spark.Request;

import java.util.Set;

public class SessionManager {

    public static boolean checkSessionVariableExists(Request req, String variable) {
        return req.session().attribute(variable) != null;
    }

    public static Object getSessionVariable(Request req, String variable) {
        return req.session().attribute(variable);
    }

    public static void setSessionVariable(Request req, String variable, Object value) {
        req.session().attribute(variable, value);
    }

    public static void endSession(Request req) {
        Set<String> sessionVariables = req.session().attributes();

        for (String s : sessionVariables) {
            req.session().removeAttribute(s);
        }
    }
}
