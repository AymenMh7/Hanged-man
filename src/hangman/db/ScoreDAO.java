package hangman.db;

import hangman.enums.Difficulty;
import hangman.models.ScoreRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object pour la table Leaderboard.
 *
 * "Plus c'est élevé, mieux c'est" — la colonne {@code score} combine
 * les chances restantes et le bonus de temps, donc le haut du
 * classement contient les scores les plus élevés.
 */
public class ScoreDAO {

    /** Insère une manche terminée dans le classement. */
    public void saveScore(Difficulty diff, String playerName, long score) {
        final String sql =
            "INSERT INTO Leaderboard (player_name, difficulty, score) VALUES (?, ?, ?)";

        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, diff.name());
            ps.setLong(3, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la sauvegarde du score pour " + playerName, e);
        }
    }

    /** Renvoie jusqu'à dix lignes du classement pour une difficulté, du plus élevé au plus bas. */
    public List<ScoreRecord> getTop10Scores(Difficulty diff) {
        final String sql =
            "SELECT player_name, score FROM Leaderboard "
          + "WHERE difficulty = ? ORDER BY score DESC LIMIT 10";

        List<ScoreRecord> records = new ArrayList<>();
        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, diff.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new ScoreRecord(
                        rs.getString("player_name"),
                        rs.getLong("score")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec du chargement des meilleurs scores pour " + diff, e);
        }
        return records;
    }

    /**
     * Renvoie true si le score candidat ferait entrer le joueur dans
     * le top 10 pour cette difficulté (soit le tableau a moins de 10
     * lignes, soit le candidat est plus élevé que le moins bon qualifié).
     */
    public boolean isTop10(Difficulty diff, long candidateScore) {
        final String sql =
            "SELECT score FROM Leaderboard "
          + "WHERE difficulty = ? ORDER BY score DESC LIMIT 10";

        Connection conn = DBConnection.getInstance().getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, diff.name());
            try (ResultSet rs = ps.executeQuery()) {
                int rows = 0;
                long lowest = Long.MAX_VALUE;
                while (rs.next()) {
                    long s = rs.getLong("score");
                    if (s < lowest) lowest = s;
                    rows++;
                }
                if (rows < 10)    return true;
                return candidateScore > lowest;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Échec de la vérification du statut top 10 pour " + diff, e);
        }
    }
}
