package hangman.ui;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.net.URL;

public class BackgroundPane extends StackPane {

    private ImageView bgImageView; // Extracted as a class field

    public BackgroundPane(Node content, String imagePath) {
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: #1A0F05;");

        // Initialize the ImageView
        bgImageView = new ImageView();
        bgImageView.setPreserveRatio(false);
        bgImageView.fitWidthProperty().bind(widthProperty());
        bgImageView.fitHeightProperty().bind(heightProperty());
        bgImageView.setMouseTransparent(true);

        // Load the initial image
        setBackgroundImage(imagePath);

        getChildren().addAll(bgImageView, content);
    }

    /**
     * Swaps the current background image dynamically.
     */
    public void setBackgroundImage(String imagePath) {
        Image newImg = loadImage(imagePath);
        if (newImg != null && bgImageView != null) {
            bgImageView.setImage(newImg);
        }
    }

    public static Image loadImage(String resourcePath) {
        URL url = BackgroundPane.class.getResource(resourcePath);

        if (url == null) {
            String rel = resourcePath.startsWith("/")
                    ? resourcePath.substring(1) : resourcePath;
            File f = new File("src/" + rel);
            if (!f.exists()) f = new File(rel);
            if (f.exists()) {
                try { url = f.toURI().toURL(); }
                catch (java.net.MalformedURLException ignored) { }
            }
        }

        if (url == null) {
            System.err.println("[BackgroundPane] Image not found: " + resourcePath);
            return null;
        }
        return new Image(url.toExternalForm());
    }
}