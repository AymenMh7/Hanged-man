package hangman.enums;

/**
 * Difficulty defines the parameters for every game mode — the
 * "rulebook" of Hangman. Each level carries:
 *   - singlePlayerChances : how many mistakes the solo player is allowed
 *   - minLength / maxLength : valid word-length window for the 1v1 secret word
 *
 * Single-player rule: harder difficulty = FEWER chances. The 1v1
 * tiebreaker mechanic uses {@link #getNextDifficulty()} to escalate
 * sudden-death rounds.
 */
public enum Difficulty {

    EASY   (10, 3, 5),
    MEDIUM ( 8, 5, 7),
    HARD   ( 6, 7, 10),
    INSANE ( 4, 8, 14);

    private final int singlePlayerChances;
    private final int minLength;
    private final int maxLength;

    Difficulty(int singlePlayerChances, int minLength, int maxLength) {
        this.singlePlayerChances = singlePlayerChances;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public int getSinglePlayerChances() {
        return singlePlayerChances;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Used by the 1v1 tiebreaker mechanic.
     * Returns the next level up; INSANE returns itself (already max).
     */
    public Difficulty getNextDifficulty() {
        Difficulty[] all = Difficulty.values();
        int next = this.ordinal() + 1;
        if (next >= all.length) {
            return INSANE; // already at the top
        }
        return all[next];
    }
}
