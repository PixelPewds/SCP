import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Player entity that draws a directional sprite (triangle) and
 * continuously rotates to face the current mouse cursor position.
 *
 * The rotation angle is recalculated every frame via
 * {@link #update(int, int)}.
 */
public class Player {

    // --- Position (centre of player) -----------------------------------------
    private double x;
    private double y;

    // --- Visual --------------------------------------------------------------
    private static final int   SIZE         = 30;          // px, radius of bounding circle
    private static final Color BODY_COLOR   = new Color(0, 200, 160);   // teal
    private static final Color BORDER_COLOR = new Color(0, 255, 200);   // lighter teal
    private static final Color GLOW_COLOR   = new Color(0, 255, 200, 40); // subtle glow

    // --- Rotation ------------------------------------------------------------
    private double angle = 0;  // radians, 0 = right

    // --- Movement ------------------------------------------------------------
    private static final double SPEED = 3.0;               // px / frame
    private boolean movingUp, movingDown, movingLeft, movingRight;


    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }

    // --- Accessors -----------------------------------------------------------

    public double getX()     { return x; }
    public double getY()     { return y; }
    public double getAngle() { return angle; }

    public void setMovingUp(boolean v)    { movingUp    = v; }
    public void setMovingDown(boolean v)  { movingDown  = v; }
    public void setMovingLeft(boolean v)  { movingLeft  = v; }
    public void setMovingRight(boolean v) { movingRight = v; }

    public void reset(double sx, double sy) {
        x = sx; y = sy; angle = 0;
        movingUp = movingDown = movingLeft = movingRight = false;
    }

    // --- Update (called every frame) -----------------------------------------

    /**
     * Rotate toward the mouse and apply WASD movement.
     *
     * @param mouseX current cursor x in panel coordinates
     * @param mouseY current cursor y in panel coordinates
     */
    public void update(int mouseX, int mouseY) {
        // Angle from player centre to mouse
        double dx = mouseX - x;
        double dy = mouseY - y;
        angle = Math.atan2(dy, dx);

        // WASD movement
        double vx = 0, vy = 0;
        if (movingUp)    vy -= 1;
        if (movingDown)  vy += 1;
        if (movingLeft)  vx -= 1;
        if (movingRight) vx += 1;

        // Normalise diagonal movement
        double len = Math.sqrt(vx * vx + vy * vy);
        if (len > 0) {
            vx = (vx / len) * SPEED;
            vy = (vy / len) * SPEED;
        }

        x += vx;
        y += vy;
    }

    /**
     * Clamp the player position so it stays within the panel bounds.
     */
    public void clamp(int width, int height) {
        if (x < SIZE) x = SIZE;
        if (y < SIZE) y = SIZE;
        if (x > width  - SIZE) x = width  - SIZE;
        if (y > height - SIZE) y = height - SIZE;
    }

    /** Clamp position within a rectangular region (for corridors, etc). */
    public void clampToRect(int left, int top, int right, int bottom) {
        if (x < left + SIZE)   x = left + SIZE;
        if (x > right - SIZE)  x = right - SIZE;
        if (y < top + SIZE)    y = top + SIZE;
        if (y > bottom - SIZE) y = bottom - SIZE;
    }

    public void setX(double v) { x = v; }
    public void setY(double v) { y = v; }

    // --- Render --------------------------------------------------------------

    public void draw(Graphics2D g2) {
        AffineTransform saved = g2.getTransform();

        // --- Player body (rotated triangle) ---
        g2.translate(x, y);
        g2.rotate(angle);

        int[] triX = {  SIZE,  -SIZE / 2,  -SIZE / 2 };
        int[] triY = {  0,     -SIZE / 2,   SIZE / 2 };
        Polygon tri = new Polygon(triX, triY, 3);

        // Outer glow
        g2.setColor(GLOW_COLOR);
        g2.setStroke(new BasicStroke(8f));
        g2.drawPolygon(tri);

        // Filled body
        g2.setColor(BODY_COLOR);
        g2.fillPolygon(tri);

        // Border
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(2f));
        g2.drawPolygon(tri);

        g2.setTransform(saved);
    }


}
