package hangman.enums;

/**
 * Difficulty définit les paramètres de chaque mode de jeu — le
 * "règlement" du Pendu. Chaque niveau contient :
 *   - singlePlayerChances : nombre d'erreurs autorisées en solo
 *   - minLength / maxLength : intervalle de longueur du mot secret en 1v1
 *
 * Règle solo : plus la difficulté est élevée, MOINS il y a de chances.
 * Le mécanisme de manche de départage en 1v1 utilise
 * {@link #getNextDifficulty()} pour passer à la difficulté supérieure.
 */
public enum Difficulty {

    EASY   (10, 3, 5),
    MEDIUM ( 8, 5, 7),
    HARD   ( 6, 7, 10),
    INSANE ( 4, 8, 14);

    private final int singlePlayerChances;
    private final int minLength;
    private final int maxLength;

    Difficulty(int singlePlayerChances, int minLength, int maxLength) {
        this.singlePlayerChances = singlePlayerChances;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    public int getSinglePlayerChances() {
        return singlePlayerChances;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Utilisée par le mécanisme de manche de départage en 1v1.
     * Renvoie le niveau supérieur ; INSANE se renvoie lui-même (déjà au max).
     */
    public Difficulty getNextDifficulty() {
        Difficulty[] all = Difficulty.values();
        int next = this.ordinal() + 1;
        if (next >= all.length) {
            return INSANE; // déjà au niveau maximum
        }
        return all[next];
    }
}
