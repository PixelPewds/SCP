import java.awt.*;
import java.util.Random;

/**
 * SCP-096 "The Shy Guy" — containment cell SCP-002.
 *
 * <ul>
 *   <li><b>WEEPING</b>  — entity huddles in its starting corner, trembling.
 *       Looking at it (hovering the cursor over it) triggers rage.</li>
 *   <li><b>SCREAMING</b> — 0.8 s build-up: body stretches, red flash overlay.
 *       No movement yet — pure tension.</li>
 *   <li><b>CHARGING</b>  — sprints toward the player at 230 px/s.
 *       Contact is immediately lethal.</li>
 * </ul>
 */
public class ShyGuyBehavior {

    // =========================================================================
    // State
    // =========================================================================

    public enum State { WEEPING, SCREAMING, CHARGING }
    private State state = State.WEEPING;

    // =========================================================================
    // Position / movement
    // =========================================================================

    private double entityX, entityY;

    /** Pixel radius used for cursor-hover detection (triggers rage). */
    private static final int    HOVER_RADIUS    = 28;
    /** Pixels-per-second charge speed once CHARGING. */
    private static final double CHARGE_SPEED    = 230;
    /** Distance (px) at which the charge is considered a lethal hit. */
    private static final double KILL_RADIUS     = 22;

    /** Starting corner — bottom-left quadrant, gives the player approach room. */
    private static final double HOME_X = 160;
    private static final double HOME_Y = 420;

    // =========================================================================
    // Timers / animation
    // =========================================================================

    private double screamTimer  = 0;
    private static final double SCREAM_DURATION = 0.8; // seconds before charge

    private double tremblePhase = 0;
    private double glitchOffset = 0;   // used to animate scanline glitch

    private final Random rng = new Random();

    // =========================================================================
    // Construction / reset
    // =========================================================================

    public ShyGuyBehavior() {
        reset();
    }

