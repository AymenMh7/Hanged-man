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
 * Fenêtre conteneur principale.
 *
 * <h3>Empilage de la scène de jeu (du bas vers le haut)</h3>
 * <ol>
 * <li>{@link BackgroundPane} — étire l'image de fond actuelle pour
 * remplir la fenêtre. Change dynamiquement les images pour montrer
 * la progression de la pendaison du pirate.</li>
 * <li>Contenu UI            — VBox ancré à la moitié DROITE :
 * tuiles du mot, clavier, statut, bouton quitter</li>
 * <li>Compteur d'erreurs    — superposé en BAS À GAUCHE via un StackPane</li>
 * </ol>
 */

public class GameWindow extends Application {

    private static final String CSS       = "/hangman/ui/theme.css";
    private static final double INITIAL_W = 980;
    private static final double INITIAL_H = 760;

    // ---- état --------------------------------------------------------
    private Stage primaryStage;
    private Scene scene;

    private SinglePlayerManager singleMgr;
    private MultiplayerManager  multiMgr;

    // Widgets HUD du jeu en direct
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
    //  MENU PRINCIPAL
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
    //  SÉLECTEUR DE DIFFICULTÉ
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
    //  MODE SOLO
    // =================================================================
    private void startSinglePlayer(Difficulty diff) {
        this.singleMgr = new SinglePlayerManager(diff);
        this.multiMgr  = null;
        try {
            singleMgr.startRound();
        } catch (RuntimeException ex) {
            showError("Impossible de démarrer la manche : " + ex.getMessage()); return;
        }
        showGameScene("Solo Run · " + diff.name(), this::onSinglePlayerEnd);
    }

