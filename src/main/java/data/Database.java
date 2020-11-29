package data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);
    private static final String URL = "jdbc:mysql://localhost/battleship?user=root&password=password";

    /**
     * Returns the connection object to the database
     *
     * @return Connection object
     */
    public static Optional<Connection> getConnection() {
        try {
            return Optional.of(DriverManager.getConnection(URL));
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Closes the connection to the database
     *
     * @param conn
     */
    public static void closeConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage());
        }
    }
}
