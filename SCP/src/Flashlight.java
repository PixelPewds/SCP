import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Full-screen darkness overlay with a flashlight cone cut-out that
 * follows the mouse cursor. Includes random flickering, CRT scanlines,
 * film grain, and a rolling interference bar for a bodycam / old-monitor look.
 */
public class Flashlight {

    // --- Cone geometry -------------------------------------------------------
    private static final int    BASE_RANGE    = 280;
    private static final double HALF_CONE_RAD = Math.toRadians(28);
    private static final int    CONE_SEGMENTS = 30;
    private static final int    BASE_DARKNESS = 210;

    // --- Flicker state -------------------------------------------------------
    private double  flickerTimer     = 0;
    private double  nextFlickerIn    = 2.0;
    private boolean flickering       = false;
    private double  flickerRemaining = 0;
    private double  intensity        = 1.0;
    private final Random rng = new Random();

    // --- Off-screen buffers --------------------------------------------------
    private final BufferedImage buf;           // darkness layer
    private final BufferedImage scanlineTex;   // pre-rendered scanlines
    private final int w, h;

    // --- CRT / bodycam state -------------------------------------------------
    private double rollingBarY   = 0;          // position of the rolling bar
    private double grainSeed     = 0;          // changes each frame for grain
    private static final int    SCANLINE_GAP   = 3;     // px between scanlines
    private static final int    SCANLINE_ALPHA = 25;    // darkness of lines
    private static final int    GRAIN_COUNT    = 600;   // noise dots per frame
    private static final double BAR_SPEED      = 80.0;  // px/sec rolling bar
    private static final int    BAR_HEIGHT     = 30;    // px tall
    private static final int    BAR_ALPHA      = 18;    // subtlety of bar

    // =========================================================================

    public Flashlight(int width, int height) {
        this.w   = width;
        this.h   = height;
        this.buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.scanlineTex = createScanlineTexture(width, height);
        scheduleFlicker();
    }

    /** Pre-render horizontal scanlines once — never changes. */
    private BufferedImage createScanlineTexture(int tw, int th) {
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sg = img.createGraphics();
        sg.setColor(new Color(0, 0, 0, SCANLINE_ALPHA));
        for (int y = 0; y < th; y += SCANLINE_GAP) {
            sg.drawLine(0, y, tw, y);
        }
        // Thicker lines every 12px for CRT character-row effect
        sg.setColor(new Color(0, 0, 0, SCANLINE_ALPHA / 2));
        for (int y = 0; y < th; y += SCANLINE_GAP * 4) {
            sg.fillRect(0, y, tw, 2);
        }
        sg.dispose();
        return img;
    }

    private void scheduleFlicker() {
        nextFlickerIn = 0.8 + rng.nextDouble() * 3.5;
        flickerTimer  = 0;
    }

    // --- Update --------------------------------------------------------------

    public void update(double dt) {
        // Flicker logic
        if (flickering) {
            flickerRemaining -= dt;
            if (flickerRemaining <= 0) {
                flickering = false;
                intensity  = 1.0;
                scheduleFlicker();
            } else {
                intensity = 0.15 + rng.nextDouble() * 0.55;
            }
        } else {
            flickerTimer += dt;
            if (flickerTimer >= nextFlickerIn) {
                flickering       = true;
                flickerRemaining = 0.06 + rng.nextDouble() * 0.18;
            }
        }

        // Rolling bar scrolls down continuously
        rollingBarY += BAR_SPEED * dt;
        if (rollingBarY > h + BAR_HEIGHT) rollingBarY = -BAR_HEIGHT;

        // Grain seed changes every frame
        grainSeed = rng.nextDouble() * 10000;
    }

    // --- Rendering -----------------------------------------------------------

