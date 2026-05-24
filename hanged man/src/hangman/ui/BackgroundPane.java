package hangman.ui;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

/**
 * Scenic backdrop for the Pirate Cove theme.
 *
 * Draws (back to front):
 *   sky gradient → sun → distant island layers → ocean → sandy beach
 *   → palm trees → drifting clouds.
 *
 * Any UI {@link Node} passed to the constructor is layered on top.
 * The canvas re-binds to the StackPane size, so the scene scales to
 * whatever window dimensions the screen requests.
 */
public class BackgroundPane extends StackPane {

    // Stable RNG so cloud + island positions stay put between repaints.
    private static final long SEED = 7L;

    private final Canvas backdrop;

    public BackgroundPane(Node content) {
        backdrop = new Canvas();
        backdrop.widthProperty().bind(widthProperty());
        backdrop.heightProperty().bind(heightProperty());
        backdrop.widthProperty().addListener((o, a, b)  -> redraw());
        backdrop.heightProperty().addListener((o, a, b) -> redraw());

        backdrop.setMouseTransparent(true);
        getChildren().addAll(backdrop, content);
    }

    private void redraw() {
        double w = backdrop.getWidth();
        double h = backdrop.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext g = backdrop.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        drawSky(g, w, h);
        drawSun(g, w, h);
        drawClouds(g, w, h);
        drawIslands(g, w, h);
        drawSea(g, w, h);
        drawSand(g, w, h);
        drawPalm(g, w, h, /*left*/ true);
        drawPalm(g, w, h, /*left*/ false);
        drawFooterPlanks(g, w, h);
    }

    // ---- layers --------------------------------------------------------

