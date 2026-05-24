package hangman.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A clickable A–Z keyboard, themed via {@code theme.css} (the {@code .key}
 * style class). Each button fires {@code onLetterPressed} once when
 * clicked. After a letter is played, the matching button fades out and
 * is disabled.
 *
 * Uses a column of HBoxes (one per row) so the offset of each row gives
 * the natural staircase of a real keyboard, instead of a rigid grid.
 */
public class VirtualKeyboard extends VBox {

    private final Map<Character, Button> buttons = new HashMap<>();
    private Consumer<Character> onLetterPressed;

    public VirtualKeyboard() {
        setSpacing(6);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(6));

        String[] rows = { "qwertyuiop", "asdfghjkl", "zxcvbnm" };
        for (String row : rows) {
            HBox rowBox = new HBox(5);
            rowBox.setAlignment(Pos.CENTER);
            for (int c = 0; c < row.length(); c++) {
                char letter = row.charAt(c);
                Button b = new Button(String.valueOf(letter).toUpperCase());
                b.getStyleClass().add("key");
                b.setOnAction(e -> {
                    if (onLetterPressed != null) {
                        onLetterPressed.accept(letter);
                    }
                });
                buttons.put(letter, b);
                rowBox.getChildren().add(b);
            }
            getChildren().add(rowBox);
        }
    }

    /** Register the callback the parent screen wants notified on each press. */
    public void setOnLetterPressed(Consumer<Character> handler) {
        this.onLetterPressed = handler;
    }

    /** Fades + disables the given key (e.g., after it has been guessed). */
    public void disableAndFadeButton(char c) {
        Button b = buttons.get(Character.toLowerCase(c));
        if (b != null) {
            b.setDisable(true);
        }
    }

    /** Re-enables every key (called at the start of a new round). */
    public void resetKeyboard() {
        for (Button b : buttons.values()) {
            b.setDisable(false);
        }
    }
}