    public void draw(Graphics2D g2, double px, double py,
                     int mouseX, int mouseY, double sanityFactor) {

        Graphics2D dg = buf.createGraphics();
        dg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Fill with darkness
        int darkness = (int)(BASE_DARKNESS + (245 - BASE_DARKNESS) * (1.0 - sanityFactor));
        darkness = Math.min(250, darkness);
        dg.setComposite(AlphaComposite.Src);
        dg.setColor(new Color(0, 0, 0, darkness));
        dg.fillRect(0, 0, w, h);

        // 2. Cut flashlight cone
        if (intensity > 0.05) {
            double angle    = Math.atan2(mouseY - py, mouseX - px);
            int    range    = (int)(BASE_RANGE * intensity);
            double halfCone = HALF_CONE_RAD * (0.85 + 0.15 * intensity);

            int[] xp = new int[CONE_SEGMENTS + 2];
            int[] yp = new int[CONE_SEGMENTS + 2];
            xp[0] = (int) px;
            yp[0] = (int) py;
            double startA = angle - halfCone;
            for (int i = 0; i <= CONE_SEGMENTS; i++) {
                double a = startA + (2 * halfCone * i / CONE_SEGMENTS);
                xp[i + 1] = (int)(px + Math.cos(a) * range);
                yp[i + 1] = (int)(py + Math.sin(a) * range);
            }
            Polygon cone = new Polygon(xp, yp, CONE_SEGMENTS + 2);

            dg.setClip(cone);
            dg.setComposite(AlphaComposite.DstOut);
            float fi = (float) intensity;
            RadialGradientPaint light = new RadialGradientPaint(
                (float) px, (float) py, (float) range,
                new float[]{0f, 0.25f, 0.65f, 1f},
                new Color[]{
                    new Color(255, 255, 255, (int)(255 * fi)),
                    new Color(255, 255, 255, (int)(210 * fi)),
                    new Color(255, 255, 255, (int)(90  * fi)),
                    new Color(255, 255, 255, 0)
                }
            );
            dg.setPaint(light);
            dg.fill(cone);
            dg.setClip(null);
        }

        // 3. Ambient glow around player
        dg.setComposite(AlphaComposite.DstOut);
        RadialGradientPaint ambient = new RadialGradientPaint(
            (float) px, (float) py, 55f,
            new float[]{0f, 1f},
            new Color[]{new Color(255, 255, 255, 70), new Color(255, 255, 255, 0)}
        );
        dg.setPaint(ambient);
        dg.fillOval((int) px - 55, (int) py - 55, 110, 110);

        dg.dispose();

        // 4. Composite darkness onto game
        g2.drawImage(buf, 0, 0, null);

        // 5. Warm light tint
        if (intensity > 0.1) {
            double angle = Math.atan2(mouseY - py, mouseX - px);
            float tx = (float)(px + Math.cos(angle) * 60);
            float ty = (float)(py + Math.sin(angle) * 60);
            int tintAlpha = (int)(12 * intensity);
            RadialGradientPaint tint = new RadialGradientPaint(
                tx, ty, (float)(BASE_RANGE * intensity * 0.6f),
                new float[]{0f, 1f},
                new Color[]{new Color(255, 240, 180, tintAlpha), new Color(255, 240, 180, 0)}
            );
            g2.setPaint(tint);
            g2.fillRect(0, 0, w, h);
        }

        // =====================================================================
        // CRT / BODYCAM POST-PROCESSING
        // =====================================================================

        // 6. Scanlines (pre-rendered texture, drawn once)
        g2.drawImage(scanlineTex, 0, 0, null);

        // 7. Film grain (random noise redrawn each frame)
        drawGrain(g2, sanityFactor);

        // 8. Rolling interference bar (horizontal band scrolling down)
        drawRollingBar(g2);

        // 9. Chromatic aberration fringe (subtle colour offset at edges)
        drawChromaticFringe(g2, px, py, sanityFactor);

        // 10. Timestamp / REC overlay (bodycam feel)
        drawRecOverlay(g2);
    }

    // --- CRT effects ---------------------------------------------------------