    private void drawSky(GraphicsContext g, double w, double h) {
        LinearGradient sky = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#9ED6F0")),  // upper sky
                new Stop(0.45, Color.web("#FFE7B2")),  // horizon haze
                new Stop(0.65, Color.web("#FFD592")));  // glow above water
        g.setFill(sky);
        g.fillRect(0, 0, w, h);
    }

    private void drawSun(GraphicsContext g, double w, double h) {
        double cx = w * 0.78;
        double cy = h * 0.30;
        double r  = Math.min(w, h) * 0.16;

        // Halo
        RadialGradient halo = new RadialGradient(
                0, 0, cx, cy, r * 2.2, false, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#FFCB66", 0.55)),
                new Stop(0.40, Color.web("#FFCB66", 0.20)),
                new Stop(1.00, Color.web("#FFCB66", 0.00)));
        g.setFill(halo);
        g.fillOval(cx - r * 2.2, cy - r * 2.2, r * 4.4, r * 4.4);

        // Core
        RadialGradient core = new RadialGradient(
                0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFF6D9")),
                new Stop(1.0, Color.web("#FF974D")));
        g.setFill(core);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    private void drawClouds(GraphicsContext g, double w, double h) {
        Random rng = new Random(SEED);
        g.setFill(Color.web("#FFFFFF", 0.85));
        for (int i = 0; i < 4; i++) {
            double cx = (rng.nextDouble() * 0.9 + 0.05) * w;
            double cy = (rng.nextDouble() * 0.25 + 0.05) * h;
            double base = Math.min(w, h) * (0.04 + rng.nextDouble() * 0.04);
            // 4 overlapping ovals make a fluffy cloud
            g.fillOval(cx - base * 1.6, cy - base * 0.6, base * 2.2, base * 1.1);
            g.fillOval(cx - base * 0.8, cy - base,       base * 1.8, base * 1.4);
            g.fillOval(cx + base * 0.2, cy - base * 0.7, base * 1.8, base * 1.2);
            g.fillOval(cx + base * 1.0, cy - base * 0.4, base * 1.6, base * 1.0);
        }
    }

    private void drawIslands(GraphicsContext g, double w, double h) {
        double seaTop = h * 0.55;
        // Far island layer (pale, biggest)
        drawIsland(g, w * 0.18, seaTop, w * 0.30, h * 0.07, Color.web("#9FBDC2"));
        drawIsland(g, w * 0.55, seaTop, w * 0.36, h * 0.08, Color.web("#85AAB0"));
        // Middle layer
        drawIsland(g, w * 0.05, seaTop, w * 0.22, h * 0.06, Color.web("#6E96A0"));
        drawIsland(g, w * 0.78, seaTop, w * 0.22, h * 0.06, Color.web("#6E96A0"));
    }

    private void drawIsland(GraphicsContext g, double cx, double baseY,
                            double radius, double height, Color color) {
        g.setFill(color);
        // Half-ellipse silhouette using fillOval with vertical clip
        double left = cx - radius;
        g.fillOval(left, baseY - height, radius * 2, height * 2);
        // Hard-cut the bottom by drawing the sea line right over it later.
    }

    private void drawSea(GraphicsContext g, double w, double h) {
        double seaTop = h * 0.55;
        double seaBot = h * 0.80;
        LinearGradient sea = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#3FB0D4")),
                new Stop(0.55, Color.web("#1F7F9F")),
                new Stop(1.00, Color.web("#155A75")));
        g.setFill(sea);
        g.fillRect(0, seaTop, w, seaBot - seaTop);

        // Wave highlights — subtle horizontal lines, slightly transparent.
        g.setStroke(Color.web("#C7ECF5", 0.4));
        g.setLineWidth(1);
        Random rng = new Random(SEED + 1);
        for (int i = 0; i < 14; i++) {
            double y  = seaTop + 6 + rng.nextDouble() * (seaBot - seaTop - 12);
            double x1 = rng.nextDouble() * w;
            double x2 = x1 + 18 + rng.nextDouble() * 60;
            g.strokeLine(x1, y, x2, y);
        }
    }

    private void drawSand(GraphicsContext g, double w, double h) {
        double sandTop = h * 0.78;
        LinearGradient sand = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.web("#F7DA9C")),
                new Stop(0.70, Color.web("#DDB573")),
                new Stop(1.00, Color.web("#B5894A")));
        g.setFill(sand);
        // Slight curve at the waterline using a path
        g.beginPath();
        g.moveTo(0, sandTop + 6);
        g.bezierCurveTo(w * 0.25, sandTop - 4, w * 0.75, sandTop + 10, w, sandTop + 2);
        g.lineTo(w, h);
        g.lineTo(0, h);
        g.closePath();
        g.fill();

        // Tiny pebble specks for texture
        Random rng = new Random(SEED + 2);
        g.setFill(Color.web("#7E5A2E", 0.45));
        for (int i = 0; i < 40; i++) {
            double x = rng.nextDouble() * w;
            double y = sandTop + 10 + rng.nextDouble() * (h - sandTop - 14);
            double s = 1 + rng.nextDouble() * 2;
            g.fillOval(x, y, s, s);
        }
    }

    private void drawPalm(GraphicsContext g, double w, double h, boolean left) {
        double baseX = left ? w * 0.08 : w * 0.92;
        double baseY = h * 0.82;
        double topY  = h * 0.30;
        double dir   = left ? 1 : -1;     // trunk leans toward center

        // Trunk — curving stack of brown segments
        g.setStroke(Color.web("#5D3A1A"));
        g.setLineWidth(11);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.beginPath();
        g.moveTo(baseX, baseY);
        double midX = baseX + dir * (w * 0.04);
        double midY = (baseY + topY) / 2;
        g.bezierCurveTo(baseX + dir * w * 0.01, baseY - h * 0.10,
                        midX,                   midY,
                        baseX + dir * w * 0.08, topY);
        g.stroke();

        // Highlight stripe on the trunk
        g.setStroke(Color.web("#8B5A2B"));
        g.setLineWidth(4);
        g.beginPath();
        g.moveTo(baseX, baseY);
        g.bezierCurveTo(baseX + dir * w * 0.01, baseY - h * 0.10,
                        midX,                   midY,
                        baseX + dir * w * 0.08, topY);
        g.stroke();

        // Crown of leaves — 6 fronds radiating
        double cx = baseX + dir * w * 0.08;
        double cy = topY;
        g.setFill(Color.web("#3F5F2A"));
        for (int i = 0; i < 7; i++) {
            double angle = -Math.PI / 2 + (i - 3) * (Math.PI / 7);
            drawFrond(g, cx, cy, angle, Math.min(w, h) * 0.13);
        }
        // Coconuts
        g.setFill(Color.web("#3A2818"));
        g.fillOval(cx - 6, cy + 4, 9, 9);
        g.fillOval(cx + 4, cy + 6, 9, 9);
    }

    private void drawFrond(GraphicsContext g, double cx, double cy,
                           double angle, double length) {
        double tipX = cx + Math.cos(angle) * length;
        double tipY = cy + Math.sin(angle) * length;
        double midX = cx + Math.cos(angle) * length * 0.55;
        double midY = cy + Math.sin(angle) * length * 0.55;
        double normX = -Math.sin(angle);
        double normY =  Math.cos(angle);
        double width = length * 0.18;

        g.beginPath();
        g.moveTo(cx, cy);
        g.bezierCurveTo(midX + normX * width, midY + normY * width,
                        tipX + normX * width * 0.3, tipY + normY * width * 0.3,
                        tipX, tipY);
        g.bezierCurveTo(tipX - normX * width * 0.3, tipY - normY * width * 0.3,
                        midX - normX * width, midY - normY * width,
                        cx, cy);
        g.closePath();
        g.fill();
    }

    /** Faint dark wood plank strip along the very bottom. */
    private void drawFooterPlanks(GraphicsContext g, double w, double h) {
        double y = h - 6;
        g.setFill(Color.web("#5D3A1A", 0.55));
        g.fillRect(0, y, w, 6);
    }
}
