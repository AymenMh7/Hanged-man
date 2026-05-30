package hangman.models;

/**
 * Objet de transfert de données : une ligne de la table Leaderboard.
 * La valeur {@code score} est le score combiné (chances + temps) —
 * plus c'est ÉLEVÉ, mieux c'est. Implémente {@link Comparable} pour
 * que les appelants puissent trier les listes directement :
 * ordre naturel = meilleur (score le plus élevé) en premier.
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

    /** Ordre naturel = score le plus élevé en premier. */
    @Override
    public int compareTo(ScoreRecord other) {
        return Long.compare(other.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("%-20s %6d", playerName, score);
    }
}
