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
 * QWERTY keyboard rendered as weathered wooden-tile keys, matching the
 * reference image aesthetic.
 *
 * Layout:
 *   Row 1 : Q W E R T Y U I O P
 *   Row 2 : A S D F G H J K L
 *   Row 3 : Z X C V B N M
 *   Row 4 : [DELETE]  SPACE  ENTER ✕   (visual / non-functional placeholders)
 *
 * Only letter keys fire {@code onLetterPressed}; the bottom row is
 * disabled by default and exists purely for the reference-image look.
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
                b.getStyleClass().setAll("key"); // <--- This wipes the slate clean and ONLY applies "key"
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

    // ---- factory helpers --------------------------------------------

    private Button makeLetterKey(String text, char letter) {
        Button b = new Button(text);
        b.getStyleClass().add("key");
        b.setOnAction(e -> {
            if (onLetterPressed != null) onLetterPressed.accept(letter);
        });
        return b;
    }

    private Button makeSpecialKey(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("key", "key-special");
        b.setDisable(true);   // visual decoration only
        return b;
    }

    // ---- public API -------------------------------------------------

    /** Register the callback invoked when the player presses a letter. */
    public void setOnLetterPressed(Consumer<Character> handler) {
        this.onLetterPressed = handler;
    }

    /**
     * Disables (fades) a key after it has been played so it cannot be
     * pressed again this round.
     */
    public void disableAndFadeButton(char c) {
        Button b = buttons.get(Character.toLowerCase(c));
        if (b != null) b.setDisable(true);
    }

    /** Re-enables every letter key (call at the start of a new round). */
    public void resetKeyboard() {
        for (Button b : buttons.values()) b.setDisable(false);
    }
}