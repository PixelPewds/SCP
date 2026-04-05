import java.awt.*;
import java.util.Random;

/**
 * Top-down scrolling corridor with labeled SCP doors on both sides.
 * Acts as the hub between the title screen and individual containment cells.
 * Doors are locked unless the player holds an Access Key.
 */
public class HallwayManager {

    // --- Corridor geometry (world space) -------------------------------------
    static final int COR_LEFT   = 280;
    static final int COR_RIGHT  = 520;
    static final int COR_WIDTH  = COR_RIGHT - COR_LEFT;   // 240px walkable
    static final int COR_TOP    = 0;
    static final int COR_BOTTOM = 2200;
    static final int WALL_THICK = 40;

    // --- Camera --------------------------------------------------------------
    private double cameraY;
    private static final double CAM_SMOOTH = 0.08;

    // --- Access key ----------------------------------------------------------
    private boolean hasAccessKey = false;

    // --- Player return position (when exiting a cell) ------------------------
    private double savedPlayerX = 400, savedPlayerY = 1900;

    // --- Interaction ---------------------------------------------------------
    private int nearDoorIdx = -1;
    private boolean nearConsole = false;
    private static final double INTERACT_DIST = 85;

    // --- Master Console at dead-end (top of corridor) -------------------------
    private static final int CONSOLE_X = (COR_LEFT + COR_RIGHT) / 2 - 30;
    private static final int CONSOLE_Y = 50;
    private static final int CONSOLE_W = 60;
    private static final int CONSOLE_H = 50;

    // --- Escape sequence state ------------------------------------------------
    private boolean escapeTriggered = false;
    private double escapeTimer = 0;

    // --- Ambient details seed ------------------------------------------------
    private final long seed;

    // --- Door data -----------------------------------------------------------
    static final int DOOR_W = 50, DOOR_H = 70;

    private final int[]     doorX, doorY;
    private final String[]  doorLabel;
    private final boolean[] doorLocked;
    private final boolean[] doorLeft;
    private final int       doorCount;

    // --- Colours -------------------------------------------------------------
    private static final Color FLOOR_BASE    = new Color(40, 38, 35);
    private static final Color FLOOR_TILE    = new Color(48, 45, 42);
    private static final Color WALL_BASE     = new Color(52, 48, 44);
    private static final Color WALL_DARK     = new Color(35, 32, 28);
    private static final Color DOOR_FILL     = new Color(68, 64, 58);
    private static final Color DOOR_FRAME    = new Color(50, 46, 40);
    private static final Color LABEL_COLOR   = new Color(200, 190, 170);
    private static final Color LOCKED_COLOR  = new Color(200, 40, 30);
    private static final Color UNLOCK_COLOR  = new Color(40, 200, 60);
    private static final Color HAZARD_YELLOW = new Color(200, 180, 40, 60);
    private static final Color CENTERLINE    = new Color(180, 160, 40, 30);


    // =========================================================================

    public HallwayManager() {
        seed = System.nanoTime();

        // Define doors (6 doors, 3 per side)
        doorCount = 6;
        doorX     = new int[doorCount];
        doorY     = new int[doorCount];
        doorLabel = new String[doorCount];
        doorLocked= new boolean[doorCount];
        doorLeft  = new boolean[doorCount];

        int[] yPositions = { 400, 750, 1100 };
        for (int row = 0; row < 3; row++) {
            int li = row * 2;       // left door index
            int ri = row * 2 + 1;   // right door index

            // Left door
            doorX[li]     = COR_LEFT - DOOR_W;
            doorY[li]     = yPositions[row];
            doorLabel[li] = String.format("SCP-%03d", li + 1);
            doorLocked[li]= (li >= 5);  // SCP-001,003,005 unlocked
            doorLeft[li]  = true;

            // Right door
            doorX[ri]     = COR_RIGHT;
            doorY[ri]     = yPositions[row];
            doorLabel[ri] = String.format("SCP-%03d", ri + 1);
            doorLocked[ri]= (ri >= 5);  // SCP-002,004 unlocked; SCP-006 locked
            doorLeft[ri]  = false;
        }
    }

    // --- Accessors -----------------------------------------------------------

