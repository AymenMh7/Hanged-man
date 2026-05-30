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
 * Clavier QWERTY rendu sous forme de touches en bois patiné, fidèles
 * à l'esthétique de l'image de référence.
 *
 * Disposition :
 *   Rangée 1 : Q W E R T Y U I O P
 *   Rangée 2 : A S D F G H J K L
 *   Rangée 3 : Z X C V B N M
 *   Rangée 4 : [DELETE]  SPACE  ENTER ✕   (placeholders visuels / non fonctionnels)
 *
 * Seules les touches de lettres déclenchent {@code onLetterPressed} ;
 * la rangée du bas est désactivée par défaut et n'existe que pour
 * coller à l'esthétique de l'image de référence.
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
                b.getStyleClass().setAll("key"); // <--- Efface l'ardoise et n'applique QUE "key"
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

    // ---- méthodes utilitaires de fabrication ------------------------

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
        b.setDisable(true);   // décoration visuelle uniquement
        return b;
    }

    // ---- API publique -----------------------------------------------

    /** Enregistre le callback invoqué quand le joueur appuie sur une lettre. */
    public void setOnLetterPressed(Consumer<Character> handler) {
        this.onLetterPressed = handler;
    }

    /**
     * Désactive (estompe) une touche après son utilisation pour qu'elle
     * ne puisse plus être pressée pendant cette manche.
     */
    public void disableAndFadeButton(char c) {
        Button b = buttons.get(Character.toLowerCase(c));
        if (b != null) b.setDisable(true);
    }

    /** Réactive toutes les touches de lettres (à appeler au début d'une nouvelle manche). */
    public void resetKeyboard() {
        for (Button b : buttons.values()) b.setDisable(false);
    }
}
