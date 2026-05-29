package hangman.models;

/**
 * Data-transfer object: one row from the Leaderboard table.
 * The {@code score} value is the combined (chances + time) score —
 * HIGHER is better. Implements {@link Comparable} so callers can sort
 * lists directly: natural order = best (highest score) first.
 */
public class ScoreRecord implements Comparable<ScoreRecord> {

    private final String playerName;
    private final long   score;

    public ScoreRecord(String playerName, long score) {
        this.playerName = playerName;
        this.score = score;
    }

    public String getPlayerName() { return playerName; }
    public long   getScore()      { return score; }

    /** Natural order = highest score first. */
    @Override
    public int compareTo(ScoreRecord other) {
        return Long.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("%-20s %6d", playerName, score);
    }
}