    private void onSinglePlayerEnd() {
        try {
            GameSession s = singleMgr.getActiveSession();
            if (s == null) { showError("Erreur interne : session nulle."); showMainMenu(); return; }

            if (s.isWon()) {
                double secs  = s.calculateTimeScore() / 1000.0;
                long   score = s.calculateScore();
                String msg   = String.format(
                        "Treasure found in %.2fs · %d chances left%nScore: %d",
                        secs, s.getRemainingChances(), score);

                boolean eligible;
                try { eligible = singleMgr.checkScoreboardEligibility(); }
                catch (Exception ex) {
                    showError("Impossible de vérifier le classement :\n" + ex.getMessage());
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
                        } catch (Exception ex) { showError("Impossible de sauvegarder le score :\n" + ex.getMessage()); }
                    });
                } else {
                    showInfo(msg + "\n\n(Not in the top 10.)");
                }
            } else {
                showInfo("The sea takes another soul.\nThe word was: " + s.getWordToGuess());
            }
            showMainMenu();
        } catch (Throwable t) {
            showError("Plantage en fin de manche :\n" + t.getMessage()); showMainMenu();
        }
    }

    // =================================================================
    //  MULTIJOUEUR
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
                // CHANCES UNIQUEMENT pour les manches MP normales :
                // chaque capitaine a un mot différent à résoudre, donc
                // courir contre la montre ne serait pas équitable. Le
                // temps n'est pris en compte que pour le départage.
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
            showError("Plantage du gestionnaire de duel :\n" + t.getMessage()); showMainMenu();
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
        // La mort subite est maintenant en DEUX demi-manches : les deux
        // capitaines reçoivent une manche à la difficulté supérieure
        // pour résoudre LE MÊME mot secret, puis on compare les totaux.
        // Celui qui marque le plus à travers les deux demi-manches
        // gagne. Si l'égalité PERSISTE, une autre paire de demi-manches
        // est jouée avec un nouveau mot.
        String word;
        try { word = multiMgr.pickTieBreakerWord(); }
        catch (RuntimeException ex) { showError("Échec du départage : " + ex.getMessage()); showMainMenu(); return; }
        playTieBreakerHalf(false, word);
    }

    /**
     * Joue une moitié d'un départage en mort subite pour le capitaine
     * qui est actuellement le devineur, puis soit passe la main à
     * l'autre capitaine (quand {@code isSecondHalf == false}), soit
     * conclut via {@link #handleMatchEnd()} (quand les deux ont joué).
     *
     * Les deux moitiés reçoivent le MÊME mot secret pour que la
     * compétition soit équitable.
     */
    private void playTieBreakerHalf(boolean isSecondHalf, String word) {
        try { multiMgr.startTieBreakerWithWord(word); }
        catch (RuntimeException ex) { showError("Échec du départage : " + ex.getMessage()); showMainMenu(); return; }

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
                // Le départage utilise la formule COMPLÈTE (chances + bonus de temps).
                long pts  = s.calculateScore();
                double secs = s.calculateTimeScore() / 1000.0;
                g.addMatchScore((int) pts);
                showInfo(String.format("%s claims %d doubloons!%n%d chances left · %.2fs",
                        g.getAlias(), pts, s.getRemainingChances(), secs));
            } else {
                showInfo(g.getAlias() + " fell to the storm.\nWord: " + s.getWordToGuess());
            }

            if (!isSecondHalf) {
                // Passe la tempête à l'autre capitaine — en utilisant
                // le MÊME mot pour que la compétition reste équitable.
                multiMgr.switchTurn();
                playTieBreakerHalf(true, word);
            } else {
                // Les deux ont affronté (ou sombré dans) la tempête. On compare les scores.
                handleMatchEnd();
            }
        });
    }

    // =================================================================
    //  SCÈNE DE JEU  ← MÉTHODE CLÉ
    // =================================================================
    /**
     * Construit l'écran de jeu avec une racine en trois couches :
     * <pre>
     *   StackPane (gameRoot)
     *     ├─ BackgroundPane  (l'image de fond remplit 100% de la fenêtre)
     *     ├─ HangmanCanvas   (Pane transparent qui remplit aussi 100% ;
     *     │                   ImageViews du pirate liés à la taille parente)
     *     └─ uiLayer         (StackPane transparent pour tous les contrôles)
     *          ├─ topBar     centré en haut
     *          ├─ rightPane  mot + clavier + quitter, ancré à droite
     *          └─ chancesLabel  ancré en bas à gauche
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

        // ── câblage du clavier ───────────────────────────────────────
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
                            showError("Échec en fin de manche :\n" + t.getMessage()); showMainMenu();
                        }
                    });
                }
            } catch (Throwable t) {
                showError("Erreur clavier :\n" + t.getMessage());
            }
        });

        // ── bouton quitter ────────────────────────────────────────────
        Button quit = bigButton("⌂  Return to Port", "button-danger");
        quit.setPrefSize(220, 38);
        quit.setOnAction(e -> confirmAbandon());

        // ── BARRE DU HAUT : pancarte en bois centrée en haut ──────────
        Label roundLbl = new Label(headerText);
        // 2. Classe de style passée à 'game-title-sign' pour qu'elle
        //    hérite des couleurs/police plus grandes.
        roundLbl.getStyleClass().add("game-title-sign");

        // 3. Le VBox ne contient plus que notre label dynamique
        VBox topBar = new VBox(roundLbl);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(14, 0, 0, 0)); // padding du haut un peu augmenté pour centrer sur la pancarte en bois
        topBar.setBackground(Background.EMPTY);
        topBar.setPickOnBounds(false);

        // ── PANNEAU DROIT : mot + statut + clavier + quitter ──────────
        // Ce panneau est transparent et se place sur le CÔTÉ DROIT de
        // la fenêtre, laissant la zone gauche libre pour que le pirate
        // soit visible à travers.
        VBox rightPane = new VBox(12, wordTileBox, statusLabel, keyboard, quit);
        rightPane.setAlignment(Pos.CENTER);
        rightPane.setPadding(new Insets(0, 28, 20, 0));
        rightPane.setBackground(Background.EMPTY);
        rightPane.setMaxWidth(530);

        // ── COUCHE UI : empile topBar (haut), rightPane (centre-droite),
        //               chancesLabel (bas-gauche) ─────────────────────
        StackPane uiLayer = new StackPane();
        uiLayer.setBackground(Background.EMPTY);
        uiLayer.setPickOnBounds(false);

        // topBar — haut-centre
        StackPane.setAlignment(topBar, Pos.TOP_CENTER);

        // rightPane — centre-droite
        StackPane.setAlignment(rightPane, Pos.CENTER_RIGHT);
        StackPane.setMargin(rightPane, new Insets(60, 0, 0, 0));

        // chancesLabel — bas-gauche (dans la bande sombre du pied)
        // chancesLabel — bas-gauche (remonté et déplacé à droite pour s'insérer dans le coffre au trésor)
        StackPane.setAlignment(chancesLabel, Pos.BOTTOM_LEFT);

// Ajustez ces valeurs pour le positionner parfaitement sur votre asset !
// Paramètres d'Insets : (Haut, Droite, Bas, Gauche)
        StackPane.setMargin(chancesLabel, new Insets(0, 0, 90, 180));

        uiLayer.getChildren().addAll(topBar, rightPane, chancesLabel);

        // ── RACINE DU JEU : fond | superposition pirate | ui ─────────
        // Le HangmanCanvas et l'uiLayer remplissent tous deux la totalité
        // du StackPane, donc les liaisons largeur/hauteur du canvas se
        // résolvent à la taille complète de la fenêtre et le
        // positionnement fractionnel fonctionne correctement.
        StackPane gameRoot = new StackPane();

        // Affecte le fond à notre variable de classe pour pouvoir changer son image plus tard
        gameBgPane = new BackgroundPane(new StackPane(), "/hangman/resources/background.png");

        // Ajouté UNIQUEMENT le bg et la couche ui (le canvas a disparu !)
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
     * Rafraîchit tous les éléments HUD en direct depuis la
     * {@link GameSession} actuelle.
     */
    public void updateUI() {
        GameSession s = activeSession();
        if (s == null) return;

        // ── étape du fond du pendu ───────────────────────────────────
        int mistakes = s.getMaxChances() - s.getRemainingChances();
        if (mistakes == 0) {
            // Aucune erreur = fond propre
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
            // Calcule quelle image d'étape afficher selon le nombre max de chances
            int stage = (int) Math.ceil(((double) mistakes / s.getMaxChances()) * stages) - 1;
            stage = Math.min(stage, stages - 1);

            gameBgPane.setBackgroundImage(paths[stage]);
        }
        // ── tuiles du mot ────────────────────────────────────────────
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

        // ── compteur d'erreurs ───────────────────────────────────────
        chancesLabel.setText("MISTAKES: " + mistakes + "/" + s.getMaxChances());

        // ── statut ───────────────────────────────────────────────────
        statusLabel.getStyleClass().removeAll("status-won", "status-lost");
        if      (s.isWon())  { statusLabel.setText("⚓  TREASURE FOUND  ⚓"); statusLabel.getStyleClass().add("status-won"); }
        else if (s.isLost()) { statusLabel.setText("☠  LOST AT SEA  ☠");    statusLabel.getStyleClass().add("status-lost"); }
        else                 { statusLabel.setText(""); }
    }

    // =================================================================
    //  CLASSEMENT
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
        } catch (RuntimeException ex) { list.getItems().add("Erreur : " + ex.getMessage()); }

        Button back = bigButton("← Back", null);
        back.setOnAction(e -> showLeaderboardPicker());

        VBox card = menuCard(560, 620);
        card.getChildren().addAll(title, header, list, back);
        setRoot(centerWrap(card));
    }

    // =================================================================
    //  AIDES
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

    // AVANT : return new BackgroundPane(inner);
    private Parent centerWrap(Node content) {
        StackPane inner = new StackPane(content);
        inner.setPadding(new Insets(20));
        // Passez ici votre nouvelle image de fond du menu !
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