    public double getCameraY()       { return cameraY; }
    public double getSavedPlayerX()  { return savedPlayerX; }
    public double getSavedPlayerY()  { return savedPlayerY; }
    public boolean hasAccessKey()    { return hasAccessKey; }
    public void setHasAccessKey(boolean v) { hasAccessKey = v; }
    public boolean isNearConsole()   { return nearConsole; }
    public boolean isEscapeTriggered() { return escapeTriggered; }
    public double  getEscapeTimer()  { return escapeTimer; }

    // --- Update --------------------------------------------------------------

    public void update(double playerX, double playerY, int viewH) {
        // Escape sequence timer
        if (escapeTriggered) {
            escapeTimer += 1.0 / 60.0;
            return;  // freeze hallway updates during escape
        }

        // Smooth camera follow
        double targetCamY = playerY - viewH / 2.0;
        targetCamY = Math.max(COR_TOP, Math.min(COR_BOTTOM - viewH, targetCamY));
        cameraY += (targetCamY - cameraY) * CAM_SMOOTH;

        // Door proximity check
        nearDoorIdx = -1;
        nearConsole = false;
        for (int i = 0; i < doorCount; i++) {
            double dcx = doorX[i] + DOOR_W / 2.0;
            double dcy = doorY[i] + DOOR_H / 2.0;
            double dx = playerX - dcx;
            double dy = playerY - dcy;
            if (Math.sqrt(dx * dx + dy * dy) < INTERACT_DIST) {
                nearDoorIdx = i;
                break;
            }
        }

        // Console proximity check
        double ccx = CONSOLE_X + CONSOLE_W / 2.0;
        double ccy = CONSOLE_Y + CONSOLE_H / 2.0;
        double cdx = playerX - ccx;
        double cdy = playerY - ccy;
        if (Math.sqrt(cdx * cdx + cdy * cdy) < INTERACT_DIST + 20) {
            nearConsole = true;
        }
    }

    /** Trigger the escape sequence. */
    public void triggerEscape() {
        escapeTriggered = true;
        escapeTimer = 0;
    }

    /**
     * Attempt to enter the nearby door. Returns the door label if
     * successfully entered, or null if no door or locked.
     */
    public String tryEnterDoor(double playerX, double playerY) {
        if (nearDoorIdx < 0) return null;

        boolean locked = doorLocked[nearDoorIdx];
        if (locked && !hasAccessKey) return null;

        // If locked but has key, unlock it
        if (locked && hasAccessKey) {
            doorLocked[nearDoorIdx] = false;
            hasAccessKey = false;
        }

        // Save position for when player returns
        savedPlayerX = playerX;
        savedPlayerY = playerY;

        return doorLabel[nearDoorIdx];
    }

    /** Clamp player within corridor bounds. */
    public void clampPlayer(double[] pos) {
        int margin = 18;
        if (pos[0] < COR_LEFT + margin)          pos[0] = COR_LEFT + margin;
        if (pos[0] > COR_RIGHT - margin)         pos[0] = COR_RIGHT - margin;
        if (pos[1] < COR_TOP + 60)               pos[1] = COR_TOP + 60;
        if (pos[1] > COR_BOTTOM - 60)            pos[1] = COR_BOTTOM - 60;
    }

    // =========================================================================
    // Drawing (world space — caller applies camera transform)
    // =========================================================================

