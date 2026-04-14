import java.awt.*;
import java.util.Random;

/**
 * The AnchorPoint is a mysterious object that drifts slowly within a
 * small radius. The player must keep their mouse cursor directly over
 * the moving anchor to maintain Focus. If Focus is lost during a blink,
 * the room layout shifts immediately.
 */
public class AnchorPoint {

    // --- Home position (centre of wander zone) -------------------------------
    private final double homeX, homeY;

    // --- Current wandering position ------------------------------------------
    private double curX, curY;
    private double targetX, targetY;

    private static final int    RADIUS        = 18;
    private static final double WANDER_RADIUS = 50;       // max drift from home
    private static final double WANDER_SPEED  = 0.55;     // px per frame

    // --- Focus detection (mouse cursor must be over the anchor) ---------------
    private static final int FOCUS_RADIUS = 24;            // hit-test radius (px)
    private boolean focused = false;

    // --- Visual state --------------------------------------------------------
    private double  pulsePhase = 0;
    private final Random rng = new Random();

    // --- Colours -------------------------------------------------------------
    private static final Color SIGIL_IDLE    = new Color(140, 40, 40);
    private static final Color SIGIL_FOCUSED = new Color(220, 40, 40);
    private static final Color GLOW_IDLE     = new Color(140, 40, 40, 30);
    private static final Color GLOW_FOCUSED  = new Color(255, 60, 40, 60);
    private static final Color RING_COLOR    = new Color(180, 50, 50, 100);
    private static final Color ZONE_COLOR    = new Color(140, 40, 40, 18);

    // =========================================================================

    public AnchorPoint(double x, double y) {
        this.homeX   = x;
        this.homeY   = y;
        this.curX    = x;
        this.curY    = y;
        pickNewTarget();
    }

    public double getX() { return curX; }
    public double getY() { return curY; }

    // --- Wander logic --------------------------------------------------------

    private void pickNewTarget() {
        double a = rng.nextDouble() * 2 * Math.PI;
        double r = rng.nextDouble() * WANDER_RADIUS;
        targetX = homeX + Math.cos(a) * r;
        targetY = homeY + Math.sin(a) * r;
    }

    // --- Update (called every frame) -----------------------------------------

    public void update() {
        // Move toward current target
        double dx = targetX - curX;
        double dy = targetY - curY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 2.5) {
            pickNewTarget();
        } else {
            // Smooth acceleration (ease-out near target)
            double speed = Math.min(WANDER_SPEED, dist * 0.04 + 0.15);
            curX += (dx / dist) * speed;
            curY += (dy / dist) * speed;
        }

        // Pulse animation
        pulsePhase += 0.05;
        if (pulsePhase > 2 * Math.PI) pulsePhase -= 2 * Math.PI;
    }

    // --- Focus detection -----------------------------------------------------

    /**
     * Check whether the mouse cursor is hovering over this anchor.
     *
     * @return true if the cursor is within FOCUS_RADIUS of the anchor centre
     */
    public boolean isFocusedBy(int mouseX, int mouseY) {
        double dx   = mouseX - curX;
        double dy   = mouseY - curY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        focused = dist <= FOCUS_RADIUS;
        return focused;
    }

    // --- Rendering -----------------------------------------------------------

    public void draw(Graphics2D g2) {
        double pulse = 0.5 + 0.5 * Math.sin(pulsePhase);
        int glowR = RADIUS + 10 + (int)(pulse * 12);

        Color baseGlow  = focused ? GLOW_FOCUSED  : GLOW_IDLE;
        Color baseSigil = focused ? SIGIL_FOCUSED : SIGIL_IDLE;

        // Focus-zone indicator (faint circle showing the hit area)
        g2.setColor(ZONE_COLOR);
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
                                     BasicStroke.JOIN_MITER, 10f,
                                     new float[]{4f, 4f}, (float)(pulsePhase * 3)));
        g2.drawOval((int)(curX - FOCUS_RADIUS), (int)(curY - FOCUS_RADIUS),
                    FOCUS_RADIUS * 2, FOCUS_RADIUS * 2);

        // Outer pulsing glow rings
        for (int i = 3; i >= 0; i--) {
            int r = glowR + i * 8;
            int alpha = baseGlow.getAlpha() / (i + 1);
            g2.setColor(new Color(baseGlow.getRed(), baseGlow.getGreen(),
                                  baseGlow.getBlue(), Math.max(alpha, 5)));
            g2.fillOval((int)(curX - r), (int)(curY - r), r * 2, r * 2);
        }

        // Rotating ring marks
        g2.setColor(RING_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        int ringR = RADIUS + 6;
        for (int i = 0; i < 8; i++) {
            double a = pulsePhase * 0.7 + i * Math.PI / 4;
            int x1 = (int)(curX + Math.cos(a) * ringR);
            int y1 = (int)(curY + Math.sin(a) * ringR);
            int x2 = (int)(curX + Math.cos(a) * (ringR + 5));
            int y2 = (int)(curY + Math.sin(a) * (ringR + 5));
            g2.drawLine(x1, y1, x2, y2);
        }

        // Core sigil (eye shape)
        drawEye(g2, baseSigil, pulse);

        // Label follows the anchor
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(focused
            ? new Color(255, 80, 60, 200)
            : new Color(140, 60, 50, 140));
        g2.drawString("ANCHOR", (int)(curX - 18), (int)(curY + RADIUS + 16));

        // "FOCUSED" / "LOST" micro-label
        if (focused) {
            g2.setColor(new Color(80, 255, 80, 160));
            g2.drawString("FOCUSED", (int)(curX - 22), (int)(curY - RADIUS - 10));
        }
    }

    /** Draws an eye-like sigil at the anchor's current position. */
    private void drawEye(Graphics2D g2, Color color, double pulse) {
        int ix = (int) curX;
        int iy = (int) curY;

        // Eye outline (two arcs forming an almond shape)
        int ew = RADIUS * 2;
        int eh = (int)(RADIUS * 0.9 * (focused ? 1.0 : 0.3 + pulse * 0.4));

        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));
        g2.drawArc(ix - RADIUS, iy - eh, ew, eh * 2, 0, 180);
        g2.drawArc(ix - RADIUS, iy - eh, ew, eh * 2, 180, 180);

        // Iris
        int irisR = Math.max(4, eh / 2);
        g2.fillOval(ix - irisR, iy - irisR, irisR * 2, irisR * 2);

        // Pupil
        int pupilR = Math.max(2, irisR / 2);
        g2.setColor(Color.BLACK);
        g2.fillOval(ix - pupilR, iy - pupilR, pupilR * 2, pupilR * 2);

        // Highlight
        g2.setColor(new Color(255, 255, 255, focused ? 200 : 70));
        g2.fillOval(ix - pupilR + 1, iy - pupilR, 3, 3);
    }
}
