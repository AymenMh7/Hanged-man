package hangman.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * Pirate-cove rendition of the hangman.
 *
 * Stage on the beach:
 *   - sandy ground with pebble specks
 *   - driftwood gibbet with a skull-and-crossbones plaque
 *   - frayed rope + noose
 *   - skeleton pirate that fills in across 4 stages:
 *       1. skull with bandana
 *       2. ribcage + spine
 *       3. arms hanging at his sides
 *       4. legs ending in bare bone feet
 *
 * The figure hangs centered below the beam, with arms and legs draped
 * naturally instead of crossed — much closer to a real swinging body.
 */
public class HangmanCanvas extends Canvas {

    private static final double W = 360;
    private static final double H = 400;

    // The skeleton "anchor" — top of skull sits here.
    private static final double SKULL_CX = 225;
    private static final double SKULL_CY = 172;

    // Wood
    private static final Color WOOD_DARK  = Color.web("#5D3A1A");
    private static final Color WOOD_MID   = Color.web("#8B5A2B");
    private static final Color WOOD_LIGHT = Color.web("#C39666");
    private static final Color WOOD_GRAIN = Color.web("#3A2818");
    // Rope
    private static final Color ROPE_DARK  = Color.web("#8B6A35");
    private static final Color ROPE_LIGHT = Color.web("#D4A857");
    // Bones
    private static final Color BONE       = Color.web("#F4EAD0");
    private static final Color BONE_SHADE = Color.web("#B8A672");
    private static final Color BONE_LINE  = Color.web("#5D3A1A");
    // Bandana
    private static final Color BANDANA    = Color.web("#A04030");
    private static final Color BANDANA_HI = Color.web("#C9503E");
    private static final Color BANDANA_DOT= Color.web("#F8ECC8");
    // Sand
    private static final Color SAND       = Color.web("#F7DA9C");
    private static final Color SAND_DARK  = Color.web("#C9A66B");

    public HangmanCanvas() {
        super(W, H);
        drawGallowsOnly();
    }

    /** Repaints the figure based on the current round state. */
    public void drawHangman(int maxChances, int remainingChances) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        drawBackdrop(g);
        drawSand(g);
        drawGibbet(g);
        drawRope(g);

        int mistakes = Math.max(0, maxChances - remainingChances);
        int stages = 4;
        int stagesToDraw = (int) Math.round(((double) mistakes / maxChances) * stages);
        if (stagesToDraw < 0) stagesToDraw = 0;
        if (stagesToDraw > stages) stagesToDraw = stages;

