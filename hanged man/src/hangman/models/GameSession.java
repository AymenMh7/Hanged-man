package hangman.models;

import java.util.HashSet;
import java.util.Set;

/**
 * GameSession is the generic "engine" running a single round of Hangman.
 *
 * It is intentionally dumb: it doesn't know whether it is part of a
 * single-player run or one half of a 1v1 match. The caller hands it a
 * word and a number of allowed mistakes, then feeds it guesses one
 * character at a time.
 */
public class GameSession {

    private final String  wordToGuess;
    private final char[]  hiddenPassword;
    private final int     maxChances;
    private int           remainingChances;
    private final Set<Character> guessedLetters = new HashSet<>();
    private final long    startTime;
    private long          endTime;
    private boolean       finished;

    public GameSession(String wordToGuess, int maxChances) {
        if (wordToGuess == null || wordToGuess.isEmpty()) {
            throw new IllegalArgumentException("wordToGuess must be non-empty");
        }
        this.wordToGuess      = wordToGuess.toLowerCase();
        this.hiddenPassword   = new char[this.wordToGuess.length()];
        for (int i = 0; i < hiddenPassword.length; i++) {
            hiddenPassword[i] = '_';
        }
        this.maxChances       = maxChances;
        this.remainingChances = maxChances;
        this.startTime        = System.currentTimeMillis();
        this.endTime          = 0L;
        this.finished         = false;
    }

    /**
     * Submits one guess.
     * @return true on a hit (letter is in the word), false otherwise.
     *         A letter already guessed counts as neither hit nor miss
     *         and returns false without spending a chance.
     */
    public boolean guess(char c) {
        if (finished) return false;
        char lower = Character.toLowerCase(c);
        if (!Character.isLetter(lower)) return false;
        if (guessedLetters.contains(lower)) return false;

        guessedLetters.add(lower);

        boolean hit = false;
        for (int i = 0; i < wordToGuess.length(); i++) {
            if (wordToGuess.charAt(i) == lower) {
                hiddenPassword[i] = lower;
                hit = true;
            }
        }
        if (!hit) {
            remainingChances--;
        }

        if (isWon() || isLost()) {
            this.endTime = System.currentTimeMillis();
            this.finished = true;
        }
        return hit;
    }

    public boolean isWon() {
        for (char c : hiddenPassword) {
            if (c == '_') return false;
        }
        return true;
    }

    public boolean isLost() {
        return remainingChances <= 0 && !isWon();
    }

    /**
     * Time the round took, in milliseconds. Lower is better.
     * If called before the round ends, returns the elapsed-so-far.
     */
    public long calculateTimeScore() {
        long stop = (endTime > 0) ? endTime : System.currentTimeMillis();
        return stop - startTime;
    }

    /**
     * Combined score used for the leaderboard AND for multiplayer points.
     * Higher is better. Returns 0 if the round was lost.
     *
     * Formula: 100 per remaining chance + a time bonus that starts at
     * 1000 and loses 10 per second elapsed (floor 0). So a fast clean
     * win with most chances intact scores around 1500–2000; a slow,
     * scraping win still scores 100–300.
     */
    public long calculateScore() {
        if (!isWon()) return 0;
        long seconds = Math.max(0, calculateTimeScore() / 1000);
        long chanceBonus = (long) remainingChances * 100L;
        long timeBonus   = Math.max(0L, 1000L - seconds * 10L);
        return chanceBonus + timeBonus;
    }

    // ---------- accessors ----------

    public String getWordToGuess()     { return wordToGuess; }
    public char[] getHiddenPassword()  { return hiddenPassword.clone(); }
    public int    getMaxChances()      { return maxChances; }
    public int    getRemainingChances(){ return remainingChances; }
    public Set<Character> getGuessedLetters() { return new HashSet<>(guessedLetters); }
    public long   getStartTime()       { return startTime; }
    public long   getEndTime()         { return endTime; }

    /** Convenience for the UI: "_ a _ _ l e". */
    public String getDisplayWord() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hiddenPassword.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(hiddenPassword[i]);
        }
        return sb.toString();
    }
}