    public void drawCorridor(Graphics2D g2) {
        Random r = new Random(seed);

        // --- Floor -----------------------------------------------------------
        g2.setColor(FLOOR_BASE);
        g2.fillRect(COR_LEFT, COR_TOP, COR_WIDTH, COR_BOTTOM);

        // Floor tile grid
        g2.setColor(FLOOR_TILE);
        g2.setStroke(new BasicStroke(1f));
        for (int y = COR_TOP; y < COR_BOTTOM; y += 40) {
            g2.drawLine(COR_LEFT, y, COR_RIGHT, y);
        }
        for (int x = COR_LEFT; x <= COR_RIGHT; x += 40) {
            g2.drawLine(x, COR_TOP, x, COR_BOTTOM);
        }

        // Centre line (faded yellow dashes)
        g2.setColor(CENTERLINE);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT,
                     BasicStroke.JOIN_MITER, 10f, new float[]{20f, 30f}, 0f));
        int cx = (COR_LEFT + COR_RIGHT) / 2;
        g2.drawLine(cx, COR_TOP + 40, cx, COR_BOTTOM - 40);

        // Floor stains
        for (int i = 0; i < 30; i++) {
            int sx = COR_LEFT + r.nextInt(COR_WIDTH);
            int sy = r.nextInt(COR_BOTTOM);
            int ss = 3 + r.nextInt(8);
            g2.setColor(new Color(30 + r.nextInt(20), 28 + r.nextInt(15),
                                  25 + r.nextInt(10), 30 + r.nextInt(30)));
            g2.fillOval(sx, sy, ss, ss);
        }

        // --- Walls -----------------------------------------------------------
        // Left wall
        g2.setColor(WALL_BASE);
        g2.fillRect(COR_LEFT - WALL_THICK, COR_TOP, WALL_THICK, COR_BOTTOM);
        // Right wall
        g2.fillRect(COR_RIGHT, COR_TOP, WALL_THICK, COR_BOTTOM);

        // Wall inner edge highlights
        g2.setColor(WALL_DARK);
        g2.fillRect(COR_LEFT - 3, COR_TOP, 3, COR_BOTTOM);
        g2.fillRect(COR_RIGHT, COR_TOP, 3, COR_BOTTOM);

        // Wall panel lines
        g2.setColor(new Color(42, 39, 35));
        g2.setStroke(new BasicStroke(1f));
        for (int y = COR_TOP; y < COR_BOTTOM; y += 120) {
            g2.drawLine(COR_LEFT - WALL_THICK, y, COR_LEFT, y);
            g2.drawLine(COR_RIGHT, y, COR_RIGHT + WALL_THICK, y);
        }

        // Emergency lights along walls
        for (int y = 100; y < COR_BOTTOM; y += 200) {
            drawEmergencyLight(g2, COR_LEFT - 20, y);
            drawEmergencyLight(g2, COR_RIGHT + 12, y);
        }

        // Pipes along ceiling (top of wall)
        g2.setColor(new Color(60, 56, 50));
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(COR_LEFT - WALL_THICK + 8, COR_TOP, COR_LEFT - WALL_THICK + 8, COR_BOTTOM);
        g2.drawLine(COR_RIGHT + WALL_THICK - 8, COR_TOP, COR_RIGHT + WALL_THICK - 8, COR_BOTTOM);

        // --- Doors -----------------------------------------------------------
        for (int i = 0; i < doorCount; i++) {
            drawDoor(g2, i);
        }

        // --- Entrance label at bottom ----------------------------------------
        g2.setFont(new Font("Courier New", Font.BOLD, 14));
        g2.setColor(new Color(160, 150, 130, 160));
        String entrance = "CONTAINMENT WING B";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(entrance, cx - fm.stringWidth(entrance) / 2, COR_BOTTOM - 100);

        g2.setFont(new Font("Courier New", Font.PLAIN, 11));
        g2.setColor(new Color(120, 110, 95, 120));
        String sub = "AUTHORIZED PERSONNEL ONLY";
        fm = g2.getFontMetrics();
        g2.drawString(sub, cx - fm.stringWidth(sub) / 2, COR_BOTTOM - 80);

        // Top dead end
        g2.setColor(WALL_BASE);
        g2.fillRect(COR_LEFT - WALL_THICK, COR_TOP, COR_WIDTH + WALL_THICK * 2, 30);
        g2.setColor(WALL_DARK);
        g2.fillRect(COR_LEFT - WALL_THICK, 28, COR_WIDTH + WALL_THICK * 2, 3);

        // --- Master Console ------------------------------------------------------
        drawMasterConsole(g2);
    }

    private void drawDoor(Graphics2D g2, int i) {
        int dx = doorX[i], dy = doorY[i];
        boolean locked = doorLocked[i] && !hasAccessKey;
        boolean nearby = (i == nearDoorIdx);

        // Hazard stripes on floor near door
        g2.setColor(HAZARD_YELLOW);
        int hz = doorLeft[i] ? COR_LEFT : COR_RIGHT - 35;
        g2.fillRect(hz, dy - 5, 35, DOOR_H + 10);

        // Door frame
        g2.setColor(DOOR_FRAME);
        g2.fillRect(dx - 3, dy - 3, DOOR_W + 6, DOOR_H + 6);

        // Door body
        g2.setColor(nearby ? DOOR_FILL.brighter() : DOOR_FILL);
        g2.fillRect(dx, dy, DOOR_W, DOOR_H);

        // Door panel lines
        g2.setColor(new Color(58, 54, 48));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(dx + 5, dy + 5, DOOR_W - 10, DOOR_H / 2 - 8);
        g2.drawRect(dx + 5, dy + DOOR_H / 2 + 3, DOOR_W - 10, DOOR_H / 2 - 8);

        // Handle
        g2.setColor(new Color(100, 95, 85));
        int hx = doorLeft[i] ? dx + DOOR_W - 12 : dx + 5;
        g2.fillOval(hx, dy + DOOR_H / 2 - 3, 7, 7);

        // Keycard reader
        int kx = doorLeft[i] ? dx + DOOR_W + 6 : dx - 16;
        g2.setColor(new Color(30, 28, 25));
        g2.fillRect(kx, dy + 15, 12, 18);
        // Status LED
        Color led = locked ? LOCKED_COLOR : UNLOCK_COLOR;
        g2.setColor(nearby ? led.brighter() : led);
        g2.fillOval(kx + 3, dy + 18, 6, 6);

        // Label above door
        g2.setFont(new Font("Courier New", Font.BOLD, 13));
        g2.setColor(LABEL_COLOR);
        FontMetrics fm = g2.getFontMetrics();
        int lx = dx + (DOOR_W - fm.stringWidth(doorLabel[i])) / 2;
        g2.drawString(doorLabel[i], lx, dy - 10);

        // Lock status text
        g2.setFont(new Font("Courier New", Font.PLAIN, 9));
        g2.setColor(locked ? new Color(180, 50, 40, 140) : new Color(50, 180, 60, 140));
        String status = locked ? "LOCKED" : "OPEN";
        fm = g2.getFontMetrics();
        g2.drawString(status, dx + (DOOR_W - fm.stringWidth(status)) / 2, dy + DOOR_H + 16);
    }

    private void drawEmergencyLight(Graphics2D g2, int x, int y) {
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.003 + y * 0.01);
        int alpha = (int)(60 + 60 * pulse);
        g2.setColor(new Color(200, 50, 30, alpha));
        g2.fillOval(x, y, 8, 8);
        // Glow
        g2.setColor(new Color(200, 50, 30, alpha / 4));
        g2.fillOval(x - 6, y - 6, 20, 20);
    }

    // --- Master Console drawing -----------------------------------------------

    private void drawMasterConsole(Graphics2D g2) {
        int mx = CONSOLE_X, my = CONSOLE_Y;

        // Ambient glow on floor around console
        double gPulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.003);
        int glowAlpha = (int)(20 + 25 * gPulse);
        g2.setColor(new Color(60, 200, 255, glowAlpha));
        g2.fillOval(mx - 20, my - 15, CONSOLE_W + 40, CONSOLE_H + 30);

        // Console base (dark metal casing)
        g2.setColor(new Color(28, 28, 32));
        g2.fillRoundRect(mx - 4, my - 4, CONSOLE_W + 8, CONSOLE_H + 8, 6, 6);
        g2.setColor(new Color(50, 50, 55));
        g2.fillRoundRect(mx, my, CONSOLE_W, CONSOLE_H, 4, 4);

        // Screen area
        int sx = mx + 5, sy = my + 5, sw = CONSOLE_W - 10, sh = CONSOLE_H - 20;
        g2.setColor(new Color(8, 20, 15));
        g2.fillRect(sx, sy, sw, sh);

        // CRT scan lines
        g2.setColor(new Color(30, 80, 60, 25));
        for (int yy = sy; yy < sy + sh; yy += 2) {
            g2.drawLine(sx, yy, sx + sw, yy);
        }

        // Screen content glow
        double sPulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.004);
        Color screenGreen = new Color(40, 220, 160, (int)(80 + 60 * sPulse));
        g2.setColor(screenGreen);
        g2.setFont(new Font("Courier New", Font.BOLD, 7));
        g2.drawString("MASTER", sx + 5, sy + 12);
        g2.drawString("CONSOLE", sx + 3, sy + 21);

        // Blinking cursor
        if (System.currentTimeMillis() % 1000 < 500) {
            g2.fillRect(sx + 4, sy + 24, 4, 2);
        }

        // Screen border (subtle glow)
        g2.setColor(new Color(40, 200, 160, (int)(40 + 30 * sPulse)));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(sx, sy, sw, sh);

        // Chip slot at bottom of console
        int slotX = mx + CONSOLE_W / 2 - 10, slotY = my + CONSOLE_H - 12;
        g2.setColor(new Color(15, 15, 18));
        g2.fillRect(slotX, slotY, 20, 8);
        g2.setColor(new Color(60, 180, 200, (int)(60 + 40 * sPulse)));
        g2.drawRect(slotX, slotY, 20, 8);

        // Small status LEDs
        for (int i = 0; i < 3; i++) {
            int lx = mx + 8 + i * 8;
            int ly = my + CONSOLE_H - 3;
            g2.setColor(new Color(40, 200, 160, (int)(80 + 80 * Math.sin(
                System.currentTimeMillis() * 0.005 + i * 1.2))));
            g2.fillOval(lx, ly, 3, 3);
        }

        // Highlight when nearby
        if (nearConsole) {
            g2.setColor(new Color(60, 220, 255, (int)(30 + 25 * gPulse)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(mx - 6, my - 6, CONSOLE_W + 12, CONSOLE_H + 12, 8, 8);
        }
    }

    // --- Screen-space prompt drawing -----------------------------------------

    public void drawPrompt(Graphics2D g2, int viewW, int viewH, int chipsCollected) {
        // Console prompt takes priority
        if (nearConsole) {
            drawConsolePrompt(g2, viewW, viewH, chipsCollected);
            return;
        }

        if (nearDoorIdx < 0) return;

        boolean locked = doorLocked[nearDoorIdx] && !hasAccessKey;
        String msg;
        Color col;
        if (locked) {
            msg = "LOCKED  -  ACCESS KEY REQUIRED";
            col = new Color(200, 60, 50);
        } else {
            msg = "Press [E] to enter " + doorLabel[nearDoorIdx];
            col = new Color(60, 200, 80);
        }

        drawPromptPill(g2, viewW, viewH, msg, col);
    }

    private void drawConsolePrompt(Graphics2D g2, int viewW, int viewH, int chipsCollected) {
        String msg;
        Color col;
        if (chipsCollected >= 5) {
            msg = "Press [E] to SYNTHESIZE MASTER KEY";
            col = new Color(60, 220, 255);
        } else {
            msg = "INSUFFICIENT DATA  [" + chipsCollected + "/5 chips]";
            col = new Color(200, 60, 50);
        }
        drawPromptPill(g2, viewW, viewH, msg, col);
    }

    private void drawPromptPill(Graphics2D g2, int viewW, int viewH, String msg, Color col) {
        Font f = new Font("Consolas", Font.BOLD, 14);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(msg);
        int px = (viewW - tw) / 2 - 12;
        int py = viewH - 80;

        // Background pill
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(px, py - 18, tw + 24, 30, 10, 10);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 60));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(px, py - 18, tw + 24, 30, 10, 10);

        // Text
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.005);
        int alpha = (int)(180 + 70 * pulse);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha));
        g2.drawString(msg, px + 12, py + 2);
    }

    public void drawHallwayHud(Graphics2D g2, int viewW, int viewH) {
        g2.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2.setColor(new Color(150, 140, 130));
        g2.drawString("WASD - move  |  E - interact  |  ACCESS KEY: "
                     + (hasAccessKey ? "YES" : "NONE"), 10, viewH - 12);

        // Location header
        g2.setFont(new Font("Consolas", Font.BOLD, 12));
        g2.setColor(new Color(180, 170, 155));
        g2.drawString("SITE-19  //  CONTAINMENT WING B", 10, 20);
    }

    // --- Escape sequence overlay drawing --------------------------------------

    public void drawEscapeSequence(Graphics2D g2, int viewW, int viewH) {
        if (!escapeTriggered) return;

        double t = escapeTimer;

        // Phase 1: 0–2s — Screen flash + "MASTER KEY SYNTHESIZED"
        if (t < 2.0) {
            double flashAlpha = Math.max(0, 1.0 - t / 0.5) * 255;
            g2.setColor(new Color(60, 220, 255, (int) flashAlpha));
            g2.fillRect(0, 0, viewW, viewH);

            double fadeIn = Math.min(1.0, t / 0.8);
            g2.setFont(new Font("Courier New", Font.BOLD, 28));
            String line = "MASTER KEY SYNTHESIZED";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(line);
            g2.setColor(new Color(60, 220, 255, (int)(255 * fadeIn)));
            g2.drawString(line, (viewW - tw) / 2, viewH / 2 - 20);

            g2.setFont(new Font("Courier New", Font.PLAIN, 14));
            String sub = "CONTAINMENT OVERRIDE INITIATED";
            fm = g2.getFontMetrics();
            g2.setColor(new Color(200, 200, 200, (int)(200 * fadeIn)));
            g2.drawString(sub, (viewW - fm.stringWidth(sub)) / 2, viewH / 2 + 15);
        }

        // Phase 2: 2–5s — Alarm sequence with scrolling messages
        if (t >= 2.0 && t < 5.0) {
            // Red flashing overlay
            double alarmPulse = 0.5 + 0.5 * Math.sin(t * 12);
            g2.setColor(new Color(200, 30, 20, (int)(40 + 60 * alarmPulse)));
            g2.fillRect(0, 0, viewW, viewH);

            g2.setFont(new Font("Courier New", Font.BOLD, 22));
            g2.setColor(new Color(220, 60, 40, (int)(180 + 70 * alarmPulse)));
            String warn = "!! CONTAINMENT BREACH !!";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(warn, (viewW - fm.stringWidth(warn)) / 2, viewH / 2 - 40);

            // Scrolling status lines
            g2.setFont(new Font("Courier New", Font.PLAIN, 12));
            g2.setColor(new Color(180, 180, 170, 200));
            String[] lines = {
                "> BLAST DOORS UNLOCKING...",
                "> SECURITY PROTOCOLS DISABLED",
                "> EMERGENCY EXIT: SECTOR 7-G",
                "> ALL PERSONNEL EVACUATE",
                "> SUBJECT ESCAPE IMMINENT"
            };
            double linePhase = (t - 2.0) / 3.0;
            int shown = Math.min(lines.length, (int)(linePhase * lines.length) + 1);
            for (int i = 0; i < shown; i++) {
                g2.drawString(lines[i], 80, viewH / 2 + i * 20);
            }
        }

        // Phase 3: 5–7s — Fade to black with final message
        if (t >= 5.0 && t < 7.0) {
            double fade = Math.min(1.0, (t - 5.0) / 1.5);
            g2.setColor(new Color(0, 0, 0, (int)(255 * fade)));
            g2.fillRect(0, 0, viewW, viewH);

            if (fade > 0.5) {
                double textFade = (fade - 0.5) / 0.5;
                g2.setFont(new Font("Courier New", Font.BOLD, 36));
                String escaped = "YOU ESCAPED.";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(escaped);
                g2.setColor(new Color(60, 220, 200, (int)(255 * textFade)));
                g2.drawString(escaped, (viewW - tw) / 2, viewH / 2 - 10);
            }
        }

        // Phase 4: 7s+ — Hold final screen
        if (t >= 7.0) {
            g2.setColor(new Color(0, 0, 0, 255));
            g2.fillRect(0, 0, viewW, viewH);

            g2.setFont(new Font("Courier New", Font.BOLD, 36));
            String escaped = "YOU ESCAPED.";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(escaped);
            g2.setColor(new Color(60, 220, 200));
            g2.drawString(escaped, (viewW - tw) / 2, viewH / 2 - 10);

            // Sub text
            g2.setFont(new Font("Courier New", Font.PLAIN, 14));
            String sub = "SCP Foundation  //  File Closed";
            fm = g2.getFontMetrics();
            g2.setColor(new Color(120, 120, 110));
            g2.drawString(sub, (viewW - fm.stringWidth(sub)) / 2, viewH / 2 + 25);

            // Return prompt
            double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.004);
            g2.setFont(new Font("Courier New", Font.BOLD, 16));
            String ret = "Press ENTER to return to Title";
            fm = g2.getFontMetrics();
            g2.setColor(new Color(60, 200, 200, (int)(120 + 120 * pulse)));
            g2.drawString(ret, (viewW - fm.stringWidth(ret)) / 2, viewH / 2 + 70);
        }
    }
}
