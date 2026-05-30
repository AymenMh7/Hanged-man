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
 * Main container window.
 *
 * <h3>Game-scene layering (bottom → top)</h3>
 * <ol>
 * <li>{@link BackgroundPane} — stretches the current background image to fill the window.
 * Dynamically swaps images to show the pirate's hanging progress.</li>
 * <li>UI content           — VBox anchored to the RIGHT half:
 * word tiles, keyboard, status, quit button</li>
 * <li>Mistakes counter     — overlaid BOTTOM-LEFT corner via StackPane</li>
 * </ol>
 */

public class GameWindow extends Application {

    private static final String CSS       = "/hangman/ui/theme.css";
    private static final double INITIAL_W = 980;
    private static final double INITIAL_H = 760;

    // ---- state -------------------------------------------------------
    private Stage primaryStage;
    private Scene scene;

    private SinglePlayerManager singleMgr;
    private MultiplayerManager  multiMgr;

    // Live game HUD widgets
    private BackgroundPane  gameBgPane;
    private VirtualKeyboard keyboard;
    private HBox            wordTileBox;
    private Label           statusLabel;
    private Label           chancesLabel;

    public static void main(String[] args) { hangman.Main.main(args); }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Pirate's Hangman");
        stage.setOnCloseRequest(e -> DBConnection.getInstance().closeConnection());

        this.scene = new Scene(new StackPane(), INITIAL_W, INITIAL_H);
        attachCss(scene.getStylesheets());
        stage.setScene(scene);
        stage.setMinWidth(720);
        stage.setMinHeight(600);

