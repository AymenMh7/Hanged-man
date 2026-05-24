package hangman.managers;

import hangman.models.GameSession;

/**
 * Abstract base of every game mode.
 *
 * Holds a reference to the currently active {@link GameSession}.
 * Concrete subclasses ({@code SinglePlayerManager}, {@code MultiplayerManager})
 * are responsible for creating that session and reacting to its outcome.
 */
public abstract class GameManager {

    /** The session currently being played. May be {@code null} between rounds. */
    protected GameSession activeSession;

    public GameSession getActiveSession() {
        return activeSession;
    }

    /** Each mode launches its rounds differently. */
    public abstract void startRound();
}
