package hangman.db;

import hangman.enums.Difficulty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for retrieving words from the Dictionary table.
 *
 * Keeping the SQL here — and nowhere else — means the rest of the
 * application can swap MySQL for another store without touching the
 * managers or models.
 */
public class DictionaryDAO {

    /**
     * Fetches one random word for the given difficulty.
     *
     * @return the word as a String, lowercase, or {@code null} if the
     *         dictionary has no entry for this difficulty.
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
            throw new RuntimeException("Failed to fetch random word for " + diff, e);
        }
        return null;
    }
}