        showMainMenu();
        stage.show();
    }

    // =================================================================
    //  MAIN MENU
    // =================================================================
    private void showMainMenu() {
        Label title = styledLabel("PIRATE'S COVE",  "title-main");
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

        VBox card = menuCard(460, 560);
        card.getChildren().addAll(title, tag, divider, new Region(),
                single, multi, scores, quit);
        setRoot(centerWrap(card));
    }

    // =================================================================
    //  DIFFICULTY PICKER
    // =================================================================
    private void showDifficultyPicker() {
        Label title    = styledLabel("Choose Your Crew",                  "title-sub");
        Label subtitle = styledLabel("Tougher seas leave fewer chances",  "tagline");

        VBox box = menuCard(440, 560);
        box.getChildren().addAll(title, subtitle, new Region());

        String[]    labels = { "Cabin Boy", "First Mate", "Captain", "Dread Pirate" };
        Difficulty[] vals  = Difficulty.values();
        for (int i = 0; i < vals.length; i++) {
            Difficulty d = vals[i];
            String lbl = String.format("%s  ·  %s  ·  %d chances",
                    labels[i], d.name(), d.getSinglePlayerChances());
            Button b = bigButton(lbl, d == Difficulty.INSANE ? "button-danger" : null);
            b.setOnAction(e -> startSinglePlayer(d));
            box.getChildren().add(b);
        }
        Button back = bigButton("← Back to Port", null);
        back.setOnAction(e -> showMainMenu());
        box.getChildren().addAll(new Region(), back);
        setRoot(centerWrap(box));
    }

    // =================================================================
    //  SINGLE PLAYER
    // =================================================================
    private void startSinglePlayer(Difficulty diff) {
        this.singleMgr = new SinglePlayerManager(diff);
        this.multiMgr  = null;
        try {
            singleMgr.startRound();
        } catch (RuntimeException ex) {
            showError("Could not start round: " + ex.getMessage()); return;
        }
        showGameScene("Solo Run · " + diff.name(), this::onSinglePlayerEnd);
    }

    private void onSinglePlayerEnd() {
        try {
            GameSession s = singleMgr.getActiveSession();
            if (s == null) { showError("Internal error: session was null."); showMainMenu(); return; }

            if (s.isWon()) {
                double secs  = s.calculateTimeScore() / 1000.0;
                long   score = s.calculateScore();
                String msg   = String.format(
                        "Treasure found in %.2fs · %d chances left%nScore: %d",
                        secs, s.getRemainingChances(), score);

                boolean eligible;
                try { eligible = singleMgr.checkScoreboardEligibility(); }
                catch (Exception ex) {
                    showError("Couldn't check leaderboard:\n" + ex.getMessage());
                    showInfo(msg); showMainMenu(); return;
                }

                if (eligible) {
                    TextInputDialog dlg = new TextInputDialog("Captain");
                    styleDialog(dlg.getDialogPane());
                    dlg.setTitle("Wall of Legends");
                    dlg.setHeaderText(msg + "\n\nTop 10!  Enter your name:");
                    dlg.setContentText("Captain:");
                    dlg.showAndWait().ifPresent(name -> {
                        String n = name.isBlank() ? "Anonymous" : name.trim();
                        try {
                            singleMgr.registerHighScore(n);
                            showInfo("⚓ " + n + " etched into the Wall of Legends!\nScore: " + score);
                        } catch (Exception ex) { showError("Couldn't save score:\n" + ex.getMessage()); }
                    });
                } else {
                    showInfo(msg + "\n\n(Not in the top 10.)");
                }
            } else {
                showInfo("The sea takes another soul.\nThe word was: " + s.getWordToGuess());
            }
            showMainMenu();
        } catch (Throwable t) {
            showError("Round end crashed:\n" + t.getMessage()); showMainMenu();
        }
    }

    // =================================================================
    //  MULTIPLAYER
    // =================================================================
    private void showPlayerSetup() {
        Label title = styledLabel("A Duel of Captains", "title-sub");

        TextField p1 = field("Captain 1"), p2 = field("Captain 2");
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
            this.multiMgr  = new MultiplayerManager(
                    new Player(n1), new Player(n2), diff.getValue(), rounds.getValue() * 2);
            this.singleMgr = null;
            promptForSecretWord();
        });
        back.setOnAction(e -> showMainMenu());

        VBox card = menuCard(440, 640);
        card.getChildren().addAll(title,
                fieldLabel("First captain"),  p1,
                fieldLabel("Second captain"), p2,
                fieldLabel("Word difficulty"), diff,
                fieldLabel("Rounds each"),    rounds,
                new Region(), start, back);
        setRoot(centerWrap(card));
    }

    private void promptForSecretWord() {
        Player chooser = multiMgr.getCurrentChooser();
        Player guesser = multiMgr.getCurrentGuesser();
        Difficulty d   = multiMgr.getBaseDifficulty();

        Label title = styledLabel(chooser.getAlias() + ", bury your treasure", "title-sub");
        Label rules = styledLabel(
                "Letters only · " + d.getMinLength() + "–" + d.getMaxLength()
                        + " chars · " + guesser.getAlias() + " — look away!", "tagline");

        PasswordField word = new PasswordField();
        word.setPromptText("type the secret word...");
        word.setMaxWidth(280);

        Label err = new Label();
        err.setStyle("-fx-text-fill: #A04030; -fx-font-weight: bold;");

        Button go      = bigButton("⚓  Bury the Word", "button-primary");
        Button abandon = bigButton("← Abandon Duel",   null);
        go.setOnAction(e -> {
            if (!multiMgr.validateSecretWord(word.getText())) {
                err.setText("⚠ Invalid — check length and letters only."); return;
            }
            multiMgr.startHalfRound(word.getText());
            showGameScene(
                    guesser.getAlias() + " hunts the word — round " + (multiMgr.getCurrentRound() + 1),
                    this::onMultiplayerHalfRoundEnd);
        });
        abandon.setOnAction(e -> showMainMenu());

        VBox card = menuCard(460, 440);
        card.getChildren().addAll(title, rules, word, go, abandon, err);
        setRoot(centerWrap(card));
    }

    private void onMultiplayerHalfRoundEnd() {
        try {
            GameSession s      = multiMgr.getActiveSession();
            Player      guesser = multiMgr.getCurrentGuesser();
            if (s.isWon()) {
                // CHANCES-ONLY for normal MP rounds: each captain has a
                // different word to solve, so racing the clock wouldn't
                // be fair. Time is only factored in on the tiebreaker.
                long pts = s.calculateChanceScore();
                guesser.addMatchScore((int) pts);
                showInfo(String.format("%s claims the treasure!%n%d chances left%n+%d doubloons",
                        guesser.getAlias(), s.getRemainingChances(), pts));
            } else {
                showInfo(guesser.getAlias() + " walks the plank.\nWord: " + s.getWordToGuess());
            }
            multiMgr.switchTurn();
            if (multiMgr.isMatchOver()) handleMatchEnd(); else promptForSecretWord();
        } catch (Throwable t) {
            showError("Duel handler crashed:\n" + t.getMessage()); showMainMenu();
        }
    }

    private void handleMatchEnd() {
        Player w = multiMgr.determineWinner();
        if (w == null) { showInfo("Tied! Sudden-death duel..."); runTieBreaker(); return; }
        String msg = "⚓ " + w.getAlias() + " rules the seas! ⚓\n\n"
                + multiMgr.getPlayer1() + "\n" + multiMgr.getPlayer2();
        Label title = styledLabel("THE VOYAGE ENDS", "title-sub");
        Label body  = new Label(msg);
        body.setStyle("-fx-font-size: 16px; -fx-text-fill: #3A2818; -fx-font-family: Georgia,serif;");
        body.setWrapText(true); body.setAlignment(Pos.CENTER);
        Button again = bigButton("⚓  Set Sail Again", "button-primary");
        Button menu  = bigButton("⌂  Return to Port",  null);
        again.setOnAction(e -> { multiMgr.resetMatch(); promptForSecretWord(); });
        menu .setOnAction(e -> showMainMenu());
        VBox card = menuCard(480, 420);
        card.getChildren().addAll(title, body, new Region(), again, menu);
        setRoot(centerWrap(card));
    }

    private void runTieBreaker() {
        // Sudden death is now TWO halves: both captains get a round at
        // the harder difficulty solving THE SAME secret word, then we
        // compare totals. Whoever scores more across the two halves
        // wins. If they STILL tie, another pair of half-rounds runs
        // with a fresh word.
        String word;
        try { word = multiMgr.pickTieBreakerWord(); }
        catch (RuntimeException ex) { showError("Tiebreaker failed: " + ex.getMessage()); showMainMenu(); return; }
        playTieBreakerHalf(false, word);
    }

    /**
     * Plays one half of a sudden-death tiebreaker for whichever captain
     * is the current guesser, then either hands it to the other captain
     * (when {@code isSecondHalf == false}) or wraps up via
     * {@link #handleMatchEnd()} (when both have played).
     *
     * Both halves receive the SAME secret word so the contest is fair.
     */
    private void playTieBreakerHalf(boolean isSecondHalf, String word) {
        try { multiMgr.startTieBreakerWithWord(word); }
        catch (RuntimeException ex) { showError("Tiebreaker failed: " + ex.getMessage()); showMainMenu(); return; }

        Player guesser = multiMgr.getCurrentGuesser();
        Player opponent = (guesser == multiMgr.getPlayer1())
                ? multiMgr.getPlayer2() : multiMgr.getPlayer1();

        String prefix = isSecondHalf
                ? "⚡  THE OTHER CAPTAIN'S TURN  ⚡"
                : "⚡  SUDDEN DEATH  ⚡";
        showInfo(prefix + "\n\n"
              + guesser.getAlias() + ", you face the storm.\n"
              + opponent.getAlias() + " — sit this one out.\n\n"
              + "Both chances AND time count this round.");

        String header = "⚡  " + guesser.getAlias() + " faces the storm — SUDDEN DEATH  ⚡";
        showGameScene(header, () -> {
            GameSession s = multiMgr.getActiveSession();
            Player g = multiMgr.getCurrentGuesser();
            if (s.isWon()) {
                // Tiebreaker uses the FULL formula (chances + time bonus).
                long pts  = s.calculateScore();
                double secs = s.calculateTimeScore() / 1000.0;
                g.addMatchScore((int) pts);
                showInfo(String.format("%s claims %d doubloons!%n%d chances left · %.2fs",
                        g.getAlias(), pts, s.getRemainingChances(), secs));
            } else {
                showInfo(g.getAlias() + " fell to the storm.\nWord: " + s.getWordToGuess());
            }

            if (!isSecondHalf) {
                // Pass the storm to the other captain — using the SAME
                // word so the contest is fair.
                multiMgr.switchTurn();
                playTieBreakerHalf(true, word);
            } else {
                // Both have weathered (or sunk in) the storm. Compare scores.
                handleMatchEnd();
            }
        });
    }

    // =================================================================
    //  GAME SCENE  ← KEY METHOD
    // =================================================================
    /**
     * Builds the game screen with a three-layer root:
     * <pre>
     *   StackPane (gameRoot)
     *     ├─ BackgroundPane  (bg image fills 100% of window)
     *     ├─ HangmanCanvas   (transparent Pane, also fills 100%;
     *     │                   pirate ImageViews bound to parent size)
     *     └─ uiLayer         (transparent StackPane for all controls)
     *          ├─ topBar     centred at top
     *          ├─ rightPane  word + keyboard + quit, anchored right
     *          └─ chancesLabel  anchored bottom-left
     * </pre>
     */
    private void showGameScene(String headerText, Runnable onRoundEnd) {

        // ── widgets ──────────────────────────────────────────────────
        keyboard    = new VirtualKeyboard();
        chancesLabel = new Label();
        chancesLabel.getStyleClass().add("chances-label");
        statusLabel  = new Label();

        wordTileBox = new HBox(8);
        wordTileBox.setAlignment(Pos.CENTER);

        // ── keyboard wiring ──────────────────────────────────────────
        keyboard.setOnLetterPressed(c -> {
            try {
                GameSession s = activeSession();
                if (s == null || s.isWon() || s.isLost()) return;
                s.guess(c);
                keyboard.disableAndFadeButton(c);
                updateUI();
                if (s.isWon() || s.isLost()) {
                    Platform.runLater(() -> {
                        try { onRoundEnd.run(); }
                        catch (Throwable t) {
                            showError("Round end failed:\n" + t.getMessage()); showMainMenu();
                        }
                    });
                }
            } catch (Throwable t) {
                showError("Keyboard error:\n" + t.getMessage());
            }
        });

        // ── quit button ───────────────────────────────────────────────
        Button quit = bigButton("⌂  Return to Port", "button-danger");
        quit.setPrefSize(220, 38);
        quit.setOnAction(e -> confirmAbandon());

        // ── TOP BAR: wooden sign centred at top ───────────────────────
        Label roundLbl = new Label(headerText);
        // 2. Upgraded its style class to 'game-title-sign' so it inherits the larger font/colors
        roundLbl.getStyleClass().add("game-title-sign");

        // 3. Changed the VBox to only include our dynamic label
        VBox topBar = new VBox(roundLbl);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(14, 0, 0, 0)); // Slightly increased top padding to center it on the wooden asset
        topBar.setBackground(Background.EMPTY);
        topBar.setPickOnBounds(false);

        // ── RIGHT PANE: word + status + keyboard + quit ───────────────
        // This panel is transparent and sits on the RIGHT side of the
        // window, leaving the left area free for the pirate to show through.
        VBox rightPane = new VBox(12, wordTileBox, statusLabel, keyboard, quit);
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setPadding(new Insets(0, 28, 20, 0));
        rightPane.setBackground(Background.EMPTY);
        rightPane.setMaxWidth(530);

        // ── UI LAYER: stacks topBar (top), rightPane (centre-right),
        //             chancesLabel (bottom-left) ─────────────────────
        StackPane uiLayer = new StackPane();
        uiLayer.setBackground(Background.EMPTY);
        uiLayer.setPickOnBounds(false);

        // topBar — top-centre
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        // rightPane — centre-right
        StackPane.setAlignment(rightPane, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightPane, new Insets(60, 0, 0, 0));

        // chancesLabel — bottom-left (in the dark footer strip)
        // chancesLabel — bottom-left (Moved up and right to fit inside the treasure chest)
        StackPane.setAlignment(chancesLabel, Pos.BOTTOM_LEFT);

