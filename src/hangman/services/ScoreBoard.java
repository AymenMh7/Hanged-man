package hangman.services;

import hangman.db.ScoreDAO;
import hangman.enums.Difficulty;
import hangman.models.ScoreRecord;

import java.util.List;

/**
 * Couche de logique métier autour de {@link ScoreDAO}.
 *
 * Les managers parlent au ScoreBoard, jamais directement au DAO —
 * cela nous donne un point unique pour ajouter du cache, de la
 * validation ou de la journalisation plus tard, sans toucher au DAO.
 */
public class ScoreBoard {

    private final ScoreDAO scoreDAO;

    public ScoreBoard() {
        this(new ScoreDAO());
    }

    /** Constructeur qui accepte un DAO — utile pour les tests. */
    public ScoreBoard(ScoreDAO scoreDAO) {
        this.scoreDAO = scoreDAO;
    }

    public boolean isTop10(Difficulty diff, long score) {
        return scoreDAO.isTop10(diff, score);
    }

    public void addScore(Difficulty diff, String playerName, long score) {
        if (playerName == null || playerName.isBlank()) {
            playerName = "Anonyme";
        }
        scoreDAO.saveScore(diff, playerName, score);
    }

    public List<ScoreRecord> getTop10(Difficulty diff) {
        return scoreDAO.getTop10Scores(diff);
    }
}
