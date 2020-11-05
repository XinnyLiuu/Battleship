package data.dao;

import data.Database;
import data.table.ImmutableUser;
import data.table.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;

import static data.table.ImmutableUser.builder;

public class UserDao implements Dao<User> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDao.class);
    private static final String INSERT_USER = "insert into users (username, password) values (?, ?)";
    private static final String GET_USER_BY_USERNAME_AND_PASSWORD = "select id, username, password from users where username = ? and password = ?";
    private static final String UPDATE_USER = "update users set username = ?, password = ? where username = ?";

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
        Optional<String> maybeHash = hashPassword(password);
        if (maybeHash.isEmpty()) return Optional.empty();

        int affected = insertUser(username, maybeHash.get());
        if (affected == 0) return Optional.empty();

        return getUser(username, maybeHash.get());
    }

    /**
     * Authenticates the user
     *
     * @param username
     * @param password
     * @return
     */
    public Optional<User> login(String username, String password) {
        Optional<String> maybeHash = hashPassword(password);
        if (maybeHash.isEmpty()) return Optional.empty();

        return getUser(username, maybeHash.get());
    }

    /**
     * Log the user out
     *
     * @param username
     * @param password
     * @return
     */
    public boolean logout(String username, String password) {
        return getUser(username, password).isPresent();
    }

    /**
     * Updates an existing user
     *
     * @param user
     * @return
     */
    private int updateUser(User user) {
        int affected = 0;

        Optional<Connection> maybeConn = Database.getConnection();
        if (maybeConn.isEmpty()) return affected;

        Connection conn = maybeConn.get();

        try {
            conn.setAutoCommit(false);

            PreparedStatement updateUser = conn.prepareStatement(UPDATE_USER);
            updateUser.setString(1, user.getUsername());
            updateUser.setString(2, user.getPassword());
            updateUser.setString(3, user.getUsername());
            affected = updateUser.executeUpdate();

            conn.commit();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage());
            Database.closeConnection(conn);
            return affected;
        }

        Database.closeConnection(conn);
        return affected;
    }

    /**
     * Inserts a user into the database
     *
     * @param username
     * @param hash
     * @return
     */
    private int insertUser(String username, String hash) {
        int affected = 0;

        Optional<Connection> maybeConn = Database.getConnection();
        if (maybeConn.isEmpty()) return affected;

        Connection conn = maybeConn.get();

        try {
            conn.setAutoCommit(false);

            PreparedStatement insertUser = conn.prepareStatement(INSERT_USER);
            insertUser.setString(1, username);
            insertUser.setString(2, hash);
            affected = insertUser.executeUpdate();

            conn.commit();
        } catch (SQLException sqle) {
            LOGGER.error(sqle.getMessage());
            Database.closeConnection(conn);
            return affected;
        }

        Database.closeConnection(conn);
        return affected;
    }

    /**
     * Gets the user object for the provided credentials
     *
     * @param username
     * @param hash
     * @return
     */
    private Optional<User> getUser(String username, String hash) {
        Optional<Connection> maybeConn = Database.getConnection();
        if (maybeConn.isEmpty()) return Optional.empty();

        Connection conn = maybeConn.get();
        ImmutableUser.Builder userBuilder = builder();

        try {
            PreparedStatement selectUser = conn.prepareStatement(GET_USER_BY_USERNAME_AND_PASSWORD);
            selectUser.setString(1, username);
            selectUser.setString(2, hash);
            ResultSet rs = selectUser.executeQuery();

            while (rs.next()) {
                userBuilder.id(rs.getLong(1))
                        .username(rs.getString(2))
                        .password(rs.getString(3));
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
     * Hash the password with SHA-512
     *
     * @param password
     * @return Hashed password
     */
    private Optional<String> hashPassword(String password) {
        try {
            // Hash
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hash = md.digest(password.getBytes());

            return Optional.of(Base64.getEncoder().encodeToString(hash));
        } catch (NoSuchAlgorithmException nsae) {
            LOGGER.error(nsae.getMessage());
        }

        return Optional.empty();
    }
}
