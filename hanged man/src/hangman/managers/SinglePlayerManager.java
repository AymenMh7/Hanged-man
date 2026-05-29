package hangman.managers;

import hangman.db.DictionaryDAO;
import hangman.enums.Difficulty;
import hangman.models.GameSession;
import hangman.services.ScoreBoard;

/**
 * Solo mode: pulls a random word from the dictionary and gives the
 * player {@link Difficulty#getSinglePlayerChances()} mistakes to spend.
 *
 * Win conditions are checked through the active GameSession; this
 * class only decides what to do <em>after</em> the round (eligibility
 * check + leaderboard write).
 */
public class SinglePlayerManager extends GameManager {

    private Difficulty currentDifficulty;
    private final DictionaryDAO dictionary;
    private final ScoreBoard    scoreBoard;

    public SinglePlayerManager(Difficulty difficulty) {
        this(difficulty, new DictionaryDAO(), new ScoreBoard());
    }

    /** Full-DI constructor — useful for tests. */
    public SinglePlayerManager(Difficulty difficulty, DictionaryDAO dictionary, ScoreBoard scoreBoard) {
        this.currentDifficulty = difficulty;
        this.dictionary        = dictionary;
        this.scoreBoard        = scoreBoard;
    }

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
    }

    @Override
    public void startRound() {
        String word = dictionary.getRandomWord(currentDifficulty);
        if (word == null) {
            throw new IllegalStateException(
                "Dictionary has no words for " + currentDifficulty);
        }
        int chances = currentDifficulty.getSinglePlayerChances();
        this.activeSession = new GameSession(word, chances);
    }

    /**
     * True if the just-finished round's combined score qualifies for
     * the top-10 board. Only meaningful after a WIN — lost rounds
     * shouldn't be saved.
     */
    public boolean checkScoreboardEligibility() {
        if (activeSession == null || !activeSession.isWon()) {
            return false;
        }
        return scoreBoard.isTop10(currentDifficulty, activeSession.calculateScore());
    }

    /** Persists the player's name + combined score to the leaderboard. */
    public void registerHighScore(String playerName) {
        if (activeSession == null || !activeSession.isWon()) return;
        scoreBoard.addScore(currentDifficulty, playerName,
                            activeSession.calculateScore());
    }

    public ScoreBoard getScoreBoard() {
        return scoreBoard;
    }
}
