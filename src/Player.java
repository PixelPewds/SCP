import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Updated Player entity:
 * - Fixes "shaking" glitch when mouse is too close (Deadzone).
 * - Implements WASD movement relative to the mouse cursor position.
 * - Cursor is intended to be replaced with a '+' elsewhere in the game engine.
 */
public class Player {

    // --- Position (centre of player) -----------------------------------------
    private double x;
    private double y;

    // --- Visual --------------------------------------------------------------
    private static final int SIZE = 30;
    private static final Color BODY_COLOR = new Color(0, 200, 160);
    private static final Color BORDER_COLOR = new Color(0, 255, 200);
    private static final Color GLOW_COLOR = new Color(0, 255, 200, 40);

    // --- Rotation ------------------------------------------------------------
    private double angle = 0;

    // --- Movement ------------------------------------------------------------
    private static final double SPEED = 3.5; // Slightly faster for responsiveness
    private static final double DEADZONE = 5.0; // Prevent glitching when mouse is on player
    private boolean movingUp, movingDown, movingLeft, movingRight;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    // --- Accessors -----------------------------------------------------------
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public void setMovingUp(boolean v) {
        movingUp = v;
    }

    public void setMovingDown(boolean v) {
        movingDown = v;
    }

    public void setMovingLeft(boolean v) {
        movingLeft = v;
    }

    public void setMovingRight(boolean v) {
        movingRight = v;
    }

    // --- Update --------------------------------------------------------------
    public void update(int mouseX, int mouseY) {
        double dx = mouseX - x;
        double dy = mouseY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 1. Update Angle: Always face the mouse cursor
        angle = Math.atan2(dy, dx);

        // 2. Deadzone Check: If mouse is too close, don't calculate movement
        if (distance < DEADZONE) {
            return;
        }

        // 3. Directional Vectors based on Current Face Angle
        // Forward (W) is toward the mouse
        double fwdX = Math.cos(angle);
        double fwdY = Math.sin(angle);

        // Right (D) is perpendicular (90 degrees clockwise)
        double rgtX = -fwdY;
        double rgtY = fwdX;

        // 4. Build facing-relative movement
        double moveForward = 0, moveStrafe = 0;

        if (movingUp)
            moveForward += 1; // W: Forward
        if (movingDown)
            moveForward -= 1; // S: Backward
        if (movingRight)
            moveStrafe += 1; // D: Right
        if (movingLeft)
            moveStrafe -= 1; // A: Left

        // 5. Normalise diagonal movement speed
        double len = Math.sqrt(moveForward * moveForward + moveStrafe * moveStrafe);
        if (len > 0) {
            moveForward = (moveForward / len) * SPEED;
            moveStrafe = (moveStrafe / len) * SPEED;
        }

        // 6. Apply Movement: X and Y move relative to where the "Head" is pointing
        x += (fwdX * moveForward) + (rgtX * moveStrafe);
        y += (fwdY * moveForward) + (rgtY * moveStrafe);
    }

    public void reset(double sx, double sy) {
        x = sx;
        y = sy;
        angle = 0;
        movingUp = movingDown = movingLeft = movingRight = false;
    }

    // --- Clamping and Rendering ----------------------------------------------
    public void clamp(int width, int height) {
        if (x < SIZE)
            x = SIZE;
        if (y < SIZE)
            y = SIZE;
        if (x > width - SIZE)
            x = width - SIZE;
        if (y > height - SIZE)
            y = height - SIZE;
    }

    public void clampToRect(int left, int top, int right, int bottom) {
        if (x < left + SIZE)
            x = left + SIZE;
        if (x > right - SIZE)
            x = right - SIZE;
        if (y < top + SIZE)
            y = top + SIZE;
        if (y > bottom - SIZE)
            y = bottom - SIZE;
    }

    public void draw(Graphics2D g2) {
        AffineTransform saved = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);

        // The "Head" of the triangle is at (SIZE, 0) - this points at the mouse
        int[] triX = { SIZE, -SIZE / 2, -SIZE / 2 };
        int[] triY = { 0, -SIZE / 2, SIZE / 2 };
        Polygon tri = new Polygon(triX, triY, 3);

        g2.setColor(GLOW_COLOR);
        g2.setStroke(new BasicStroke(8f));
        g2.drawPolygon(tri);

        g2.setColor(BODY_COLOR);
        g2.fillPolygon(tri);

        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolygon(tri);

        g2.setTransform(saved);
    }
}