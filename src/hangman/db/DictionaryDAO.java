package hangman.db;

import hangman.enums.Difficulty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object pour récupérer les mots de la table Dictionary.
 *
 * Garder le SQL ici — et nulle part ailleurs — permet au reste de
 * l'application de remplacer MySQL par un autre stockage sans toucher
 * aux managers ni aux modèles.
 */
public class DictionaryDAO {

    /**
     * Récupère un mot aléatoire pour la difficulté donnée.
     *
     * @return le mot sous forme de String en minuscules, ou {@code null}
     *         si le dictionnaire n'a aucune entrée pour cette difficulté.
     */
    public String getRandomWord(Difficulty diff) {
        final String sql =
            "SELECT word FROM Dictionary WHERE difficulty = ? ORDER BY RAND() LIMIT 1";

        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, diff.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("word").toLowerCase();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec de récupération d'un mot aléatoire pour " + diff, e);
        }
        return null;
    }
}
