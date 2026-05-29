package hangman.services;

import hangman.db.ScoreDAO;
import hangman.enums.Difficulty;
import hangman.models.ScoreRecord;

import java.util.List;

/**
 * Business-logic wrapper around {@link ScoreDAO}.
 *
 * The managers talk to the ScoreBoard, never to the DAO directly —
 * this gives us one place to add caching, validation, or logging
 * later without touching DAO code.
 */
public class ScoreBoard {

    private final ScoreDAO scoreDAO;

    public ScoreBoard() {
        this(new ScoreDAO());
    }

    /** Constructor that accepts a DAO — useful for tests. */
    public ScoreBoard(ScoreDAO scoreDAO) {
        this.scoreDAO = scoreDAO;
    }

    public boolean isTop10(Difficulty diff, long score) {
        return scoreDAO.isTop10(diff, score);
    }

    public void addScore(Difficulty diff, String playerName, long score) {
        if (playerName == null || playerName.isBlank()) {
            playerName = "Anonymous";
        }
        scoreDAO.saveScore(diff, playerName, score);
    }

    public List<ScoreRecord> getTop10(Difficulty diff) {
        return scoreDAO.getTop10Scores(diff);
    }
}
