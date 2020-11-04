package data.dao;

import data.Database;
import data.table.ImmutableUser;
import data.table.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;

import static data.table.ImmutableUser.builder;

public class UserDao implements Dao<User> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDao.class);
    private static final String INSERT_USER_SQL = "insert into users (username, password, session_key) values (?, ?, ?)";
    private static final String GET_USER_BY_USERNAME = "select id, username, password, session_key from users where username = ?";

    public UserDao() {
    }

    /**
     * Registers the user
     *
     * @param username
     * @param password
     * @return
     */
    public Optional<User> register(String username, String password) {
        Optional<Connection> maybeConn = Database.getConnection();
        if (maybeConn.isEmpty()) {
            return Optional.empty();
        }

        Connection conn = maybeConn.get();
        int affected;

        try {
            conn.setAutoCommit(false);

            // Generate password hash
            Optional<String> maybeHash = hashPassword(password);
            if (maybeHash.isEmpty()) {
                return Optional.empty();
            }

            // Create session key
            Optional<String> maybeSessionKey = generateSessionKey();
            if (maybeSessionKey.isEmpty()) {
                return Optional.empty();
            }

            // Insert user
            PreparedStatement insertUser = conn.prepareStatement(INSERT_USER_SQL);
            insertUser.setString(1, username);
            insertUser.setString(2, maybeHash.get());
            insertUser.setString(3, maybeSessionKey.get());
            affected = insertUser.executeUpdate();

            conn.commit();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage());
            Database.closeConnection(conn);
            return Optional.empty();
        }

        if (affected == 0) {
            return Optional.empty();
        }

        ImmutableUser.Builder userBuilder = builder();

        try {
            // Get user
            PreparedStatement selectUser = conn.prepareStatement(GET_USER_BY_USERNAME);
            selectUser.setString(1, username);
            ResultSet rs = selectUser.executeQuery();

            while (rs.next()) {
                userBuilder.id(rs.getLong(1))
                        .username(rs.getString(2))
                        .password(rs.getString(3))
                        .sessionKey(rs.getString(4));
            }
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage());
            Database.closeConnection(conn);
            return Optional.empty();
        }

        Database.closeConnection(conn);
        return Optional.of(userBuilder.build());
    }

    /**
     * Logs in the user
     *
     * @param username
     * @param password
     * @return
     */
    public String login(String username, String password) {
        return "";
    }

    /**
     * Hash the password with salt + SHA-512
     *
     * @param password
     * @return Hashed password
     */
    private Optional<String> hashPassword(String password) {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes());

            return Optional.of(Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException nsae) {
            LOGGER.error(nsae.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Generate a random session key
     *
     * @return
     */
    private Optional<String> generateSessionKey() {
        try {
            // Generate salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hash
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(salt);

            return Optional.of(Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException nsae) {
            LOGGER.error(nsae.getMessage());
        }

        return Optional.empty();
    }
}
