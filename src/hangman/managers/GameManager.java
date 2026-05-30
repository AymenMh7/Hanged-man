package hangman.managers;

import hangman.models.GameSession;

/**
 * Classe abstraite de base pour chaque mode de jeu.
 *
 * Contient une référence à la {@link GameSession} actuellement active.
 * Les sous-classes concrètes ({@code SinglePlayerManager},
 * {@code MultiplayerManager}) sont responsables de la création de cette
 * session et de la réaction à son résultat.
 */
public abstract class GameManager {

    /** La session en cours de jeu. Peut être {@code null} entre les manches. */
    protected GameSession activeSession;

    public GameSession getActiveSession() {
        return activeSession;
    }

    /** Chaque mode lance ses manches différemment. */
    public abstract void startRound();
}
