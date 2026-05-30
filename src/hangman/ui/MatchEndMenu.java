package hangman.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Carte popup affichée à la fin d'un match 1v1.
 * Deux choix : Rejouer ou Retour au Menu.
 *
 * Le style visuel vient de {@code theme.css} via la classe
 * {@code .card} ; ce widget ne fait que monter la mise en page.
 */
public class MatchEndMenu extends VBox {

    public MatchEndMenu(String message, Runnable onPlayAgain, Runnable onReturnToMenu) {
        setSpacing(18);
        setPadding(new Insets(32));
        setAlignment(Pos.CENTER);
        getStyleClass().add("card");

        Label title = new Label(message);
        title.getStyleClass().add("title-sub");
        title.setWrapText(true);

        Button again  = new Button("⚔  Play Again");
        Button menu   = new Button("⌂  Return to Menu");
        again.getStyleClass().add("button-primary");
        again.setPrefWidth(220);
        menu .setPrefWidth(220);

        again.setOnAction(e -> { if (onPlayAgain    != null) onPlayAgain.run(); });
        menu .setOnAction(e -> { if (onReturnToMenu != null) onReturnToMenu.run(); });

        getChildren().addAll(title, again, menu);
    }
}
