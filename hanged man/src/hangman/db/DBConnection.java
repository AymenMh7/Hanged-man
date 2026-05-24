package hangman.db;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton MySQL connection manager.
 *
 * The constructor is private — DAO classes obtain the active connection
 * through {@link #getInstance()}.{@link #getConnection()}, guaranteeing
 * the application only ever opens one JDBC connection.
 *
 * Credentials are read from <code>config.properties</code> sitting next
 * to the executable (or supplied via JVM system properties).
 */
public final class DBConnection {

    /** The single instance of the class. */
    private static DBConnection instance;

    /** The active JDBC connection object. */
    private Connection connection;

    /**
     * Private constructor — loads the driver and opens the connection.
     * Throws {@link RuntimeException} if anything fails (we want loud
     * failures during startup, not silent NPEs later).
     */
    private DBConnection() {
        try {
            Properties props = loadConfig();
            String url      = props.getProperty("db.url",
                    "jdbc:mysql://localhost:3306/hangman_db?useSSL=false&serverTimezone=UTC");
            String user     = props.getProperty("db.user", "root");
            String password = props.getProperty("db.password", "");

            // Load the MySQL driver explicitly (good practice for plain
            // Java apps where the ServiceLoader may not pick it up).
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "MySQL JDBC Driver not found. Place mysql-connector-j-*.jar in lib/.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                "Could not connect to MySQL — check config.properties.", e);
        }
    }

    /** Returns the active instance (creating it the first time). */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Returns the live SQL connection — DAOs use this to build statements. */
    public Connection getConnection() {
        return connection;
    }

    /** Closes the connection gracefully on game exit. */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // shutting down — nothing we can do
        } finally {
            instance = null;
        }
    }

    /** Loads config.properties from the working directory, if present. */
    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (Exception ignored) {
            // No config file? Fall back to JVM system properties / defaults.
        }
        // System properties override file (handy for tests).
        for (String key : new String[]{"db.url", "db.user", "db.password"}) {
            String sys = System.getProperty(key);
            if (sys != null) {
                props.setProperty(key, sys);
            }
        }
        return props;
    }
}
