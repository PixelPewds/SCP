import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A piece of furniture inside the containment room.
 * Each object knows its type and where it sits on the 800x600 panel.
 */
public class RoomObject {

    public enum Type { DOOR, DESK, TERMINAL }

    private final Type type;
    private final int  x, y;      // top-left corner
    private final int  w, h;      // bounding box

    // --- Palette per type ----------------------------------------------------
    private static final Color DOOR_FILL     = new Color(50, 40, 35);
    private static final Color DOOR_FRAME    = new Color(90, 75, 60);
    private static final Color DOOR_KNOB     = new Color(180, 160, 80);

    private static final Color DESK_FILL     = new Color(55, 45, 40);
    private static final Color DESK_TOP      = new Color(75, 60, 50);
    private static final Color DESK_LEG      = new Color(40, 32, 28);

    private static final Color TERM_BODY     = new Color(35, 35, 40);
    private static final Color TERM_SCREEN   = new Color(0, 40, 0);
    private static final Color TERM_GLOW     = new Color(0, 200, 60, 50);
    private static final Color TERM_SCANLINE = new Color(0, 255, 80, 18);
    private static final Color TERM_TEXT     = new Color(0, 200, 60, 140);

    public RoomObject(Type type, int x, int y) {
        this.type = type;
        this.x    = x;
        this.y    = y;
        switch (type) {
            case DOOR:     w = 50;  h = 100; break;
            case DESK:     w = 100; h = 50;  break;
            case TERMINAL: w = 40;  h = 50;  break;
            default:       w = 40;  h = 40;
        }
    }

    public Type getType() { return type; }
    public int  getX()    { return x; }
    public int  getY()    { return y; }
    public int  getW()    { return w; }
    public int  getH()    { return h; }

    // --- Drawing -------------------------------------------------------------

    public void draw(Graphics2D g2) {
        switch (type) {
            case DOOR:     drawDoor(g2);     break;
            case DESK:     drawDesk(g2);     break;
            case TERMINAL: drawTerminal(g2); break;
        }
    }

    private void drawDoor(Graphics2D g2) {
        // Frame
        g2.setColor(DOOR_FRAME);
        g2.fillRect(x - 4, y - 4, w + 8, h + 4);

        // Door panel
        g2.setColor(DOOR_FILL);
        g2.fillRect(x, y, w, h);

        // Inset panels
        g2.setColor(DOOR_FRAME);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(x + 6, y + 6, w - 12, h / 2 - 10);
        g2.drawRect(x + 6, y + h / 2 + 4, w - 12, h / 2 - 10);

        // Knob
        g2.setColor(DOOR_KNOB);
        g2.fillOval(x + w - 14, y + h / 2 - 4, 8, 8);
        g2.setColor(DOOR_KNOB.brighter());
        g2.fillOval(x + w - 12, y + h / 2 - 2, 3, 3);

        // Label
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(120, 100, 80));
        g2.drawString("EXIT", x + 8, y + h + 14);
    }

    private void drawDesk(Graphics2D g2) {
        // Desk legs
        g2.setColor(DESK_LEG);
        g2.fillRect(x + 5,      y + h - 2, 6, 14);
        g2.fillRect(x + w - 11, y + h - 2, 6, 14);

        // Desk surface
        g2.setColor(DESK_FILL);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 6, 6));

        // Top highlight
        g2.setColor(DESK_TOP);
        g2.fill(new RoundRectangle2D.Float(x + 2, y + 2, w - 4, h / 3, 4, 4));

        // Some papers on desk
        g2.setColor(new Color(180, 175, 165, 80));
        g2.fillRect(x + 15, y + 10, 22, 30);
        g2.setColor(new Color(100, 95, 85, 60));
        for (int i = 0; i < 5; i++) {
            g2.drawLine(x + 18, y + 15 + i * 5, x + 33, y + 15 + i * 5);
        }

        // Label
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(100, 90, 75));
        g2.drawString("DESK", x + 35, y + h + 20);
    }

    private void drawTerminal(Graphics2D g2) {
        // Screen glow (behind)
        g2.setColor(TERM_GLOW);
        g2.fillOval(x - 10, y - 6, w + 20, h + 12);

        // Monitor body
        g2.setColor(TERM_BODY);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h - 10, 4, 4));

        // Screen
        g2.setColor(TERM_SCREEN);
        g2.fillRect(x + 3, y + 3, w - 6, h - 16);

        // Scanlines
        g2.setColor(TERM_SCANLINE);
        for (int sy = y + 3; sy < y + h - 13; sy += 3) {
            g2.drawLine(x + 3, sy, x + w - 4, sy);
        }

        // Fake text on screen
        g2.setFont(new Font("Consolas", Font.PLAIN, 7));
        g2.setColor(TERM_TEXT);
        g2.drawString("SCP-173", x + 5, y + 14);
        g2.drawString("STATUS:", x + 5, y + 22);
        g2.drawString("ACTIVE", x + 5, y + 30);

        // Monitor stand
        g2.setColor(TERM_BODY);
        g2.fillRect(x + w / 2 - 4, y + h - 10, 8, 8);
        g2.fillRect(x + w / 2 - 8, y + h - 4, 16, 4);

        // Label
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(0, 160, 60, 160));
        g2.drawString("TERM", x + 2, y + h + 12);
    }
}
