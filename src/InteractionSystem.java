import java.awt.*;

/**
 * Handles player proximity detection to interactable objects (desks)
 * and manages the ContainmentLog overlay lifecycle.
 */
public class InteractionSystem {

    private static final double INTERACT_DIST = 100; // px

    private ContainmentLog openLog;
    private boolean canInteract;
    private RoomObject nearestDesk;

    // --- Update (called every frame) -----------------------------------------

    public void update(double playerX, double playerY, RoomObject[] objects) {
        nearestDesk = null;
        canInteract = false;

        if (openLog != null) return; // skip proximity check while reading

        for (RoomObject obj : objects) {
            if (obj.getType() == RoomObject.Type.DESK) {
                double dx = playerX - (obj.getX() + obj.getW() / 2.0);
                double dy = playerY - (obj.getY() + obj.getH() / 2.0);
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < INTERACT_DIST) {
                    nearestDesk = obj;
                    canInteract = true;
                    break;
                }
            }
        }
    }

    // --- Key handlers --------------------------------------------------------

    public void onInteractKey(int panelW, int panelH) {
        if (openLog != null) {
            openLog = null;             // close log
        } else if (canInteract) {
            openLog = new ContainmentLog(panelW, panelH);
        }
    }

    public void onEscapeKey() {
        if (openLog != null) openLog = null;
    }

    // --- Mouse forwarding ----------------------------------------------------

    public void handleMousePressed(int x, int y) {
        if (openLog != null) openLog.handleMousePressed(x, y);
    }

    public void handleMouseDragged(int x, int y) {
        if (openLog != null) openLog.handleMouseDragged(x, y);
    }

    public void handleMouseReleased() {
        if (openLog != null) openLog.handleMouseReleased();
    }

    // --- State queries -------------------------------------------------------

    public boolean isLogOpen()    { return openLog != null; }
    public boolean canInteract()  { return canInteract; }

    public void reset() { openLog = null; canInteract = false; nearestDesk = null; }

    // --- Drawing -------------------------------------------------------------

    /** Draw the interaction prompt near the desk. */
    public void drawPrompt(Graphics2D g2) {
        if (!canInteract || nearestDesk == null) return;

        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.005);
        int alpha = (int) (160 + 80 * pulse);

        String msg = "Press [E] to read Containment Log";
        Font font = new Font("Consolas", Font.BOLD, 13);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(msg);

        int px = nearestDesk.getX() + nearestDesk.getW() / 2 - tw / 2 - 10;
        int py = nearestDesk.getY() - 30;

        // Background pill
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(px, py - 16, tw + 20, 26, 10, 10);

        // Border
        g2.setColor(new Color(200, 180, 140, 80));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(px, py - 16, tw + 20, 26, 10, 10);

        // Text
        g2.setColor(new Color(220, 200, 160, alpha));
        g2.drawString(msg, px + 10, py);
    }

    /** Draw the containment log overlay. */
    public void drawLog(Graphics2D g2) {
        if (openLog != null) openLog.draw(g2);
    }
}
