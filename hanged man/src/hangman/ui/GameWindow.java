package hangman.ui;

import hangman.db.DBConnection;
import hangman.enums.Difficulty;
import hangman.managers.MultiplayerManager;
import hangman.managers.SinglePlayerManager;
import hangman.models.GameSession;
import hangman.models.Player;
import hangman.models.ScoreRecord;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Main container window — owns the {@link Stage} and swaps content via
 * a SINGLE persistent {@link Scene}. By replacing the scene's root
 * (instead of creating new Scenes), the window keeps its size — and
 * whatever maximize/fullscreen state the player set.
 *
 * Visual theme lives in {@code theme.css}; the scenic beach backdrop
 * lives in {@link BackgroundPane}.
 */
public class GameWindow extends Application {

    private static final String CSS = "/hangman/ui/theme.css";
    private static final double INITIAL_W = 980;
    private static final double INITIAL_H = 760;

    // ---- state -------------------------------------------------------
    private Stage primaryStage;
    private Scene scene;                 // ONE scene, root swapped on navigation

    private SinglePlayerManager singleMgr;
    private MultiplayerManager  multiMgr;

    // Bound widgets (held so updateUI() can refresh them)
    private HangmanCanvas   canvas;
    private VirtualKeyboard keyboard;
    private Label           wordLabel;
    private Label           statusLabel;
    private Label           chancesLabel;

    /**
     * Convenience entry point so an IDE can launch the game by
     * right-clicking either this file or {@code Main.java}. Delegates
     * to {@link hangman.Main#main(String[])} for the friendly
     * JavaFX-missing error handling.
     */
    public static void main(String[] args) {
        hangman.Main.main(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Pirate's Cove — A Hangman Adventure");
        stage.setOnCloseRequest(e -> DBConnection.getInstance().closeConnection());

        // Build the scene ONCE with a placeholder; subsequent navigation
        // calls setRoot(...) on this same Scene so the window doesn't
        // reset to its preferred size on each transition.
        this.scene = new Scene(new StackPane(), INITIAL_W, INITIAL_H);
        java.net.URL css = getClass().getResource(CSS);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            java.io.File f = new java.io.File("src/hangman/ui/theme.css");
            if (f.exists()) scene.getStylesheets().add(f.toURI().toString());
        }
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(620);

        showMainMenu();
        stage.show();
    }

    // =================================================================
    //                          MAIN MENU
    // =================================================================
    private void showMainMenu() {
        Label title = styledLabel("PIRATE'S COVE", "title-main");
        Label tag   = styledLabel("Solve the word — or swing from the yardarm", "tagline");

        Region divider = new Region();
        divider.getStyleClass().add("divider");
        divider.setPrefWidth(280);

        Button single = bigButton("⚓  Set Sail Alone",   "button-primary");
        Button multi  = bigButton("⚔  Duel of Captains", null);
        Button scores = bigButton("☠  Wall of Legends",  null);
        Button quit   = bigButton("✖  Abandon Ship",     "button-danger");

        single.setOnAction(e -> showDifficultyPicker());
        multi .setOnAction(e -> showPlayerSetup());
        scores.setOnAction(e -> showLeaderboardPicker());
        quit  .setOnAction(e -> { DBConnection.getInstance().closeConnection(); primaryStage.close(); });

        VBox card = new VBox(14, title, tag, divider,
                             new Region(), single, multi, scores, quit);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        card.setMaxHeight(560);
        card.setPadding(new Insets(36, 40, 36, 40));

        setRoot(centerWrap(card));
    }

    private Button bigButton(String text, String extraClass) {
        Button b = new Button(text);
        b.setPrefSize(300, 48);
        if (extraClass != null) b.getStyleClass().add(extraClass);
        return b;
    }

