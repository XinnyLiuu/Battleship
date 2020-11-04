package data;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatabaseTest {
    @Test
    public void itConnectsToDatabase() {
        Connection actual = Database.getConnection().orElseGet(null);
        assertNotNull(actual, "Expected to connect to database");
    }
}
