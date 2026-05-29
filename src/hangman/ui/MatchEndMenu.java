package hangman.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Popup card shown after a 1v1 match ends.
 * Two choices: Play Again or Return to Menu.
 *
 * Visual styling comes from {@code theme.css} via the {@code .card}
 * class; this widget only assembles the layout.
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
