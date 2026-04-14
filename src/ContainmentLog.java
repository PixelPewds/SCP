import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A draggable "Containment Log" UI panel rendered on old yellowed paper
 * with typewriter text and a randomly generated 4-digit access code.
 */
public class ContainmentLog {

    private static final int LOG_W = 420, LOG_H = 530;

    // --- Position and dragging -----------------------------------------------
    private int logX, logY;
    private int dragOffX, dragOffY;
    private boolean dragging = false;

    // --- Content -------------------------------------------------------------
    private final String accessCode;
    private final int    logNumber;
    private final String date;
    private final String site;

    // --- Pre-rendered paper texture ------------------------------------------
    private final BufferedImage paper;

    // --- Fonts ---------------------------------------------------------------
    private static final Font F_TITLE  = new Font("Courier New", Font.BOLD,  16);
    private static final Font F_SUB    = new Font("Courier New", Font.PLAIN, 10);
    private static final Font F_HEAD   = new Font("Courier New", Font.BOLD,  13);
    private static final Font F_BODY   = new Font("Courier New", Font.PLAIN, 12);
    private static final Font F_BOLD   = new Font("Courier New", Font.BOLD,  12);
    private static final Font F_CODE   = new Font("Courier New", Font.BOLD,  22);
    private static final Font F_STAMP  = new Font("Courier New", Font.BOLD,  38);
    private static final Font F_HINT   = new Font("Consolas",    Font.PLAIN, 11);

    // --- Ink colours ---------------------------------------------------------
    private static final Color INK       = new Color(30, 25, 20);
    private static final Color INK_GRAY  = new Color(80, 75, 65);
    private static final Color INK_RED   = new Color(160, 30, 20);
    private static final Color REDACT_BG = new Color(15, 12, 10);

    // =========================================================================

    public ContainmentLog(int panelW, int panelH) {
        logX = (panelW - LOG_W) / 2;
        logY = (panelH - LOG_H) / 2;

        Random rng = new Random();
        accessCode = String.format("%04d", rng.nextInt(10000));
        logNumber  = 1000 + rng.nextInt(9000);
        site       = String.valueOf(11 + rng.nextInt(40));
        int mo = 1 + rng.nextInt(12);
        int dy = 1 + rng.nextInt(28);
        date = String.format("2024/%02d/%02d", mo, dy);

        paper = createPaper(rng.nextLong());
    }

    // --- Paper texture -------------------------------------------------------

    private BufferedImage createPaper(long seed) {
        BufferedImage img = new BufferedImage(LOG_W, LOG_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random r = new Random(seed);

        // Base fill
        g.setColor(new Color(215, 200, 170));
        g.fillRect(0, 0, LOG_W, LOG_H);

        // Foxing / age spots
        for (int i = 0; i < 400; i++) {
            int nx = r.nextInt(LOG_W), ny = r.nextInt(LOG_H);
            int sz = 1 + r.nextInt(4);
            boolean dark = r.nextBoolean();
            int a = 10 + r.nextInt(25);
            g.setColor(dark ? new Color(140, 110, 70, a) : new Color(235, 225, 205, a));
            g.fillOval(nx, ny, sz, sz);
        }

        // Edge darkening (four gradient strips)
        int edgeW = 40;
        for (int i = 0; i < edgeW; i++) {
            int a = (int)(50 * (1.0 - (double) i / edgeW));
            g.setColor(new Color(80, 60, 30, a));
            g.drawLine(i, 0, i, LOG_H);                 // left
            g.drawLine(LOG_W - 1 - i, 0, LOG_W - 1 - i, LOG_H); // right
            g.drawLine(0, i, LOG_W, i);                 // top
            g.drawLine(0, LOG_H - 1 - i, LOG_W, LOG_H - 1 - i); // bottom
        }

        // Fold lines
        g.setColor(new Color(170, 155, 130, 80));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(0, LOG_H / 3, LOG_W, LOG_H / 3);
        g.drawLine(0, LOG_H * 2 / 3, LOG_W, LOG_H * 2 / 3);

        // Coffee ring stain
        int cx = 300 + r.nextInt(60), cy = 60 + r.nextInt(80), cr = 25 + r.nextInt(15);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(130, 90, 50, 35));
        g.drawOval(cx - cr, cy - cr, cr * 2, cr * 2);
        g.setColor(new Color(130, 90, 50, 15));
        g.fillOval(cx - cr + 3, cy - cr + 3, cr * 2 - 6, cr * 2 - 6);

        // Water damage blotch
        for (int i = 0; i < 5; i++) {
            int bx = 20 + r.nextInt(100), by = 350 + r.nextInt(100);
            int bsz = 15 + r.nextInt(30);
            g.setColor(new Color(180, 165, 140, 20 + r.nextInt(20)));
            g.fillOval(bx, by, bsz, bsz);
        }

        g.dispose();
        return img;
    }

    // --- Drawing -------------------------------------------------------------

    public void draw(Graphics2D g2) {
        // Dim overlay behind log
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, 800, 600);

        // Paper shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fillRect(logX + 6, logY + 6, LOG_W, LOG_H);

        // Paper texture
        g2.drawImage(paper, logX, logY, null);

        // Border
        g2.setColor(new Color(160, 140, 110));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(logX, logY, LOG_W - 1, LOG_H - 1);

        // --- Text content (relative to logX, logY) ---
        int lx = logX + 30;   // left margin
        int ty = logY + 45;   // starting y

        // Header
        drawTW(g2, "SCP FOUNDATION", lx + 95, ty, F_TITLE, INK);
        ty += 18;
        drawTW(g2, "SECURE . CONTAIN . PROTECT", lx + 72, ty, F_SUB, INK_GRAY);
        ty += 16;
        g2.setColor(INK);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(lx, ty, logX + LOG_W - 30, ty);

