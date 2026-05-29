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
 * Data Access Object for the Leaderboard table.
 *
 * "Higher is better" — the {@code score} column blends remaining
 * chances and time bonus, so the top of the board has the highest
 * scores.
 */
public class ScoreDAO {

    /** Inserts a finished round into the leaderboard. */
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
            throw new RuntimeException("Failed to save score for " + playerName, e);
        }
    }

    /** Returns up to ten leaderboard rows for a difficulty, highest first. */
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
            throw new RuntimeException("Failed to load top scores for " + diff, e);
        }
        return records;
    }

    /**
     * True if the candidate score would make the top-10 board for this
     * difficulty (either the board has fewer than 10 rows, or the
     * candidate is higher than the lowest qualifier).
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
            throw new RuntimeException("Failed to check top-10 status for " + diff, e);
        }
    }
}
