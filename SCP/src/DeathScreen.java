import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Full-screen death overlay with TV-static noise, "SUBJECT TERMINATED"
 * text with chromatic aberration, and clickable Retry / Main Menu buttons.
 */
public class DeathScreen {

    public enum Action { NONE, RETRY, MAIN_MENU }

    // --- Static noise --------------------------------------------------------
    private static final int STATIC_W = 160, STATIC_H = 120;
    private final BufferedImage staticBuf =
        new BufferedImage(STATIC_W, STATIC_H, BufferedImage.TYPE_INT_RGB);
    private final Random rng = new Random();
    private int frameTick = 0;

    // --- Fade-in -------------------------------------------------------------
    private double fadeIn = 0;                 // 0..1
    private static final double FADE_SPEED = 2.5;

    // --- Death info ----------------------------------------------------------
    private final String cause;

    // --- Buttons -------------------------------------------------------------
    private static final int BTN_W = 220, BTN_H = 42;
    private final Rectangle retryBtn;
    private final Rectangle menuBtn;
    private int hoveredBtn = 0; // 0 = none, 1 = retry, 2 = menu

    // --- Fonts & colours -----------------------------------------------------
    private static final Font F_TITLE = new Font("Courier New", Font.BOLD, 38);
    private static final Font F_SUB   = new Font("Courier New", Font.BOLD, 14);
    private static final Font F_CAUSE = new Font("Courier New", Font.PLAIN, 13);
    private static final Font F_BTN   = new Font("Courier New", Font.BOLD, 16);

    // =========================================================================

    public DeathScreen(String deathCause, int panelW, int panelH) {
        this.cause = deathCause;
        int cx = panelW / 2;
        retryBtn = new Rectangle(cx - BTN_W / 2, 370, BTN_W, BTN_H);
        menuBtn  = new Rectangle(cx - BTN_W / 2, 430, BTN_W, BTN_H);
        regenerateStatic();
    }

    // --- Update (call every frame) -------------------------------------------

    public void update(int mx, int my) {
        // Fade in
        if (fadeIn < 1.0) fadeIn = Math.min(1.0, fadeIn + FADE_SPEED / 60.0);

        // Regenerate static every 2 frames for jitter
        frameTick++;
        if (frameTick % 2 == 0) regenerateStatic();

        // Button hover
        hoveredBtn = 0;
        if (retryBtn.contains(mx, my)) hoveredBtn = 1;
        if (menuBtn.contains(mx, my))  hoveredBtn = 2;
    }

    public Action handleClick(int mx, int my) {
        if (fadeIn < 0.8) return Action.NONE;
        if (retryBtn.contains(mx, my)) return Action.RETRY;
        if (menuBtn.contains(mx, my))  return Action.MAIN_MENU;
        return Action.NONE;
    }

    // --- Static noise generation ---------------------------------------------

    private void regenerateStatic() {
        for (int y = 0; y < STATIC_H; y++) {
            // Occasional bright interference row
            boolean hotRow = rng.nextDouble() < 0.03;
            for (int x = 0; x < STATIC_W; x++) {
                int v;
                if (hotRow) {
                    v = 100 + rng.nextInt(120);
                } else {
                    v = rng.nextInt(65);
                }
                // Slight colour cast (greenish, like old CCTV)
                int r = Math.min(255, v + rng.nextInt(6));
                int g = Math.min(255, v + rng.nextInt(10));
                int b = Math.min(255, v);
                staticBuf.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
    }

    // --- Rendering -----------------------------------------------------------

    public void draw(Graphics2D g2, int w, int h) {
        int alpha = (int)(255 * fadeIn);

        // 1. Static noise background (scaled up for chunky look)
        Composite saved = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) fadeIn));
        g2.drawImage(staticBuf, 0, 0, w, h, null);
        g2.setComposite(saved);

        // 2. Dark overlay to dim the static
        g2.setColor(new Color(0, 0, 0, Math.min(alpha, 180)));
        g2.fillRect(0, 0, w, h);