        // Log info
        ty += 22;
        drawTW(g2, "CONTAINMENT LOG #" + logNumber, lx, ty, F_HEAD, INK);
        ty += 20;
        drawTW(g2, "DATE: " + date + "  TIME: 0347", lx, ty, F_BODY, INK);
        ty += 16;
        drawTW(g2, "FACILITY: Site-" + site, lx, ty, F_BODY, INK);
        ty += 16;
        drawTW(g2, "CLEARANCE LEVEL: 3", lx, ty, F_BODY, INK);
        ty += 20;
        g2.setColor(new Color(80, 70, 55, 120));
        g2.drawLine(lx, ty, logX + LOG_W - 30, ty);

        // Subject
        ty += 20;
        drawTW(g2, "SUBJECT: SCP-173", lx, ty, F_BOLD, INK);
        ty += 16;
        drawTW(g2, "CLASS:   EUCLID", lx, ty, F_BODY, INK);

        // Notes header
        ty += 26;
        drawTW(g2, "CONTAINMENT NOTES:", lx, ty, F_BOLD, INK);
        g2.drawLine(lx, ty + 3, lx + 155, ty + 3);
        ty += 20;

        // Body text
        String[] lines = {
            "Subject was observed moving during",
            "a power outage in Sector 7-G at",
            "approximately 0347 hrs.",
            "",
            "Personnel report scraping sounds",
            "from within the containment cell.",
            "Blood residue found near eastern",
        };
        for (String line : lines) {
            drawTW(g2, line, lx, ty, F_BODY, INK);
            ty += 15;
        }

        // Redacted line
        drawTW(g2, "wall. Source: ", lx, ty, F_BODY, INK);
        int rStart = lx + g2.getFontMetrics(F_BODY).stringWidth("wall. Source: ");
        g2.setColor(REDACT_BG);
        g2.fillRect(rStart, ty - 11, 90, 14);
        ty += 22;

        drawTW(g2, "All personnel must maintain", lx, ty, F_BODY, INK);
        ty += 15;
        drawTW(g2, "DIRECT VISUAL CONTACT at all", lx, ty, F_BOLD, INK);
        ty += 15;
        drawTW(g2, "times. DO NOT BLINK.", lx, ty, F_BOLD, INK_RED);

        // Access code section
        ty += 24;
        g2.setColor(new Color(80, 70, 55, 120));
        g2.drawLine(lx, ty, logX + LOG_W - 30, ty);
        ty += 22;
        drawTW(g2, "EMERGENCY ACCESS CODE:", lx, ty, F_BOLD, INK);
        ty += 30;

        // The code itself
        String codeStr = ">>  " + accessCode + "  <<";
        FontMetrics cfm = g2.getFontMetrics(F_CODE);
        int codeW = cfm.stringWidth(codeStr);
        int codeX = logX + (LOG_W - codeW) / 2;
        drawTW(g2, codeStr, codeX, ty, F_CODE, INK_RED);

        // Circle around code
        g2.setColor(new Color(180, 30, 20, 100));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(codeX - 10, ty - 20, codeW + 20, 32);

        // Footer
        ty += 30;
        g2.setColor(new Color(80, 70, 55, 120));
        g2.drawLine(lx, ty, logX + LOG_W - 30, ty);
        ty += 18;
        drawTW(g2, "Auth: Dr. ", lx, ty, F_BODY, INK);
        int authEnd = lx + g2.getFontMetrics(F_BODY).stringWidth("Auth: Dr. ");
        g2.setColor(REDACT_BG);
        g2.fillRect(authEnd, ty - 11, 80, 14);
        ty += 16;
        drawTW(g2, "Site Director, Site-" + site, lx, ty, F_BODY, INK_GRAY);

        // "CLASSIFIED" watermark
        Graphics2D g2c = (Graphics2D) g2.create();
        g2c.rotate(Math.toRadians(-25), logX + LOG_W / 2, logY + LOG_H / 2);
        g2c.setFont(F_STAMP);
        g2c.setColor(new Color(180, 30, 20, 30));
        g2c.drawString("CLASSIFIED", logX + 55, logY + LOG_H / 2 + 15);
        g2c.setStroke(new BasicStroke(2f));
        g2c.drawRect(logX + 48, logY + LOG_H / 2 - 25, 310, 50);
        g2c.dispose();

        // Close hint
        g2.setFont(F_HINT);
        g2.setColor(new Color(200, 190, 170, 180));
        g2.drawString("[E] or [ESC] to close", logX + LOG_W - 150, logY + LOG_H - 10);
    }

    /** Draw text with subtle typewriter misalignment. */
    private void drawTW(Graphics2D g2, String text, int x, int y, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        Random tr = new Random(text.hashCode());
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int oy = (int) ((tr.nextDouble() - 0.5) * 1.5);
            g2.drawString(String.valueOf(c), cx, y + oy);
            cx += fm.charWidth(c);
        }
    }

    // --- Drag handling -------------------------------------------------------

    public boolean handleMousePressed(int mx, int my) {
        if (mx >= logX && mx <= logX + LOG_W && my >= logY && my <= logY + LOG_H) {
            dragging = true;
            dragOffX = mx - logX;
            dragOffY = my - logY;
            return true;
        }
        return false;
    }

    public void handleMouseDragged(int mx, int my) {
        if (dragging) {
            logX = mx - dragOffX;
            logY = my - dragOffY;
        }
    }

    public void handleMouseReleased() {
        dragging = false;
    }

    public String getAccessCode() { return accessCode; }
}
