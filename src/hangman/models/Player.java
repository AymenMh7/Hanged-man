package hangman.models;

/**
 * Un participant humain dans un match multijoueur 1v1.
 *
 * Remarque : ceci est indépendant du classement. matchScore n'existe
 * que pour la durée d'un seul match en face à face ; il n'est pas persisté.
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

    /** Ajoute les points donnés au score 1v1 en cours de ce joueur. */
    public void addMatchScore(int points) {
        this.matchScore += points;
    }

    /** Remet le score du match à zéro (utilisé au début d'un nouveau match). */
    public void resetMatchScore() {
        this.matchScore = 0;
    }

    @Override
    public String toString() {
        return alias + " (" + matchScore + ")";
    }
}