// Adjust these values to position it perfectly over your asset box!
// Insets parameters are: (Top, Right, Bottom, Left)
        StackPane.setMargin(chancesLabel, new Insets(0, 0, 90, 180));

        uiLayer.getChildren().addAll(topBar, rightPane, chancesLabel);

        // ── GAME ROOT: bg | pirate overlay | ui ──────────────────────
        // The HangmanCanvas and uiLayer both fill the whole StackPane,
        // so the canvas's width/height bindings resolve to the full
        // window size and fractional positioning works correctly.
        StackPane gameRoot = new StackPane();

        // Assign the background to our class variable so we can change its image later
        gameBgPane = new BackgroundPane(new StackPane(), "/hangman/resources/background.png");

        // Added ONLY the bg and ui layer (canvas is gone!)
        gameRoot.getChildren().addAll(gameBgPane, uiLayer);

        StackPane.setAlignment(gameBgPane, Pos.TOP_LEFT);
        StackPane.setAlignment(uiLayer, Pos.TOP_LEFT);

        gameBgPane.prefWidthProperty().bind(gameRoot.widthProperty());
        gameBgPane.prefHeightProperty().bind(gameRoot.heightProperty());
        uiLayer.prefWidthProperty().bind(gameRoot.widthProperty());
        uiLayer.prefHeightProperty().bind(gameRoot.heightProperty());

        updateUI();
        setRoot(gameRoot);
    }

    private void confirmAbandon() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Abandon and return to port?\n(Round will be lost.)",
                ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Leaving so soon, captain?");
        styleDialog(a.getDialogPane());
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) { singleMgr = null; multiMgr = null; showMainMenu(); }
        });
    }

    private GameSession activeSession() {
        if (singleMgr != null) return singleMgr.getActiveSession();
        if (multiMgr  != null) return multiMgr.getActiveSession();
        return null;
    }

    /**
     * Refreshes all live HUD elements from the current {@link GameSession}.
     */
    public void updateUI() {
        GameSession s = activeSession();
        if (s == null) return;

        // ── hangman background stage ─────────────────────────────────
        int mistakes = s.getMaxChances() - s.getRemainingChances();
        if (mistakes == 0) {
            // No mistakes = clean background
            gameBgPane.setBackgroundImage("/hangman/resources/background.png");
        } else {
            String[] paths = {
                    "/hangman/resources/hat.png",
                    "/hangman/resources/hat_head.png",
                    "/hangman/resources/hat_head_torso.png",
                    "/hangman/resources/hat_head_torso_arms.png",
                    "/hangman/resources/full_pirate.png"
            };
            int stages = paths.length;
            // Calculate which stage image to show based on max chances
            int stage = (int) Math.ceil(((double) mistakes / s.getMaxChances()) * stages) - 1;
            stage = Math.min(stage, stages - 1);

            gameBgPane.setBackgroundImage(paths[stage]);
        }
        // ── word tiles ───────────────────────────────────────────────
        wordTileBox.getChildren().clear();
        for (char ch : s.getHiddenPassword()) {
            if (ch == ' ') {
                Region gap = new Region();
                gap.setPrefWidth(20);
                wordTileBox.getChildren().add(gap);
            } else {
                Label tile = new Label(ch == '_' ? "" : String.valueOf(ch).toUpperCase());
                tile.getStyleClass().add("word-tile");
                if (ch == '_') tile.getStyleClass().add("word-tile-hidden");
                wordTileBox.getChildren().add(tile);
            }
        }

        // ── mistakes counter ─────────────────────────────────────────
        chancesLabel.setText("MISTAKES: " + mistakes + "/" + s.getMaxChances());

        // ── status ───────────────────────────────────────────────────
        statusLabel.getStyleClass().removeAll("status-won", "status-lost");
        if      (s.isWon())  { statusLabel.setText("⚓  TREASURE FOUND  ⚓"); statusLabel.getStyleClass().add("status-won"); }
        else if (s.isLost()) { statusLabel.setText("☠  LOST AT SEA  ☠");    statusLabel.getStyleClass().add("status-lost"); }
        else                 { statusLabel.setText(""); }
    }

    // =================================================================
    //  LEADERBOARD
    // =================================================================
    private void showLeaderboardPicker() {
        Label title = styledLabel("Wall of Legends", "title-sub");
        Label sub   = styledLabel("Choose a rank to view", "tagline");
        VBox card   = menuCard(440, 560);
        card.getChildren().addAll(title, sub, new Region());

        String[]    tiers = { "Cabin Boy", "First Mate", "Captain", "Dread Pirate" };
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
        Label title  = styledLabel("Wall of Legends · " + d.name(), "title-sub");
        Label header = styledLabel("       Captain                 Score",   "tagline");

        ListView<String> list = new ListView<>();
        list.setPrefHeight(380);
        try {
            SinglePlayerManager tmp = new SinglePlayerManager(d);
            List<ScoreRecord> rows  = tmp.getScoreBoard().getTop10(d);
            int rank = 1;
            for (ScoreRecord r : rows) {
                String medal = rank == 1 ? "⚓" : rank == 2 ? "★" : rank == 3 ? "✦" : "·";
                list.getItems().add(String.format(" %s  %2d.  %s", medal, rank++, r));
            }
            if (rows.isEmpty()) list.getItems().add("   (no legends yet)");
        } catch (RuntimeException ex) { list.getItems().add("Error: " + ex.getMessage()); }

        Button back = bigButton("← Back", null);
        back.setOnAction(e -> showLeaderboardPicker());

        VBox card = menuCard(560, 620);
        card.getChildren().addAll(title, header, list, back);
        setRoot(centerWrap(card));
    }

    // =================================================================
    //  HELPERS
    // =================================================================
    private VBox menuCard(double maxW, double maxH) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card");
        card.setMaxWidth(maxW);
        card.setMaxHeight(maxH);
        card.setPadding(new Insets(30, 36, 30, 36));
        return card;
    }

    private Label styledLabel(String text, String cls) {
        Label l = new Label(text); l.getStyleClass().add(cls); return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text); l.getStyleClass().add("muted"); return l;
    }

    private TextField field(String prompt) {
        TextField t = new TextField(); t.setPromptText(prompt); t.setMaxWidth(280); return t;
    }

    private Button bigButton(String text, String extraClass) {
        Button b = new Button(text);
        b.setPrefSize(300, 48);
        if (extraClass != null) b.getStyleClass().add(extraClass);
        return b;
    }

    // BEFORE: return new BackgroundPane(inner);
    private Parent centerWrap(Node content) {
        StackPane inner = new StackPane(content);
        inner.setPadding(new Insets(20));
        // Pass your new menu background image here!
        return new BackgroundPane(inner, "/hangman/resources/menu_background.png");
    }

    private void setRoot(Parent root) { scene.setRoot(root); }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); styleDialog(a.getDialogPane()); a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null); styleDialog(a.getDialogPane()); a.showAndWait();
    }

    private void styleDialog(DialogPane pane) { attachCss(pane.getStylesheets()); }

    private void attachCss(List<String> sheets) {
        java.net.URL url = getClass().getResource(CSS);
        if (url != null) { sheets.add(url.toExternalForm()); return; }
        java.io.File f = new java.io.File("src/hangman/ui/theme.css");
        if (f.exists()) sheets.add(f.toURI().toString());
    }
}