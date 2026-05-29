package hangman.models;

/**
 * A human participant inside a 1v1 multiplayer match.
 *
 * Note: this is separate from the leaderboard. matchScore only lives
 * for the duration of one head-to-head match; it is not persisted.
 */
public class Player {

    private final String alias;
    private int matchScore;

    public Player(String alias) {
        this.alias = alias;
        this.matchScore = 0;
    }

    public String getAlias() {
        return alias;
    }

    public int getMatchScore() {
        return matchScore;
    }

    /** Adds the given points to this player's running 1v1 match score. */
    public void addMatchScore(int points) {
        this.matchScore += points;
    }

    /** Wipes the match score back to zero (used at the start of a new match). */
    public void resetMatchScore() {
        this.matchScore = 0;
    }

    @Override
    public String toString() {
        return alias + " (" + matchScore + ")";
    }
}