    // =================================================================
    //                       DIFFICULTY PICKER
    // =================================================================
    private void showDifficultyPicker() {
        Label title    = styledLabel("Choose Your Crew", "title-sub");
        Label subtitle = styledLabel("Tougher seas leave fewer chances", "tagline");

        VBox box = new VBox(12, title, subtitle, new Region());
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("card");
        box.setMaxWidth(440);
        box.setMaxHeight(560);
        box.setPadding(new Insets(32));

        String[] labels = { "Cabin Boy", "First Mate", "Captain", "Dread Pirate" };
        Difficulty[] vals = Difficulty.values();
        for (int i = 0; i < vals.length; i++) {
            Difficulty d = vals[i];
            String label = String.format("%s · %s · %d chances",
                                         labels[i], d.name(), d.getSinglePlayerChances());
            Button b = bigButton(label, d == Difficulty.INSANE ? "button-danger" : null);
            b.setOnAction(e -> startSinglePlayer(d));
            box.getChildren().add(b);
        }

        Button back = bigButton("← Back to Port", null);
        back.setOnAction(e -> showMainMenu());
        box.getChildren().addAll(new Region(), back);

        setRoot(centerWrap(box));
    }

    // =================================================================
    //                     SINGLE-PLAYER GAMEPLAY
    // =================================================================
    private void startSinglePlayer(Difficulty diff) {
        this.singleMgr = new SinglePlayerManager(diff);
        this.multiMgr  = null;
        try {
            singleMgr.startRound();
        } catch (RuntimeException ex) {
            showError("Could not start round: " + ex.getMessage());
            return;
        }
        showGameScene("Solo Run · " + diff.name(), this::onSinglePlayerEnd);
    }