        if (stagesToDraw >= 1) drawSkull(g);
        if (stagesToDraw >= 2) drawTorso(g);
        if (stagesToDraw >= 3) drawArms(g);
        if (stagesToDraw >= 4) drawLegs(g);
    }

    /** Wipes the canvas and redraws only the empty gibbet. */
    public void drawGallowsOnly() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, W, H);
        drawBackdrop(g);
        drawSand(g);
        drawGibbet(g);
        drawRope(g);
    }

    // ---------- helpers ----------

    private void drawBackdrop(GraphicsContext g) {
        // Soft parchment wash so the canvas reads as part of the card.
        LinearGradient bg = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#F8ECC8", 0.85)),
                new Stop(1.0, Color.web("#EAD49B", 0.85)));
        g.setFill(bg);
        g.fillRoundRect(0, 0, W, H, 18, 18);

        g.setStroke(Color.web("#8B5A2B", 0.45));
        g.setLineWidth(1.5);
        g.strokeRoundRect(0.75, 0.75, W - 1.5, H - 1.5, 18, 18);
    }

    private void drawSand(GraphicsContext g) {
        LinearGradient sand = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, SAND),
                new Stop(1.0, SAND_DARK));
        g.setFill(sand);
        g.beginPath();
        g.moveTo(0, 345);
        g.bezierCurveTo(120, 325, 240, 360, W, 340);
        g.lineTo(W, H);
        g.lineTo(0, H);
        g.closePath();
        g.fill();

        // Pebble specks for texture
        g.setFill(Color.web("#7E5A2E", 0.5));
        for (int i = 0; i < 24; i++) {
            double x = 20 + (i * 17) % (W - 40);
            double y = 352 + ((i * 31) % 36);
            g.fillOval(x, y, 2, 2);
        }
        // A couple of seashells
        g.setFill(Color.web("#FFFFFF", 0.75));
        g.fillOval(56, 374, 10, 5);
        g.fillOval(296, 366, 11, 5);
    }

    private void drawGibbet(GraphicsContext g) {
        // Vertical post
        drawPlank(g, 70, 60, 18, 290);

        // Diagonal brace
        g.save();
        g.translate(86, 110);
        g.rotate(45);
        drawPlank(g, 0, 0, 14, 64);
        g.restore();

        // Horizontal beam
        drawPlank(g, 70, 60, 240, 22);

        // Nail heads at corners
        g.setFill(Color.web("#1a1a1a"));
        g.fillOval(74, 64, 6, 6);
        g.fillOval(302, 64, 6, 6);
        g.fillOval(74, 76, 6, 6);
        g.fillOval(302, 76, 6, 6);
        g.fillOval(74, 340, 6, 6);

        // Skull-and-crossbones plaque hanging on the post
        g.setFill(WOOD_DARK);
        g.fillRect(96, 110, 50, 38);
        g.setFill(WOOD_MID);
        g.fillRect(99, 113, 44, 32);
        g.setStroke(WOOD_LIGHT);
        g.setLineWidth(1);
        g.strokeRect(96, 110, 50, 38);
        // skull glyph
        g.setFill(BONE);
        g.fillOval(112, 117, 18, 14);
        g.setFill(WOOD_DARK);
        g.fillOval(115, 122, 3, 3);
        g.fillOval(124, 122, 3, 3);
        // crossbones
        g.setStroke(BONE);
        g.setLineWidth(2);
        g.strokeLine(104, 137, 138, 143);
        g.strokeLine(104, 143, 138, 137);
    }

    private void drawPlank(GraphicsContext g, double x, double y, double w, double h) {
        LinearGradient grad = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, WOOD_LIGHT),
                new Stop(0.5, WOOD_MID),
                new Stop(1.0, WOOD_DARK));
        g.setFill(grad);
        g.fillRect(x, y, w, h);

        g.setStroke(WOOD_DARK);
        g.setLineWidth(1.5);
        g.strokeRect(x, y, w, h);

        // Wood grain streaks
        g.setStroke(WOOD_GRAIN);
        g.setLineWidth(0.8);
        g.setGlobalAlpha(0.35);
        if (h > w) {
            for (int i = 1; i < 4; i++) {
                double yy = y + (h * i / 4);
                g.strokeLine(x + 2, yy, x + w - 2, yy + 4);
            }
        } else {
            for (int i = 1; i < 4; i++) {
                double xx = x + (w * i / 4);
                g.strokeLine(xx, y + 2, xx + 4, y + h - 2);
            }
        }
        g.setGlobalAlpha(1.0);
    }

    private void drawRope(GraphicsContext g) {
        // Two-strand rope coming down from the beam
        g.setStroke(ROPE_DARK);
        g.setLineWidth(5);
        g.strokeLine(SKULL_CX, 82, SKULL_CX, 144);
        g.setStroke(ROPE_LIGHT);
        g.setLineWidth(2);
        g.strokeLine(SKULL_CX - 1, 82, SKULL_CX - 1, 144);

        // Noose loop sitting on top of the skull
        g.setStroke(ROPE_DARK);
        g.setLineWidth(4);
        g.strokeOval(SKULL_CX - 18, 138, 36, 22);
        g.setStroke(ROPE_LIGHT);
        g.setLineWidth(1.5);
        g.strokeOval(SKULL_CX - 17, 139, 34, 20);

        // Knot at the top of the noose
        g.setFill(ROPE_DARK);
        g.fillRoundRect(SKULL_CX - 7, 130, 14, 14, 4, 4);
        g.setFill(ROPE_LIGHT);
        g.fillRoundRect(SKULL_CX - 5, 132, 4, 10, 2, 2);
    }

    // ---- skeleton pirate parts ----

    private void drawSkull(GraphicsContext g) {
        double cx = SKULL_CX;
        double cy = SKULL_CY;

        // Cranium (rounded oval)
        g.setFill(BONE);
        g.fillOval(cx - 26, cy - 26, 52, 48);
        // Shading along the right + bottom
        g.setFill(BONE_SHADE);
        g.fillArc(cx - 26, cy - 26, 52, 48, 210, 130,
                  javafx.scene.shape.ArcType.ROUND);
        g.setFill(BONE);
        // Re-fill the upper portion so shading only kisses the bottom curve
        g.fillArc(cx - 26, cy - 26, 52, 48, 30, 130,
                  javafx.scene.shape.ArcType.ROUND);

        // Outline
        g.setStroke(BONE_LINE);
        g.setLineWidth(1.8);
        g.strokeOval(cx - 26, cy - 26, 52, 48);

        // Cheekbone notches (small inward arcs)
        g.setStroke(BONE_LINE);
        g.setLineWidth(1.2);
        g.strokeArc(cx - 26, cy + 4, 12, 10, 90, 90,
                    javafx.scene.shape.ArcType.OPEN);
        g.strokeArc(cx + 14, cy + 4, 12, 10, 0, 90,
                    javafx.scene.shape.ArcType.OPEN);

        // Eye sockets — two clean dark voids
        g.setFill(BONE_LINE);
        g.fillOval(cx - 16, cy - 8, 12, 12);   // left eye
        g.fillOval(cx + 4,  cy - 8, 12, 12);   // right eye

        // Triangular nose hole
        g.setFill(BONE_LINE);
        g.beginPath();
        g.moveTo(cx, cy + 4);
        g.lineTo(cx - 4, cy + 12);
        g.lineTo(cx + 4, cy + 12);
        g.closePath();
        g.fill();

        // Lower jaw — small rounded rectangle below the cranium
        g.setFill(BONE);
        g.fillRoundRect(cx - 14, cy + 14, 28, 14, 8, 8);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1.4);
        g.strokeRoundRect(cx - 14, cy + 14, 28, 14, 8, 8);

        // Teeth (vertical separators)
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        for (int i = 1; i < 5; i++) {
            double tx = cx - 14 + i * (28.0 / 5);
            g.strokeLine(tx, cy + 14, tx, cy + 24);
        }

        // Bandana — a smooth arc cap across the top of the skull
        g.setFill(BANDANA);
        g.beginPath();
        g.moveTo(cx - 28, cy - 12);
        g.bezierCurveTo(cx - 22, cy - 32, cx + 22, cy - 32, cx + 28, cy - 12);
        g.bezierCurveTo(cx + 22, cy - 16, cx - 22, cy - 16, cx - 28, cy - 12);
        g.closePath();
        g.fill();

        // Bandana highlight stripe
        g.setStroke(BANDANA_HI);
        g.setLineWidth(2);
        g.strokeArc(cx - 22, cy - 30, 44, 22, 20, 140,
                    javafx.scene.shape.ArcType.OPEN);

        // Polka dots
        g.setFill(BANDANA_DOT);
        g.fillOval(cx - 14, cy - 22, 3, 3);
        g.fillOval(cx - 4,  cy - 26, 3, 3);
        g.fillOval(cx + 6,  cy - 26, 3, 3);
        g.fillOval(cx + 14, cy - 22, 3, 3);

        // Bandana knot on the right side
        g.setFill(BANDANA);
        g.fillOval(cx + 24, cy - 16, 12, 10);
        g.setFill(BANDANA_HI);
        g.fillOval(cx + 28, cy - 14, 4, 4);

        // Bandana tail flowing right
        g.setFill(BANDANA);
        g.beginPath();
        g.moveTo(cx + 32, cy - 14);
        g.bezierCurveTo(cx + 50, cy - 18, cx + 50, cy - 4, cx + 36, cy - 6);
        g.closePath();
        g.fill();
        g.setFill(BANDANA_HI);
        g.fillOval(cx + 42, cy - 12, 2, 2);

        // Gold earring near the jaw
        g.setStroke(Color.web("#DAA520"));
        g.setLineWidth(1.6);
        g.strokeOval(cx - 22, cy + 16, 6, 8);
    }

    private void drawTorso(GraphicsContext g) {
        double cx = SKULL_CX;
        double topY = SKULL_CY + 28; // just under the jaw

        // Neck vertebrae (two small bone discs)
        g.setFill(BONE);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        g.fillOval(cx - 5, topY,      10, 6);
        g.strokeOval(cx - 5, topY,    10, 6);
        g.fillOval(cx - 5, topY + 6,  10, 6);
        g.strokeOval(cx - 5, topY + 6,10, 6);

        double spineTop = topY + 14;
        double spineBot = spineTop + 70;

        // Sternum (a thicker bone stripe in the middle)
        g.setFill(BONE);
        g.fillRoundRect(cx - 3, spineTop, 6, 50, 3, 3);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        g.strokeRoundRect(cx - 3, spineTop, 6, 50, 3, 3);

        // Ribs — three pairs of curved arcs, growing slightly downward
        g.setStroke(BONE);
        g.setLineWidth(4);
        for (int i = 0; i < 3; i++) {
            double y = spineTop + 6 + i * 14;
            // left rib
            g.strokeArc(cx - 26, y - 4, 26, 16, 0, -180,
                        javafx.scene.shape.ArcType.OPEN);
            // right rib
            g.strokeArc(cx,      y - 4, 26, 16, 0, -180,
                        javafx.scene.shape.ArcType.OPEN);
        }
        // Outline ribs in dark thin line for definition
        g.setStroke(BONE_LINE);
        g.setLineWidth(0.8);
        g.setGlobalAlpha(0.6);
        for (int i = 0; i < 3; i++) {
            double y = spineTop + 6 + i * 14;
            g.strokeArc(cx - 26, y - 4, 26, 16, 0, -180,
                        javafx.scene.shape.ArcType.OPEN);
            g.strokeArc(cx,      y - 4, 26, 16, 0, -180,
                        javafx.scene.shape.ArcType.OPEN);
        }
        g.setGlobalAlpha(1.0);

        // Pelvis pad (small horizontal bone)
        g.setFill(BONE);
        g.fillRoundRect(cx - 14, spineBot - 4, 28, 10, 6, 6);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        g.strokeRoundRect(cx - 14, spineBot - 4, 28, 10, 6, 6);
    }

    private void drawArms(GraphicsContext g) {
        double cx = SKULL_CX;
        double shoulderY = SKULL_CY + 46;

        // Shoulder joints
        drawBoneJoint(g, cx - 22, shoulderY, 9);
        drawBoneJoint(g, cx + 22, shoulderY, 9);

        // Left upper arm
        drawBoneSegment(g, cx - 22, shoulderY + 4, cx - 30, shoulderY + 38);
        // Right upper arm
        drawBoneSegment(g, cx + 22, shoulderY + 4, cx + 30, shoulderY + 38);

        // Elbow joints
        drawBoneJoint(g, cx - 30, shoulderY + 38, 8);
        drawBoneJoint(g, cx + 30, shoulderY + 38, 8);

        // Forearms — hanging slightly outward then in
        drawBoneSegment(g, cx - 30, shoulderY + 42, cx - 28, shoulderY + 72);
        drawBoneSegment(g, cx + 30, shoulderY + 42, cx + 28, shoulderY + 72);

        // Skeletal hands (three finger ticks each)
        g.setFill(BONE);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        g.fillOval(cx - 33, shoulderY + 70, 10, 8);
        g.strokeOval(cx - 33, shoulderY + 70, 10, 8);
        g.fillOval(cx + 23, shoulderY + 70, 10, 8);
        g.strokeOval(cx + 23, shoulderY + 70, 10, 8);

        // Finger bones
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        for (int i = 0; i < 3; i++) {
            g.strokeLine(cx - 32 + i * 3, shoulderY + 78, cx - 31 + i * 3, shoulderY + 84);
            g.strokeLine(cx + 24 + i * 3, shoulderY + 78, cx + 25 + i * 3, shoulderY + 84);
        }
    }

    private void drawLegs(GraphicsContext g) {
        double cx = SKULL_CX;
        double hipY = SKULL_CY + 112;

        // Hip joints
        drawBoneJoint(g, cx - 8, hipY, 8);
        drawBoneJoint(g, cx + 8, hipY, 8);

        // Upper legs (femurs) — angle slightly outward
        drawBoneSegment(g, cx - 8, hipY + 4, cx - 14, hipY + 38);
        drawBoneSegment(g, cx + 8, hipY + 4, cx + 14, hipY + 38);

        // Knee joints
        drawBoneJoint(g, cx - 14, hipY + 38, 8);
        drawBoneJoint(g, cx + 14, hipY + 38, 8);

        // Lower legs — straight down
        drawBoneSegment(g, cx - 14, hipY + 42, cx - 14, hipY + 72);
        drawBoneSegment(g, cx + 14, hipY + 42, cx + 14, hipY + 72);

        // Bony feet — small horizontal ovals at the bottom
        g.setFill(BONE);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1);
        g.fillOval(cx - 22, hipY + 70, 18, 8);
        g.strokeOval(cx - 22, hipY + 70, 18, 8);
        g.fillOval(cx + 6,  hipY + 70, 18, 8);
        g.strokeOval(cx + 6,  hipY + 70, 18, 8);

        // Toe ticks
        g.setStroke(BONE_LINE);
        g.setLineWidth(0.8);
        for (int i = 0; i < 3; i++) {
            g.strokeLine(cx - 20 + i * 6, hipY + 76, cx - 20 + i * 6, hipY + 80);
            g.strokeLine(cx + 8  + i * 6, hipY + 76, cx + 8  + i * 6, hipY + 80);
        }
    }

    /** Draws a bone segment with a slight shadow on one side. */
    private void drawBoneSegment(GraphicsContext g, double x1, double y1,
                                  double x2, double y2) {
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        // dark outline
        g.setStroke(BONE_LINE);
        g.setLineWidth(7);
        g.strokeLine(x1, y1, x2, y2);
        // bone fill
        g.setStroke(BONE);
        g.setLineWidth(5);
        g.strokeLine(x1, y1, x2, y2);
        // highlight
        g.setStroke(Color.web("#FFFFFF", 0.55));
        g.setLineWidth(1.2);
        g.strokeLine(x1 - 0.5, y1, x2 - 0.5, y2);
    }

    /** Round bone joint with outline. */
    private void drawBoneJoint(GraphicsContext g, double cx, double cy, double r) {
        g.setFill(BONE);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(BONE_LINE);
        g.setLineWidth(1.4);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }
}
