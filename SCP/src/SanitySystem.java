import java.awt.*;
import java.util.Random;

/**
 * Tracks the player's mental state. Sanity drains when the room shifts
 * and causes escalating screen-shake and visual distortion.
 */
public class SanitySystem {

    private double sanity = 100.0;

    private static final double DRAIN_PER_SHIFT = 12.0;
    private static final double MAX_SHAKE       = 10.0;

    private double shakeX, shakeY;
    private final Random rng = new Random();

    // --- Glitch effect at low sanity -----------------------------------------
    private boolean glitching   = false;
    private double  glitchTimer = 0;

    // --- Public API ----------------------------------------------------------

    public void drainFromShift() {
        sanity = Math.max(0, sanity - DRAIN_PER_SHIFT);
    }

    public void update(double dt) {
        // Passive paranoia drain below 30
        if (sanity < 30) sanity = Math.max(0, sanity - dt * 0.4);

        // Very slow passive recovery when above 60
        if (sanity > 0 && sanity < 60) sanity = Math.min(100, sanity + dt * 0.15);

        // Screen shake proportional to lost sanity
        double lostFraction = 1.0 - sanity / 100.0;
        double mag = MAX_SHAKE * lostFraction * lostFraction; // quadratic ramp
        shakeX = (rng.nextDouble() - 0.5) * 2 * mag;
        shakeY = (rng.nextDouble() - 0.5) * 2 * mag;

        // Random visual glitch below 50%
        if (sanity < 50) {
            glitchTimer += dt;
            double chance = dt * (1.0 - sanity / 50.0) * 0.4;
            if (!glitching && rng.nextDouble() < chance) {
                glitching  = true;
                glitchTimer = 0;
            }
            if (glitching && glitchTimer > 0.08) glitching = false;
        } else {
            glitching = false;
        }
    }

    // --- Accessors -----------------------------------------------------------

    public double  getSanity()       { return sanity; }
    public double  getSanityFactor() { return sanity / 100.0; }
    public double  getShakeX()       { return shakeX; }
    public double  getShakeY()       { return shakeY; }
    public boolean isGlitching()     { return glitching; }

    public void reset() {
        sanity = 100; shakeX = shakeY = 0; glitching = false; glitchTimer = 0;
    }

    // --- HUD rendering -------------------------------------------------------

    public void drawMeter(Graphics2D g2, int panelWidth) {
        int barW = 120, barH = 14;
        int bx = panelWidth - barW - 50, by = 50;

        // Label
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(new Color(180, 180, 180));
        g2.drawString("SANITY", bx, by - 5);

        // Background
        g2.setColor(new Color(20, 20, 20, 200));
        g2.fillRoundRect(bx, by, barW, barH, 6, 6);

        // Fill (colour shifts green -> yellow -> red)
        double fill = sanity / 100.0;
        Color barCol;
        if      (sanity > 65) barCol = new Color(50, 190, 90);
        else if (sanity > 35) barCol = new Color(210, 190, 40);
        else                  barCol = new Color(210, 45, 35);

        int fw = Math.max(0, (int)((barW - 2) * fill));
        g2.setColor(barCol);
        g2.fillRoundRect(bx + 1, by + 1, fw, barH - 2, 5, 5);

        // Glow on low sanity
        if (sanity < 35) {
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.008);
            g2.setColor(new Color(210, 45, 35, (int)(40 * pulse)));
            g2.fillRoundRect(bx - 2, by - 2, barW + 4, barH + 4, 8, 8);
        }

        // Border
        g2.setColor(new Color(100, 100, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(bx, by, barW, barH, 6, 6);

        // Percentage
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString((int) sanity + "%", bx + barW + 5, by + 12);
    }

    // --- Full-screen distortion overlay (drawn after everything) -------------

    public void drawDistortion(Graphics2D g2, int width, int height) {
        if (!glitching) return;
        // Brief colour-shift scanline burst
        g2.setColor(new Color(rng.nextInt(60), 0, rng.nextInt(60), 40));
        int bandH = 3 + rng.nextInt(8);
        int bandY = rng.nextInt(height);
        g2.fillRect(0, bandY, width, bandH);

        // Slight horizontal offset bar
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRect(rng.nextInt(20), rng.nextInt(height), width, 2);
    }
}