    private void onSinglePlayerEnd() {
        // Top-level try/catch so an exception ANYWHERE in this handler
        // surfaces as a visible Alert instead of getting eaten by the
        // EDT (which would leave the player stranded on the game scene
        // with no popup, no navigation, no clue what went wrong).
        try {
            GameSession s = singleMgr.getActiveSession();
            if (s == null) {
                showError("Internal error: active session was null.");
                showMainMenu();
                return;
            }
            if (s.isWon()) {
                double secs  = s.calculateTimeScore() / 1000.0;
                long   score = s.calculateScore();
                String msg = String.format(
                    "Treasure found in %.2fs · %d chances left%nScore: %d",
                    secs, s.getRemainingChances(), score);

                // Eligibility check hits the DB — wrap it so a missing
                // column or connection drop tells us clearly.
                boolean eligible;
                try {
                    eligible = singleMgr.checkScoreboardEligibility();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showError("Couldn't check the leaderboard:\n" + ex.getMessage()
                            + "\n\nDid you re-run sql/schema.sql after\n"
                            + "the column was renamed (time_elapsed → score)?");
                    showInfo(msg);
                    showMainMenu();
                    return;
                }

                if (eligible) {
                    TextInputDialog name = new TextInputDialog("Captain");
                    styleDialog(name.getDialogPane());
                    name.setTitle("Wall of Legends");
                    name.setHeaderText(msg + "\n\nYou cracked the top 10!\n"
                            + "Your name will be carved into the Wall of Legends.");
                    name.setContentText("Captain's name:");

                    java.util.Optional<String> result = name.showAndWait();
                    if (result.isPresent()) {
                        String captain = result.get().isBlank()
                                ? "Anonymous"
                                : result.get().trim();
                        try {
                            singleMgr.registerHighScore(captain);
                            showInfo("⚓  " + captain + " etched onto the Wall of Legends!\n"
                                    + "Score saved: " + score);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showError("Couldn't save your score:\n" + ex.getMessage());
                        }
                    } else {
                        showInfo("Your treasure goes uncatalogued, captain.\nScore: " + score);
                    }
                } else {
                    showInfo(msg + "\n\n(Not enough for the top 10.)");
                }
            } else {
                showInfo("The sea takes another soul.\nThe word was: " + s.getWordToGuess());
            }
            showMainMenu();
        } catch (Throwable t) {
            t.printStackTrace();
            showError("Round end handler crashed:\n"
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            showMainMenu();
        }
    }

    // =================================================================
    //                    MULTIPLAYER SETUP / GAMEPLAY
    // =================================================================
    private void showPlayerSetup() {
        Label title = styledLabel("A Duel of Captains", "title-sub");

        TextField p1 = new TextField(); p1.setPromptText("Captain 1");
        TextField p2 = new TextField(); p2.setPromptText("Captain 2");
        p1.setMaxWidth(280); p2.setMaxWidth(280);

        ComboBox<Difficulty> diff = new ComboBox<>();
        diff.getItems().addAll(Difficulty.values());
        diff.getSelectionModel().select(Difficulty.MEDIUM);
        diff.setMaxWidth(280);

        Spinner<Integer> rounds = new Spinner<>(1, 10, 2);
        rounds.setMaxWidth(120);

        Button start = bigButton("⚔  Begin the Duel", "button-primary");
        Button back  = bigButton("← Back to Port", null);
        start.setOnAction(e -> {
            String n1 = p1.getText().isBlank() ? "P1" : p1.getText().trim();
            String n2 = p2.getText().isBlank() ? "P2" : p2.getText().trim();
            this.multiMgr = new MultiplayerManager(
                    new Player(n1), new Player(n2),
                    diff.getValue(), rounds.getValue() * 2
            );
            this.singleMgr = null;
            promptForSecretWord();
        });
        back.setOnAction(e -> showMainMenu());

        VBox card = new VBox(10,
                title,
                fieldLabel("First captain"),  p1,
                fieldLabel("Second captain"), p2,
                fieldLabel("Word difficulty"), diff,
                fieldLabel("Rounds each"),    rounds,
                new Region(), start, back);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(440);
        card.setMaxHeight(620);
        card.setPadding(new Insets(28));

        setRoot(centerWrap(card));
    }

    private void promptForSecretWord() {
        Player chooser = multiMgr.getCurrentChooser();
        Player guesser = multiMgr.getCurrentGuesser();
        Difficulty d   = multiMgr.getBaseDifficulty();

        Label title = styledLabel(chooser.getAlias() + ", bury your treasure", "title-sub");
        Label rules = styledLabel(
            "Letters only · " + d.getMinLength() + "–" + d.getMaxLength()
            + " characters · " + guesser.getAlias() + " — look away!",
            "tagline");

        PasswordField word = new PasswordField();
        word.setPromptText("type the secret word...");
        word.setMaxWidth(280);

        Label err = new Label();
        err.setStyle("-fx-text-fill: #A04030; -fx-font-weight: bold;");

        Button go = bigButton("⚓  Bury the Word", "button-primary");
        Button abandon = bigButton("← Abandon Duel", null);
        go.setOnAction(e -> {
            String secret = word.getText();
            if (!multiMgr.validateSecretWord(secret)) {
                err.setText("⚠ Invalid word. Check length and letters-only.");
                return;
            }
            multiMgr.startHalfRound(secret);
            showGameScene(
                guesser.getAlias() + " hunts the word — round "
                + (multiMgr.getCurrentRound() + 1),
                this::onMultiplayerHalfRoundEnd);
        });
        abandon.setOnAction(e -> showMainMenu());

        VBox card = new VBox(14, title, rules, word, go, abandon, err);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(460);
        card.setMaxHeight(440);
        card.setPadding(new Insets(32));

        setRoot(centerWrap(card));
    }

    private void onMultiplayerHalfRoundEnd() {
        try {
            GameSession s = multiMgr.getActiveSession();
            Player guesser = multiMgr.getCurrentGuesser();
            if (s.isWon()) {
                // Combined score IS the doubloon haul.
                long pts = s.calculateScore();
                guesser.addMatchScore((int) pts);
                double secs = s.calculateTimeScore() / 1000.0;
                showInfo(String.format(
                    "%s claims the treasure!%n%d chances left · %.2fs%n+%d doubloons",
                    guesser.getAlias(), s.getRemainingChances(), secs, pts));
            } else {
                showInfo(guesser.getAlias() + " walks the plank.\nThe word was: " + s.getWordToGuess());
            }

            multiMgr.switchTurn();

            if (multiMgr.isMatchOver()) {
                handleMatchEnd();
            } else {
                promptForSecretWord();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            showError("Duel handler crashed:\n"
                    + t.getClass().getSimpleName() + ": " + t.getMessage());
            showMainMenu();
        }
    }

    private void handleMatchEnd() {
        Player winner = multiMgr.determineWinner();
        if (winner == null) {
            showInfo("Both crews tied! A sudden-death duel begins...");
            runTieBreaker();
            return;
        }
        String msg = "⚓ " + winner.getAlias() + " rules the seas! ⚓\n\n"
                   + multiMgr.getPlayer1() + "\n"
                   + multiMgr.getPlayer2();
        showMatchEndPopup(msg);
    }

    private void runTieBreaker() {
        try {
            multiMgr.startTieBreaker();
        } catch (RuntimeException ex) {
            showError("Tiebreaker failed: " + ex.getMessage());
            showMainMenu();
            return;
        }
        showGameScene("⚡  THE STORM BREAKS  ⚡", () -> {
            GameSession s = multiMgr.getActiveSession();
            Player guesser = multiMgr.getCurrentGuesser();
            if (s.isWon()) {
                // Combined score from the tiebreaker tilts the match.
                long pts = s.calculateScore();
                guesser.addMatchScore((int) pts);
                showInfo(guesser.getAlias() + " survives the tempest!  +" + pts + " doubloons");
            } else {
                showInfo("The storm claims no one. The word was: " + s.getWordToGuess());
                multiMgr.switchTurn();
            }
            handleMatchEnd();
        });
    }

    private void showMatchEndPopup(String msg) {
        Label title = styledLabel("THE VOYAGE ENDS", "title-sub");
        Label body  = new Label(msg);
        body.setStyle("-fx-font-size: 16px; -fx-text-fill: #3A2818; -fx-font-family: Georgia,serif;");
        body.setWrapText(true);
        body.setAlignment(Pos.CENTER);

        Button again = bigButton("⚓  Set Sail Again", "button-primary");
        Button menu  = bigButton("⌂  Return to Port", null);
        again.setOnAction(e -> { multiMgr.resetMatch(); promptForSecretWord(); });
        menu .setOnAction(e -> showMainMenu());

        VBox card = new VBox(16, title, body, new Region(), again, menu);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(480);
        card.setMaxHeight(420);
        card.setPadding(new Insets(36));

        setRoot(centerWrap(card));
    }

    // =================================================================
    //                          GAME SCENE
    // =================================================================
    private void showGameScene(String headerText, Runnable onRoundEnd) {
        Label header = styledLabel(headerText, "title-sub");

        canvas   = new HangmanCanvas();
        keyboard = new VirtualKeyboard();

        wordLabel    = new Label();
        wordLabel.getStyleClass().add("word-display");

        chancesLabel = new Label();
        chancesLabel.getStyleClass().add("chances-label");

        statusLabel  = new Label();

        // Wire keyboard presses into the active session.
        keyboard.setOnLetterPressed(c -> {
            try {
                GameSession s = activeSession();
                if (s == null || s.isWon() || s.isLost()) return;
                s.guess(c);
                keyboard.disableAndFadeButton(c);
                updateUI();
                if (s.isWon() || s.isLost()) {
                    // Wrap the runLater target so a thrown exception
                    // surfaces as an Alert rather than vanishing on EDT.
                    Platform.runLater(() -> {
                        try {
                            onRoundEnd.run();
                        } catch (Throwable t) {
                            t.printStackTrace();
                            showError("Round end failed:\n"
                                    + t.getClass().getSimpleName() + ": " + t.getMessage());
                            showMainMenu();
                        }
                    });
                }
            } catch (Throwable t) {
                t.printStackTrace();
                showError("Keyboard handler failed:\n" + t.getMessage());
            }
        });

        // Quit-game button: confirm + drop back to main menu.
        Button quitGame = bigButton("⌂  Return to Port", "button-danger");
        quitGame.setPrefSize(220, 38);
        quitGame.setOnAction(e -> confirmAbandon());

        VBox rightPane = new VBox(18, wordLabel, chancesLabel, statusLabel, keyboard, quitGame);
        rightPane.setAlignment(Pos.CENTER);

        VBox leftPane = new VBox(canvas);
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setPadding(new Insets(10));

        HBox center = new HBox(40, leftPane, rightPane);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20));
        center.getStyleClass().add("card");

        VBox root = new VBox(16, header, center);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        updateUI();
        setRoot(centerWrap(root));
    }