    private void drawGrain(Graphics2D g2, double sanityFactor) {
        Random gr = new Random((long) grainSeed);
        // More grain at lower sanity
        int count = (int)(GRAIN_COUNT * (0.6 + 0.4 * (1.0 - sanityFactor)));

        for (int i = 0; i < count; i++) {
            int gx = gr.nextInt(w);
            int gy = gr.nextInt(h);
            int sz = 1 + gr.nextInt(2);
            boolean bright = gr.nextBoolean();
            int alpha = 8 + gr.nextInt(20);
            if (bright) {
                g2.setColor(new Color(200, 200, 190, alpha));
            } else {
                g2.setColor(new Color(0, 0, 0, alpha + 10));
            }
            g2.fillRect(gx, gy, sz, sz);
        }
    }

    private void drawRollingBar(Graphics2D g2) {
        // Semi-transparent horizontal band that scrolls down
        int by = (int) rollingBarY;

        // Main bar (subtle brightness shift)
        for (int i = 0; i < BAR_HEIGHT; i++) {
            int y = by + i;
            if (y < 0 || y >= h) continue;
            // Bell curve intensity across the bar height
            double t = (double) i / BAR_HEIGHT;
            double bell = Math.exp(-((t - 0.5) * (t - 0.5)) / 0.05);
            int alpha = (int)(BAR_ALPHA * bell);
            g2.setColor(new Color(180, 180, 170, alpha));
            g2.drawLine(0, y, w, y);
        }

        // Faint secondary bar (offset, dimmer)
        int by2 = (by + h / 3) % (h + BAR_HEIGHT * 2) - BAR_HEIGHT;
        for (int i = 0; i < BAR_HEIGHT / 2; i++) {
            int y = by2 + i;
            if (y < 0 || y >= h) continue;
            double t = (double) i / (BAR_HEIGHT / 2);
            double bell = Math.exp(-((t - 0.5) * (t - 0.5)) / 0.08);
            int alpha = (int)(BAR_ALPHA * 0.4 * bell);
            g2.setColor(new Color(160, 170, 160, alpha));
            g2.drawLine(0, y, w, y);
        }
    }

    private void drawChromaticFringe(Graphics2D g2, double px, double py,
                                     double sanityFactor) {
        // Subtle red/cyan offset lines at the screen edges — stronger at low sanity
        int strength = (int)(3 + 4 * (1.0 - sanityFactor));
        int alpha = (int)(8 + 12 * (1.0 - sanityFactor));

        // Top edge
        g2.setColor(new Color(255, 60, 40, alpha));
        g2.fillRect(0, 0, w, strength);
        g2.setColor(new Color(40, 200, 255, alpha));
        g2.fillRect(0, strength, w, strength);

        // Bottom edge
        g2.setColor(new Color(40, 200, 255, alpha));
        g2.fillRect(0, h - strength * 2, w, strength);
        g2.setColor(new Color(255, 60, 40, alpha));
        g2.fillRect(0, h - strength, w, strength);

        // Left edge
        g2.setColor(new Color(255, 60, 40, alpha));
        g2.fillRect(0, 0, strength, h);

        // Right edge
        g2.setColor(new Color(40, 200, 255, alpha));
        g2.fillRect(w - strength, 0, strength, h);
    }

    private void drawRecOverlay(Graphics2D g2) {
        // Blinking REC dot + timestamp in top-left corner
        long ms = System.currentTimeMillis();
        boolean recVisible = (ms / 600) % 2 == 0; // blink every 0.6s

        if (recVisible) {
            // Red dot
            g2.setColor(new Color(220, 30, 20, 180));
            g2.fillOval(705, 10, 10, 10);

            // REC text
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(new Color(220, 30, 20, 180));
            g2.drawString("REC", 720, 20);
        }

        // Timestamp (always visible)
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(180, 180, 170, 120));
        long sec = (ms / 1000) % 60;
        long min = (ms / 60000) % 60;
        long hr  = (ms / 3600000) % 24;
        g2.drawString(String.format("CAM-07  %02d:%02d:%02d", hr, min, sec),
                      680, 35);
    }
}