    public void reset() {
        entityX     = HOME_X;
        entityY     = HOME_Y;
        state       = State.WEEPING;
        screamTimer = 0;
        tremblePhase = 0;
        glitchOffset = 0;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public State   getState()   { return state; }
    public double  getEntityX() { return entityX; }
    public double  getEntityY() { return entityY; }

    /** Returns true if the mouse cursor is hovering over SCP-096's hit zone. */
    public boolean isCursorOver(int mouseX, int mouseY) {
        double dx = mouseX - entityX;
        double dy = mouseY - entityY;
        return Math.sqrt(dx * dx + dy * dy) <= HOVER_RADIUS;
    }

    /**
     * Returns true if SCP-096 has reached lethal contact distance with the
     * player.  Only meaningful during CHARGING state.
     *
     * @param playerRadius approximate visual radius of the player sprite (px)
     */
    public boolean hasKilledPlayer(double px, double py, double playerRadius) {
        if (state != State.CHARGING) return false;
        double dx = px - entityX;
        double dy = py - entityY;
        return Math.sqrt(dx * dx + dy * dy) <= KILL_RADIUS + playerRadius;
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Called once per frame from GamePanel while the active cell is SCP-002.
     *
     * @param dt        delta-time in seconds
     * @param mouseX    screen-space mouse X
     * @param mouseY    screen-space mouse Y
     * @param playerX   player world X
     * @param playerY   player world Y
     */
    public void update(double dt, int mouseX, int mouseY,
                       double playerX, double playerY) {

        tremblePhase += dt * 9.0;
        glitchOffset += dt;

        switch (state) {

            case WEEPING:
                // Trigger on cursor-over
                if (isCursorOver(mouseX, mouseY)) {
                    state       = State.SCREAMING;
                    screamTimer = 0;
                }
                break;

            case SCREAMING:
                screamTimer += dt;
                if (screamTimer >= SCREAM_DURATION) {
                    state = State.CHARGING;
                }
                break;

            case CHARGING:
                double dx   = playerX - entityX;
                double dy   = playerY - entityY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 1.0) {
                    double step = Math.min(CHARGE_SPEED * dt, dist);
                    entityX += (dx / dist) * step;
                    entityY += (dy / dist) * step;
                }
                break;
        }
    }

    // =========================================================================
    // Drawing
    // =========================================================================

    /** Draw SCP-096 in its current visual state. */
    public void draw(Graphics2D g2) {
        switch (state) {
            case WEEPING:   drawWeeping(g2);   break;
            case SCREAMING: drawScreaming(g2); break;
            case CHARGING:  drawCharging(g2);  break;
        }
    }

    /**
     * Draw a full-screen red-tint "rage" overlay on top of the room, plus
     * periodic scanline noise.  Call this <em>before</em> {@link #draw(Graphics2D)}
     * so the entity renders on top.  No-ops during WEEPING.
     */
    public void drawRageOverlay(Graphics2D g2, int w, int h) {
        if (state == State.WEEPING) return;

        double t = (state == State.SCREAMING)
                ? screamTimer / SCREAM_DURATION
                : 1.0;

        // Reddening tint — ramps from 0 → ~40 alpha
        g2.setColor(new Color(150, 10, 10, (int)(t * 40)));
        g2.fillRect(0, 0, w, h);

        // Scanline glitch strips during CHARGING
        if (state == State.CHARGING) {
            for (int i = 0; i < 4; i++) {
                if (rng.nextInt(8) == 0) {
                    int lineY = rng.nextInt(h);
                    int lineH = 1 + rng.nextInt(3);
                    int alpha  = 30 + rng.nextInt(40);
                    g2.setColor(new Color(255, 50, 30, alpha));
                    g2.fillRect(0, lineY, w, lineH);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // WEEPING draw
    // -------------------------------------------------------------------------

    private void drawWeeping(Graphics2D g2) {
        double tremble = Math.sin(tremblePhase) * 2.0;
        int cx = (int)(entityX + tremble);
        int cy = (int) entityY;

        // Ground shadow
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(cx - 20, cy + 18, 40, 12);

        // Legs (crouched, barely visible beneath torso)
        g2.setColor(new Color(175, 170, 162));
        g2.fillOval(cx - 13, cy + 10, 11, 14);
        g2.fillOval(cx + 2,  cy + 10, 11, 14);

        // Torso
        g2.setColor(new Color(200, 195, 185));
        g2.fillOval(cx - 11, cy - 16, 22, 30);

        // Head bowed into hands
        g2.setColor(new Color(215, 210, 200));
        g2.fillOval(cx - 8, cy - 30, 16, 18);

        // Arms wrapped protectively over head
        g2.setColor(new Color(188, 183, 172));
        g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(cx - 17, cy - 34, 12, 12, 200, 200);
        g2.drawArc(cx + 5,  cy - 34, 12, 12, 340, 200);
        g2.setStroke(new BasicStroke(1f));

        // Subtle sobbing tears streak
        g2.setColor(new Color(160, 200, 230, 60));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(cx - 4, cy - 20, cx - 5, cy - 10);
        g2.drawLine(cx + 3, cy - 20, cx + 4, cy - 10);
        g2.setStroke(new BasicStroke(1f));

        // "DO NOT LOOK" diegetic label
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(160, 50, 40, 130));
        String warn = "DO NOT LOOK";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(warn, cx - fm.stringWidth(warn) / 2, cy + 42);
    }

    // -------------------------------------------------------------------------
    // SCREAMING draw
    // -------------------------------------------------------------------------

    private void drawScreaming(Graphics2D g2) {
        double t  = screamTimer / SCREAM_DURATION;    // 0 → 1
        double flash = Math.abs(Math.sin(screamTimer * 28));

        // Red camera-flash overlay
        g2.setColor(new Color(220, 20, 10, (int)(flash * 80)));
        g2.fillRect(0, 0, 800, 600);

        int cx = (int) entityX;
        int cy = (int) entityY;

        // Ground shadow (growing)
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(cx - (int)(20 + t * 10), cy + 18, (int)(40 + t * 20), 12);

        // Body — stretching upright as t increases
        int torsoH  = (int)(28 + t * 22);
        int torsoTop = cy - torsoH / 2;

        g2.setColor(new Color(220, 210, 195));
        g2.fillOval(cx - 12, torsoTop, 24, torsoH);

        // Legs extending downward
        g2.setColor(new Color(175, 168, 158));
        g2.fillOval(cx - 14, cy + 8, 12, (int)(12 + t * 10));
        g2.fillOval(cx + 2,  cy + 8, 12, (int)(12 + t * 10));

        // Head rising
        int headY = torsoTop - (int)(18 + t * 10);
        g2.setColor(new Color(235, 225, 210));
        g2.fillOval(cx - 10, headY, 20, 20);

        // Mouth — opening wide
        int mW = (int)(3 + t * 11);
        int mH = (int)(2 + t * 9);
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - mW / 2, headY + 11, mW, mH);

        // Eyes — visible, wide, red-tinged
        g2.setColor(new Color(200, 50, 40, (int)(150 * t)));
        g2.fillOval(cx - 7, headY + 4, 5, 5);
        g2.fillOval(cx + 2, headY + 4, 5, 5);

        // Arms flinging outward
        int armSpread = (int)(14 + t * 24);
        g2.setColor(new Color(200, 190, 175));
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 12, cy - 8, cx - armSpread, cy - 4 + (int)(t * 14));
        g2.drawLine(cx + 12, cy - 8, cx + armSpread, cy - 4 + (int)(t * 14));
        g2.setStroke(new BasicStroke(1f));

        // "CONTAINMENT BREACH" warning
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(new Color(240, 25, 15, (int)(180 * t)));
        String warn = "CONTAINMENT BREACH";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(warn, cx - fm.stringWidth(warn) / 2, cy + 52);
    }

    // -------------------------------------------------------------------------
    // CHARGING draw
    // -------------------------------------------------------------------------

    private void drawCharging(Graphics2D g2) {
        double tremble = Math.sin(tremblePhase * 2.2) * 3.5;
        int cx = (int)(entityX + tremble);
        int cy = (int)(entityY + tremble * 0.4);

        // Motion blur ghost trail (3 fading copies offset in direction of travel)
        for (int i = 3; i >= 1; i--) {
            int tx = (int)(cx - tremble * i * 1.2);
            int alpha = 20 / i;
            g2.setColor(new Color(220, 30, 20, alpha));
            g2.fillOval(tx - 14, cy - 38, 28, 58);
        }

        // Red radial aura
        float[] fracs = {0f, 1f};
        Color[] cols  = {new Color(220, 20, 10, 75), new Color(220, 20, 10, 0)};
        g2.setPaint(new RadialGradientPaint(cx, cy, 50, fracs, cols));
        g2.fillOval(cx - 50, cy - 50, 100, 100);
        g2.setPaint(null);

        // Ground shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(cx - 22, cy + 20, 44, 12);

        // Legs (long stride)
        g2.setColor(new Color(170, 162, 152));
        g2.fillOval(cx - 14, cy + 10, 13, 22);
        g2.fillOval(cx + 1,  cy + 10, 13, 22);

        // Torso — upright, tall
        g2.setColor(new Color(228, 218, 203));
        g2.fillOval(cx - 13, cy - 38, 26, 52);

        // Head
        g2.setColor(new Color(240, 230, 215));
        g2.fillOval(cx - 11, cy - 58, 22, 22);

        // Mouth still open and screaming
        g2.setColor(Color.BLACK);
        g2.fillOval(cx - 5, cy - 47, 10, 12);

        // Eyes — fully red, visible
        g2.setColor(new Color(220, 30, 20, 200));
        g2.fillOval(cx - 8, cy - 54, 5, 5);
        g2.fillOval(cx + 3, cy - 54, 5, 5);

        // Arms reaching forward
        g2.setColor(new Color(208, 198, 183));
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(cx - 13, cy - 22, cx - 30, cy - 12);
        g2.drawLine(cx + 13, cy - 22, cx + 30, cy - 12);
        g2.setStroke(new BasicStroke(1f));
    }
}