    /** Asks the player to confirm abandoning the round. */
    private void confirmAbandon() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Abandon the voyage and return to port?\n"
              + "(Your current round will be lost.)",
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Leaving so soon, captain?");
        styleDialog(a.getDialogPane());
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                singleMgr = null;
                multiMgr  = null;
                showMainMenu();
            }
        });
    }

    private GameSession activeSession() {
        if (singleMgr != null) return singleMgr.getActiveSession();
        if (multiMgr  != null) return multiMgr.getActiveSession();
        return null;
    }

    public void updateUI() {
        GameSession s = activeSession();
        if (s == null) return;
        canvas.drawHangman(s.getMaxChances(), s.getRemainingChances());
        wordLabel.setText(s.getDisplayWord().toUpperCase());
        chancesLabel.setText("✦ Chances remaining: " + s.getRemainingChances()
                + " / " + s.getMaxChances());
        statusLabel.getStyleClass().removeAll("status-won", "status-lost");
        if (s.isWon()) {
            statusLabel.setText("⚓  TREASURE FOUND  ⚓");
            statusLabel.getStyleClass().add("status-won");
        } else if (s.isLost()) {
            statusLabel.setText("☠  LOST AT SEA  ☠");
            statusLabel.getStyleClass().add("status-lost");
        } else {
            statusLabel.setText("");
        }
    }

    // =================================================================
    //                       LEADERBOARD VIEW
    // =================================================================
    private void showLeaderboardPicker() {
        Label title = styledLabel("Wall of Legends", "title-sub");
        Label sub   = styledLabel("Choose a rank to view", "tagline");

        VBox card = new VBox(12, title, sub, new Region());
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(440);
        card.setMaxHeight(560);
        card.setPadding(new Insets(32));

        String[] tiers = { "Cabin Boy", "First Mate", "Captain", "Dread Pirate" };
        Difficulty[] vals = Difficulty.values();
        for (int i = 0; i < vals.length; i++) {
            Difficulty d = vals[i];
            Button b = bigButton(tiers[i] + "  ·  " + d.name(), null);
            b.setOnAction(e -> showLeaderboard(d));
            card.getChildren().add(b);
        }
        Button back = bigButton("← Back to Port", null);
        back.setOnAction(e -> showMainMenu());
        card.getChildren().addAll(new Region(), back);

        setRoot(centerWrap(card));
    }

    private void showLeaderboard(Difficulty d) {
        Label title = styledLabel("Wall of Legends · " + d.name(), "title-sub");
        Label header = styledLabel("       Captain                 Score", "tagline");

        ListView<String> list = new ListView<>();
        list.setPrefHeight(380);
        try {
            SinglePlayerManager tmp = new SinglePlayerManager(d);
            List<ScoreRecord> rows = tmp.getScoreBoard().getTop10(d);
            int rank = 1;
            for (ScoreRecord r : rows) {
                String medal = rank == 1 ? "⚓" : rank == 2 ? "★" : rank == 3 ? "✦" : "·";
                list.getItems().add(String.format(" %s  %2d.  %s", medal, rank++, r));
            }
            if (rows.isEmpty()) {
                list.getItems().add("   (no legends yet — be the first to claim the treasure)");
            }
        } catch (RuntimeException ex) {
            list.getItems().add("Error: " + ex.getMessage());
        }

        Button back = bigButton("← Back", null);
        back.setOnAction(e -> showLeaderboardPicker());

        VBox card = new VBox(12, title, header, list, back);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(560);
        card.setMaxHeight(620);
        card.setPadding(new Insets(24));

        setRoot(centerWrap(card));
    }

    // =================================================================
    //                          HELPERS
    // =================================================================
    private Label styledLabel(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }

    /**
     * Wraps any node in a {@link BackgroundPane} so the pirate-cove
     * scene sits behind it on every screen.
     */
    private Parent centerWrap(Node content) {
        StackPane inner = new StackPane(content);
        inner.setPadding(new Insets(20));
        return new BackgroundPane(inner);
    }

    /**
     * Swaps the persistent scene's root — preserves window size,
     * position, and maximize/fullscreen state across screens.
     */
    private void setRoot(Parent root) {
        scene.setRoot(root);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        styleDialog(a.getDialogPane());
        a.showAndWait();
    }

    private void styleDialog(DialogPane pane) {
        java.net.URL css = getClass().getResource(CSS);
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        } else {
            java.io.File f = new java.io.File("src/hangman/ui/theme.css");
            if (f.exists()) pane.getStylesheets().add(f.toURI().toString());
        }
    }
}
