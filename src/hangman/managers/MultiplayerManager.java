package hangman.managers;

import hangman.db.DictionaryDAO;
import hangman.enums.Difficulty;
import hangman.models.GameSession;
import hangman.models.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Mode multijoueur 1v1.
 *
 * À chaque manche, un joueur saisit un mot secret et l'autre essaie
 * de le deviner. Le nombre de chances que reçoit le devineur dépend
 * du mot secret lui-même : nombre de lettres uniques, plafonné à 7.
 *
 * Si le match se termine à égalité, {@link #startTieBreaker()} récupère
 * un mot aléatoire à la difficulté supérieure et lance une manche de
 * mort subite.
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
        this.isPlayer1Turn   = true;   // p1 choisit le mot secret en premier
        this.isTieBreaker    = false;
        this.dictionary      = dictionary;
    }

    /**
     * Vérifie que le mot secret proposé respecte la fenêtre de longueur
     * de la difficulté de base ET ne contient que des lettres.
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
     * Compte les lettres uniques dans le mot secret — c'est le nombre
     * de chances que reçoit l'adversaire. Plafonné strictement à 7.
     *
     * (Logique de difficulté inversée : un mot plus long et plus varié
     *  donne en fait PLUS de chances au devineur, mais découvrir toutes
     *  ces lettres est en soi plus difficile.)
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
     * Démarre une demi-manche : celui qui a la main fournit le mot
     * secret, et l'autre joueur devient le devineur.
     */
    public void startHalfRound(String secretWord) {
        if (!validateSecretWord(secretWord)) {
            throw new IllegalArgumentException(
                "Le mot secret ne doit contenir que des lettres et avoir une longueur entre "
              + baseDifficulty.getMinLength() + " et "
              + baseDifficulty.getMaxLength());
        }
        int chances = calculateOpponentChances(secretWord);
        this.activeSession = new GameSession(secretWord, chances);
    }

    /**
     * Démarrage de manche de commodité qui consomme ce que le chooser
     * actuel a fourni. L'appelant (l'interface) préfère typiquement
     * {@link #startHalfRound(String)} pour pouvoir collecter le mot.
     */
    @Override
    public void startRound() {
        throw new UnsupportedOperationException(
            "Les manches multijoueur se lancent via startHalfRound(secret).");
    }

    /**
     * Bascule le drapeau de tour et avance le compteur de demi-manches.
     * Une "demi-manche" = un joueur a deviné un mot. {@link #totalRounds}
     * est également stocké en demi-manches pour que {@link #isMatchOver()}
     * compare des choses comparables.
     */
    public void switchTurn() {
        currentRound++;
        isPlayer1Turn = !isPlayer1Turn;
    }

    /** Renvoie le gagnant, ou {@code null} si le match est à égalité. */
    public Player determineWinner() {
        if (player1.getMatchScore() > player2.getMatchScore()) return player1;
        if (player2.getMatchScore() > player1.getMatchScore()) return player2;
        return null;
    }

    /**
     * Lance la mort subite : tire un mot à la difficulté supérieure
     * et crée une session que les deux joueurs vont s'affronter.
     *
     * L'interface est responsable de transmettre les lettres devinées
     * et de décider quel joueur résout en premier.
     */
    public void startTieBreaker() {
        startTieBreakerWithWord(pickTieBreakerWord());
    }

    /**
     * Tire un mot aléatoire à la difficulté supérieure SANS démarrer
     * de session. L'interface appelle ceci une seule fois au début
     * du départage pour que les deux capitaines puissent recevoir le
     * MÊME mot secret dans leurs demi-manches respectives.
     */
    public String pickTieBreakerWord() {
        Difficulty harder = baseDifficulty.getNextDifficulty();
        String word = dictionary.getRandomWord(harder);
        if (word == null) {
            throw new IllegalStateException(
                "Le départage nécessite un mot à la difficulté " + harder);
        }
        return word;
    }

    /**
     * Démarre une demi-manche de départage avec un mot pré-sélectionné,
     * de sorte que les deux capitaines puissent affronter la même cible.
     * Chaque appel produit une nouvelle GameSession (pour que le second
     * capitaine ne voie pas les lettres révélées du premier).
     */
    public void startTieBreakerWithWord(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("Le mot de départage ne peut pas être vide");
        }
        this.isTieBreaker = true;
        int chances = Math.min(
            baseDifficulty.getNextDifficulty().getSinglePlayerChances(), 7);
        this.activeSession = new GameSession(word, chances);
    }

    /** Remet à zéro les scores des deux joueurs et le compteur de manches. */
    public void resetMatch() {
        player1.resetMatchScore();
        player2.resetMatchScore();
        this.currentRound    = 0;
        this.isPlayer1Turn   = true;
        this.isTieBreaker    = false;
        this.activeSession   = null;
    }

    // ---------- accesseurs ----------

    public Player getPlayer1()          { return player1; }
    public Player getPlayer2()          { return player2; }
    public Difficulty getBaseDifficulty(){ return baseDifficulty; }
    public int  getTotalRounds()        { return totalRounds; }
    public int  getCurrentRound()       { return currentRound; }
    public boolean isPlayer1Turn()      { return isPlayer1Turn; }
    public boolean isTieBreaker()       { return isTieBreaker; }

    /** Le joueur qui est ACTUELLEMENT en train de deviner (pas celui qui choisit). */
    public Player getCurrentGuesser() {
        return isPlayer1Turn ? player2 : player1;
    }

    /** Le joueur qui est ACTUELLEMENT en train de choisir le mot secret. */
    public Player getCurrentChooser() {
        return isPlayer1Turn ? player1 : player2;
    }

    /** Renvoie true quand toutes les manches prévues ont été jouées. */
    public boolean isMatchOver() {
        return currentRound >= totalRounds;
    }
}
