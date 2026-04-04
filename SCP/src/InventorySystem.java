import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks Chip Fragments collected from SCP cells.
 * A fragment is only permanently saved when the player escapes
 * back to the hallway alive. Dying forfeits the carried fragment.
 */
public class InventorySystem {

    private final Set<String> collected = new HashSet<>();
    private String carrying = null;   // held but not yet saved
    private static final int TOTAL_FRAGMENTS = 5;

    // --- Pickup message flash ------------------------------------------------
    private String pickupMsg = null;
    private double msgTimer  = 0;
    private static final double MSG_DURATION = 2.5;

    // --- Actions -------------------------------------------------------------

    /** Pick up a fragment inside a cell (not saved yet). */
    public void pickUp(String cellLabel) {
        carrying  = cellLabel;
        pickupMsg = "CHIP FRAGMENT ACQUIRED";
        msgTimer  = MSG_DURATION;
    }

    /** Save the carried fragment (called when returning to the hallway). */
    public void saveCarrying() {
        if (carrying != null) {
            collected.add(carrying);
            carrying = null;
        }
    }

    /** Drop the carried fragment (called on death). */
    public void dropCarrying() {
        carrying = null;
    }

    // --- Queries -------------------------------------------------------------

    public boolean isCollected(String cellLabel) { return collected.contains(cellLabel); }
    public boolean isCarrying()                  { return carrying != null; }
    public int     getCollectedCount()           { return collected.size(); }
    public int     getTotalFragments()           { return TOTAL_FRAGMENTS; }

    // --- Update --------------------------------------------------------------

    public void update(double dt) {
        if (pickupMsg != null) {
            msgTimer -= dt;
            if (msgTimer <= 0) pickupMsg = null;
        }
    }

    // --- HUD: fragment counter (always visible) ------------------------------

    public void drawCounter(Graphics2D g2, int panelWidth) {
        int bx = panelWidth - 170, by = 75;

        // Background
        g2.setColor(new Color(15, 15, 15, 200));
        g2.fillRoundRect(bx, by, 155, 26, 8, 8);
        g2.setColor(new Color(60, 180, 200, 80));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(bx, by, 155, 26, 8, 8);

        // Chip icon (small diamond)
        int ix = bx + 12, iy = by + 13;
        int[] dx = { ix, ix+5, ix, ix-5 };
        int[] dy = { iy-5, iy, iy+5, iy };
        Color chipCol = collected.size() >= TOTAL_FRAGMENTS
            ? new Color(60, 220, 255) : new Color(60, 180, 200);
        g2.setColor(chipCol);
        g2.fillPolygon(dx, dy, 4);
        g2.setColor(new Color(100, 220, 240, 120));
        g2.drawPolygon(dx, dy, 4);

        // Count text
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        g2.setColor(new Color(60, 200, 220));
        g2.drawString("Chips: " + collected.size() + " / " + TOTAL_FRAGMENTS,
                       bx + 24, by + 18);

        // Carrying indicator
        if (carrying != null) {
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.006);
            g2.setColor(new Color(60, 220, 180, (int)(100 + 100 * pulse)));
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.drawString("[CARRYING]", bx + 95, by + 18);
        }
    }

    // --- HUD: pickup message flash -------------------------------------------

    public void drawPickupMessage(Graphics2D g2, int panelWidth, int panelHeight) {
        if (pickupMsg == null) return;

        double fade = Math.min(1.0, msgTimer / 0.5);
        int alpha = (int)(220 * fade);

        g2.setFont(new Font("Courier New", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(pickupMsg);
        int tx = (panelWidth - tw) / 2;
        int ty = panelHeight / 2 - 40;

        // Background
        g2.setColor(new Color(0, 0, 0, alpha * 3 / 4));
        g2.fillRoundRect(tx - 16, ty - 22, tw + 32, 36, 10, 10);

        // Glow border
        g2.setColor(new Color(60, 220, 200, alpha / 2));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(tx - 16, ty - 22, tw + 32, 36, 10, 10);

        // Text with cyan glow
        g2.setColor(new Color(40, 200, 220, alpha / 3));
        g2.drawString(pickupMsg, tx + 1, ty + 1);
        g2.setColor(new Color(60, 240, 240, alpha));
        g2.drawString(pickupMsg, tx, ty);
    }
}
