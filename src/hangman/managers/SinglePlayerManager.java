package hangman.managers;

import hangman.db.DictionaryDAO;
import hangman.enums.Difficulty;
import hangman.models.GameSession;
import hangman.services.ScoreBoard;

/**
 * Mode solo : tire un mot aléatoire du dictionnaire et donne au joueur
 * {@link Difficulty#getSinglePlayerChances()} erreurs à dépenser.
 *
 * Les conditions de victoire sont vérifiées via la GameSession active ;
 * cette classe décide uniquement quoi faire <em>après</em> la manche
 * (vérification de l'éligibilité + écriture dans le classement).
 */
public class SinglePlayerManager extends GameManager {

    private Difficulty currentDifficulty;
    private final DictionaryDAO dictionary;
    private final ScoreBoard    scoreBoard;

    public SinglePlayerManager(Difficulty difficulty) {
        this(difficulty, new DictionaryDAO(), new ScoreBoard());
    }

    /** Constructeur avec injection complète — utile pour les tests. */
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
                "Le dictionnaire ne contient aucun mot pour " + currentDifficulty);
        }
        int chances = currentDifficulty.getSinglePlayerChances();
        this.activeSession = new GameSession(word, chances);
    }

    /**
     * Renvoie true si le score combiné de la manche qui vient de se
     * terminer qualifie pour le top 10. Significatif uniquement après
     * une VICTOIRE — les manches perdues ne doivent pas être sauvegardées.
     */
    public boolean checkScoreboardEligibility() {
        if (activeSession == null || !activeSession.isWon()) {
            return false;
        }
        return scoreBoard.isTop10(currentDifficulty, activeSession.calculateScore());
    }

    /** Sauvegarde le nom du joueur et son score combiné dans le classement. */
    public void registerHighScore(String playerName) {
        if (activeSession == null || !activeSession.isWon()) return;
        scoreBoard.addScore(currentDifficulty, playerName,
                            activeSession.calculateScore());
    }

    public ScoreBoard getScoreBoard() {
        return scoreBoard;
    }
}
