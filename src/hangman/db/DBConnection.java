package hangman.db;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestionnaire singleton de la connexion MySQL.
 *
 * Le constructeur est privé — les classes DAO obtiennent la connexion
 * active via {@link #getInstance()}.{@link #getConnection()}, garantissant
 * que l'application n'ouvre qu'une seule connexion JDBC.
 *
 * Les identifiants sont lus depuis <code>config.properties</code> placé
 * à côté de l'exécutable (ou fournis via les propriétés système de la JVM).
 */
public final class DBConnection {

    /** L'instance unique de la classe. */
    private static DBConnection instance;

    /** L'objet de connexion JDBC actif. */
    private Connection connection;

    /**
     * Constructeur privé — charge le pilote et ouvre la connexion.
     * Lance une {@link RuntimeException} en cas d'échec (on veut des
     * erreurs bruyantes au démarrage, pas des NullPointerException
     * silencieuses plus tard).
     */
    private DBConnection() {
        try {
            Properties props = loadConfig();
            String url      = props.getProperty("db.url",
                    "jdbc:mysql://localhost:3306/hangman_db?useSSL=false&serverTimezone=UTC");
            String user     = props.getProperty("db.user", "root");
            String password = props.getProperty("db.password", "");

            // Charge le pilote MySQL explicitement (bonne pratique pour
            // les applications Java standard où le ServiceLoader peut
            // ne pas le repérer).
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "Pilote JDBC MySQL introuvable. Placez mysql-connector-j-*.jar dans lib/.", e);
        } catch (SQLException e) {
            throw new RuntimeException(
                "Impossible de se connecter à MySQL — vérifiez config.properties.", e);
        }
    }

    /** Renvoie l'instance active (la crée à la première utilisation). */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    /** Renvoie la connexion SQL active — les DAO l'utilisent pour bâtir les requêtes. */
    public Connection getConnection() {
        return connection;
    }

    /** Ferme proprement la connexion à la sortie du jeu. */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // arrêt en cours — on ne peut rien faire
        } finally {
            instance = null;
        }
    }

    /** Charge config.properties depuis le dossier courant, s'il existe. */
    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (Exception ignored) {
            // Pas de fichier de config ? On retombe sur les propriétés
            // système de la JVM ou les valeurs par défaut.
        }
        // Les propriétés système ont priorité sur le fichier (pratique pour les tests).
        for (String key : new String[]{"db.url", "db.user", "db.password"}) {
            String sys = System.getProperty(key);
            if (sys != null) {
                props.setProperty(key, sys);
            }
        }
        return props;
    }
}
