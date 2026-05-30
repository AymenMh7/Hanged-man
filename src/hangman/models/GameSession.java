package hangman.models;

import java.util.HashSet;
import java.util.Set;

/**
 * GameSession est le "moteur" générique qui exécute une manche du Pendu.
 *
 * Elle est volontairement bête : elle ne sait pas si elle fait partie
 * d'une partie solo ou d'une moitié d'un match 1v1. L'appelant lui
 * fournit un mot et un nombre d'erreurs autorisées, puis lui transmet
 * les lettres devinées une par une.
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
     * Soumet une tentative de lettre.
     * @return true en cas de réussite (la lettre est dans le mot),
     *         false sinon. Une lettre déjà jouée ne compte ni comme
     *         réussite ni comme échec et renvoie false sans dépenser
     *         de chance.
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
     * Temps qu'a duré la manche, en millisecondes. Plus c'est faible,
     * mieux c'est. Appelée avant la fin de la manche, renvoie le temps
     * écoulé jusque-là.
     */
    public long calculateTimeScore() {
        long stop = (endTime > 0) ? endTime : System.currentTimeMillis();
        return stop - startTime;
    }

    /**
     * Score combiné : chances + bonus de temps. Utilisé pour le
     * classement SOLO et pour la manche de DÉPARTAGE multijoueur
     * (où le temps compte parce que les deux joueurs courent pour
     * résoudre le même mot).
     *
     * Formule : 100 par chance restante + un bonus de temps qui
     * commence à 1000 et perd 10 par seconde écoulée (minimum 0).
     * Une victoire rapide et nette avec la plupart des chances
     * intactes vaut autour de 1500–2000 ; une victoire de justesse
     * et lente reste à 100–300.
     *
     * Renvoie 0 si la manche a été perdue.
     */
    public long calculateScore() {
        if (!isWon()) return 0;
        long seconds = Math.max(0, calculateTimeScore() / 1000);
        long chanceBonus = (long) remainingChances * 100L;
        long timeBonus   = Math.max(0L, 1000L - seconds * 10L);
        return chanceBonus + timeBonus;
    }

    /**
     * Score basé uniquement sur les chances restantes — le temps est
     * ignoré. Utilisé pour les manches MULTIJOUEUR normales où chaque
     * capitaine a son propre mot à résoudre ; courir contre la montre
     * ne serait pas équitable.
     *
     * Formule : 100 par chance restante. Renvoie 0 si la manche est perdue.
     */
    public long calculateChanceScore() {
        if (!isWon()) return 0;
        return (long) remainingChances * 100L;
    }

    // ---------- accesseurs ----------

    public String getWordToGuess()     { return wordToGuess; }
    public char[] getHiddenPassword()  { return hiddenPassword.clone(); }
    public int    getMaxChances()      { return maxChances; }
    public int    getRemainingChances(){ return remainingChances; }
    public Set<Character> getGuessedLetters() { return new HashSet<>(guessedLetters); }
    public long   getStartTime()       { return startTime; }
    public long   getEndTime()         { return endTime; }

    /** Pratique pour l'interface : "_ a _ _ l e". */
    public String getDisplayWord() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hiddenPassword.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(hiddenPassword[i]);
        }
        return sb.toString();
    }
}
