package hangman.managers;

import hangman.db.DictionaryDAO;
import hangman.enums.Difficulty;
import hangman.models.GameSession;
import hangman.models.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * 1v1 multiplayer mode.
 *
 * Each round, one player types a secret word and the other tries to
 * guess it. The number of chances the guesser receives is derived from
 * the secret word itself: unique-letter count, capped at 7.
 *
 * If the match ends tied, {@link #startTieBreaker()} fetches a random
 * word from the next-harder difficulty and runs a sudden-death round.
 */
public class MultiplayerManager extends GameManager {

    private final Player     player1;
    private final Player     player2;
    private final Difficulty baseDifficulty;
    private final int        totalRounds;
    private int              currentRound;

    private boolean isPlayer1Turn;
    private boolean isTieBreaker;
    private final DictionaryDAO dictionary;

    public MultiplayerManager(Player p1, Player p2, Difficulty baseDifficulty, int rounds) {
        this(p1, p2, baseDifficulty, rounds, new DictionaryDAO());
    }

    public MultiplayerManager(Player p1, Player p2, Difficulty baseDifficulty,
                              int rounds, DictionaryDAO dictionary) {
        this.player1         = p1;
        this.player2         = p2;
        this.baseDifficulty  = baseDifficulty;
        this.totalRounds     = rounds;
        this.currentRound    = 0;
        this.isPlayer1Turn   = true;   // p1 chooses the secret word first
        this.isTieBreaker    = false;
        this.dictionary      = dictionary;
    }

    /**
     * Verifies the proposed secret word fits the base difficulty's
     * length window AND contains only letters.
     */
    public boolean validateSecretWord(String word) {
        if (word == null) return false;
        String w = word.trim().toLowerCase();
        if (w.length() < baseDifficulty.getMinLength()) return false;
        if (w.length() > baseDifficulty.getMaxLength()) return false;
        for (int i = 0; i < w.length(); i++) {
            if (!Character.isLetter(w.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Counts unique letters in the secret word — that's how many
     * chances the opponent gets. Strictly capped at 7.
     *
     * (Inverted-difficulty logic: a longer, more varied word actually
     *  gives the guesser MORE chances, but discovering all those
     *  letters is itself harder.)
     */
    int calculateOpponentChances(String word) {
        if (word == null || word.isEmpty()) return 0;
        Set<Character> unique = new HashSet<>();
        for (char c : word.toLowerCase().toCharArray()) {
            if (Character.isLetter(c)) unique.add(c);
        }
        return Math.min(unique.size(), 7);
    }

    /**
     * Begins one half-round: the current chooser provides the secret
     * word, and the other player becomes the guesser.
     */
    public void startHalfRound(String secretWord) {
        if (!validateSecretWord(secretWord)) {
            throw new IllegalArgumentException(
                "Secret word must contain only letters and have length between "
              + baseDifficulty.getMinLength() + " and "
              + baseDifficulty.getMaxLength());
        }
        int chances = calculateOpponentChances(secretWord);
        this.activeSession = new GameSession(secretWord, chances);
    }

    /**
     * Convenience round-start that consumes whatever the current
     * chooser provided. The caller (UI) typically prefers
     * {@link #startHalfRound(String)} so it can collect the word.
     */
    @Override
    public void startRound() {
        throw new UnsupportedOperationException(
            "Multiplayer rounds are started via startHalfRound(secret).");
    }

    /**
     * Flips the turn flag and advances the half-round counter.
     * One "half-round" = one player guessed one word. {@link #totalRounds}
     * is also stored in half-rounds so {@link #isMatchOver()} compares
     * apples to apples.
     */
    public void switchTurn() {
        currentRound++;
        isPlayer1Turn = !isPlayer1Turn;
    }

    /** Returns the winner, or {@code null} if the match is tied. */
    public Player determineWinner() {
        if (player1.getMatchScore() > player2.getMatchScore()) return player1;
        if (player2.getMatchScore() > player1.getMatchScore()) return player2;
        return null;
    }

    /**
     * Runs sudden-death: picks a word from the next-harder difficulty
     * and creates a session both players will race through.
     *
     * The UI is responsible for actually feeding the guesses and
     * deciding which player solves it first.
     */
    public void startTieBreaker() {
        this.isTieBreaker = true;
        Difficulty harder = baseDifficulty.getNextDifficulty();
        String word = dictionary.getRandomWord(harder);
        if (word == null) {
            throw new IllegalStateException(
                "Tiebreaker requires a word at difficulty " + harder);
        }
        int chances = Math.min(harder.getSinglePlayerChances(), 7);
        this.activeSession = new GameSession(word, chances);
    }

    /** Zeros both players' scores and rewinds the round counter. */
    public void resetMatch() {
        player1.resetMatchScore();
        player2.resetMatchScore();
        this.currentRound    = 0;
        this.isPlayer1Turn   = true;
        this.isTieBreaker    = false;
        this.activeSession   = null;
    }

    // ---------- accessors ----------

    public Player getPlayer1()          { return player1; }
    public Player getPlayer2()          { return player2; }
    public Difficulty getBaseDifficulty(){ return baseDifficulty; }
    public int  getTotalRounds()        { return totalRounds; }
    public int  getCurrentRound()       { return currentRound; }
    public boolean isPlayer1Turn()      { return isPlayer1Turn; }
    public boolean isTieBreaker()       { return isTieBreaker; }

    /** The player who is CURRENTLY guessing (not the one choosing). */
    public Player getCurrentGuesser() {
        return isPlayer1Turn ? player2 : player1;
    }

    /** The player who is CURRENTLY choosing the secret word. */
    public Player getCurrentChooser() {
        return isPlayer1Turn ? player1 : player2;
    }

    /** True when every scheduled round has been played. */
    public boolean isMatchOver() {
        return currentRound >= totalRounds;
    }
}
