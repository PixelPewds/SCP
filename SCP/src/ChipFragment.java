import java.awt.*;

/**
 * A collectible chip fragment placed inside SCP containment cells.
 * Appears as a glowing circuit chip on the floor. Auto-collected
 * when the player walks within pickup range.
 */
public class ChipFragment {

    private final double x, y;
    private boolean collected = false;
    private double pulsePhase = 0;

    private static final int SIZE         = 12;
    private static final double PICKUP_DIST = 50;

    // --- Positions per cell (indexed 0..4 for SCP-001..SCP-005) ----------------
    private static final int[][] POSITIONS = {
        {660, 460},   // SCP-001
        {140, 140},   // SCP-002
        {630, 130},   // SCP-003
        {170, 440},   // SCP-004
        {670, 200},   // SCP-005
    };

    // --- Colours ---------------------------------------------------------------
    private static final Color CHIP_BASE  = new Color(50, 200, 210);
    private static final Color CHIP_DARK  = new Color(30, 120, 130);
    private static final Color GLOW_OUTER = new Color(40, 200, 220, 30);
    private static final Color TRACE_COL  = new Color(50, 180, 200, 100);

    // =========================================================================

    /** Create a fragment for the given cell label (e.g. "SCP-001"). */
    public ChipFragment(String cellLabel) {
        int idx = parseCellIndex(cellLabel);
        if (idx >= 0 && idx < POSITIONS.length) {
            x = POSITIONS[idx][0];
            y = POSITIONS[idx][1];
        } else {
            x = 400; y = 300; // fallback centre
        }
    }

    private static int parseCellIndex(String label) {
        // "SCP-001" -> 0, "SCP-002" -> 1, etc.
        try {
            return Integer.parseInt(label.substring(4)) - 1;
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isCollected() { return collected; }

    // --- Update ---------------------------------------------------------------

    public void update() {
        pulsePhase += 0.07;
        if (pulsePhase > 2 * Math.PI) pulsePhase -= 2 * Math.PI;
    }

    /** Check if the player is close enough to auto-collect. */
    public boolean checkPickup(double px, double py) {
        if (collected) return false;
        double dx = px - x, dy = py - y;
        if (Math.sqrt(dx * dx + dy * dy) < PICKUP_DIST) {
            collected = true;
            return true;
        }
        return false;
    }

    // --- Rendering ------------------------------------------------------------

    public void draw(Graphics2D g2) {
        if (collected) return;

        double pulse = 0.5 + 0.5 * Math.sin(pulsePhase);
        int floatY = (int)(y + Math.sin(pulsePhase * 0.6) * 3);  // gentle float

        // Outer glow rings
        for (int i = 3; i >= 0; i--) {
            int r = SIZE + 8 + i * 8 + (int)(pulse * 6);
            g2.setColor(new Color(GLOW_OUTER.getRed(), GLOW_OUTER.getGreen(),
                                  GLOW_OUTER.getBlue(), Math.max(5, GLOW_OUTER.getAlpha() / (i + 1))));
            g2.fillOval((int)(x - r), floatY - r, r * 2, r * 2);
        }

        // Shadow on floor
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillOval((int)(x - SIZE / 2 - 2), (int)(y + 6), SIZE + 4, 4);

        // Circuit traces extending outward
        g2.setColor(TRACE_COL);
        g2.setStroke(new BasicStroke(1f));
        int half = SIZE / 2;
        // 4 traces in cardinal directions
        g2.drawLine((int)(x - half - 8), floatY, (int)(x - half), floatY);
        g2.drawLine((int)(x + half), floatY, (int)(x + half + 8), floatY);
        g2.drawLine((int) x, floatY - half - 8, (int) x, floatY - half);
        g2.drawLine((int) x, floatY + half, (int) x, floatY + half + 8);
        // Small corner traces
        g2.drawLine((int)(x - half), floatY - half, (int)(x - half - 5), floatY - half - 5);
        g2.drawLine((int)(x + half), floatY - half, (int)(x + half + 5), floatY - half - 5);
        g2.drawLine((int)(x - half), floatY + half, (int)(x - half - 5), floatY + half + 5);
        g2.drawLine((int)(x + half), floatY + half, (int)(x + half + 5), floatY + half + 5);

        // Chip body (dark base)
        g2.setColor(CHIP_DARK);
        g2.fillRect((int)(x - half), floatY - half, SIZE, SIZE);

        // Inner bright core (pulses)
        int coreAlpha = (int)(180 + 70 * pulse);
        g2.setColor(new Color(CHIP_BASE.getRed(), CHIP_BASE.getGreen(),
                              CHIP_BASE.getBlue(), coreAlpha));
        g2.fillRect((int)(x - half + 2), floatY - half + 2, SIZE - 4, SIZE - 4);

        // Cross pattern on chip face
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawLine((int) x, floatY - half + 2, (int) x, floatY + half - 2);
        g2.drawLine((int)(x - half + 2), floatY, (int)(x + half - 2), floatY);

        // Highlight
        g2.setColor(new Color(200, 255, 255, (int)(100 + 60 * pulse)));
        g2.fillRect((int)(x - half + 2), floatY - half + 2, 3, 3);

        // Border
        g2.setColor(new Color(80, 240, 255, (int)(120 + 80 * pulse)));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect((int)(x - half), floatY - half, SIZE, SIZE);
    }
}