        // 3. Horizontal glitch bars
        for (int i = 0; i < 5; i++) {
            int by = rng.nextInt(h);
            int bh = 1 + rng.nextInt(3);
            g2.setColor(new Color(rng.nextInt(40), rng.nextInt(40), rng.nextInt(40),
                                  40 + rng.nextInt(40)));
            g2.fillRect(0, by, w, bh);
        }

        if (fadeIn < 0.4) return; // don't show text until fade is partway done

        int textAlpha = Math.min(255, (int)(255 * (fadeIn - 0.3) / 0.7));

        // 4. "SUBJECT TERMINATED" with chromatic aberration
        String title = "SUBJECT TERMINATED";
        g2.setFont(F_TITLE);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(title);
        int tx = (w - tw) / 2;
        int ty = 220;

        // Red offset (left)
        g2.setColor(new Color(220, 30, 20, textAlpha / 2));
        g2.drawString(title, tx - 3, ty);
        // Cyan offset (right)
        g2.setColor(new Color(30, 200, 220, textAlpha / 2));
        g2.drawString(title, tx + 3, ty + 1);
        // Main white text
        g2.setColor(new Color(230, 225, 220, textAlpha));
        g2.drawString(title, tx, ty);

        // 5. Subtitle
        g2.setFont(F_SUB);
        String sub = "SCP FOUNDATION  -  SECURE . CONTAIN . PROTECT";
        fm = g2.getFontMetrics();
        g2.setColor(new Color(160, 150, 140, textAlpha * 3 / 4));
        g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, ty + 35);

        // 6. Cause of death
        g2.setFont(F_CAUSE);
        String causeStr = "CAUSE OF DEATH:  " + cause;
        fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 60, 50, textAlpha));
        g2.drawString(causeStr, (w - fm.stringWidth(causeStr)) / 2, ty + 65);

        // 7. Divider line
        g2.setColor(new Color(100, 90, 80, textAlpha / 2));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(w / 2 - 140, ty + 85, w / 2 + 140, ty + 85);

        // 8. Classification stamp
        String stamp = "[LEVEL 4 CLEARANCE REQUIRED]";
        g2.setFont(new Font("Courier New", Font.PLAIN, 11));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(140, 130, 110, textAlpha / 2));
        g2.drawString(stamp, (w - fm.stringWidth(stamp)) / 2, ty + 100);

        // 9. Buttons
        drawButton(g2, retryBtn, "[ RETRY ]", hoveredBtn == 1, textAlpha);
        drawButton(g2, menuBtn,  "[ MAIN MENU ]", hoveredBtn == 2, textAlpha);

        // 10. Scanlines over everything
        g2.setColor(new Color(0, 0, 0, 18));
        for (int y = 0; y < h; y += 3) g2.drawLine(0, y, w, y);
    }

    private void drawButton(Graphics2D g2, Rectangle r, String label,
                            boolean hovered, int alpha) {
        // Background
        if (hovered) {
            g2.setColor(new Color(180, 40, 30, Math.min(alpha, 160)));
        } else {
            g2.setColor(new Color(50, 45, 40, Math.min(alpha, 140)));
        }
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);

        // Border
        g2.setColor(new Color(hovered ? 220 : 120, hovered ? 80 : 100,
                              hovered ? 60 : 90, alpha));
        g2.setStroke(new BasicStroke(hovered ? 2f : 1f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);

        // Label
        g2.setFont(F_BTN);
        FontMetrics fm = g2.getFontMetrics();
        int lx = r.x + (r.width - fm.stringWidth(label)) / 2;
        int ly = r.y + (r.height + fm.getAscent()) / 2 - 2;
        g2.setColor(new Color(hovered ? 255 : 200, hovered ? 220 : 200,
                              hovered ? 200 : 190, alpha));
        g2.drawString(label, lx, ly);
    }
}
