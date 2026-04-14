import java.awt.*;
import java.util.Random;

/**
 * Manages the containment room's furniture layouts and the Blink mechanic.
 *
 * <ul>
 *   <li>3 predefined layouts (different positions for door, desk, terminal).</li>
 *   <li>An AnchorPoint that persists across all layouts.</li>
 *   <li>Blink cycle: every 4 s the screen fades to black for 0.2 s.
 *       If the player is NOT staring at the AnchorPoint while the screen
 *       is fully black, the layout is randomly swapped.</li>
 * </ul>
 */
public class RoomManager {

    // --- Blink timing (seconds) ----------------------------------------------
    private static final double BLINK_INTERVAL  = 4.0;   // seconds between blinks
    private static final double FADE_IN_TIME    = 0.15;  // seconds to go black
    private static final double BLACK_TIME      = 0.20;  // seconds fully black
    private static final double FADE_OUT_TIME   = 0.15;  // seconds to come back

    private enum BlinkState { OPEN, CLOSING, CLOSED, OPENING }
    private BlinkState blinkState = BlinkState.OPEN;
    private double     blinkTimer = 0;        // counts up to BLINK_INTERVAL
    private double     phaseTimer = 0;        // timer within current blink phase
    private boolean    swapDone   = false;    // ensure we only swap once per blink

    // --- Overlay alpha (0 = transparent, 255 = fully black) ------------------
    private int overlayAlpha = 0;

    // --- Layouts -------------------------------------------------------------
    private static final int LAYOUT_COUNT = 3;
    private final RoomObject[][] layouts = new RoomObject[LAYOUT_COUNT][];
    private int currentLayout = 0;
    private int swapCount = 0;  // how many times the room has shifted
    private boolean shiftOccurred = false;  // consumed by GamePanel each frame

    // --- Anchor (shared across all layouts) -----------------------------------
    private final AnchorPoint anchor;
    private boolean playerIsStaring = false;

    // --- Feedback text (flashes briefly after a swap) -------------------------
    private String flashMessage = null;
    private double flashTimer   = 0;
    private static final double FLASH_DURATION = 2.0;

    private final Random rng = new Random();

    public void reset() {
        currentLayout = 0; swapCount = 0; shiftOccurred = false;
        blinkState = BlinkState.OPEN; blinkTimer = 0; phaseTimer = 0;
        overlayAlpha = 0; swapDone = false;
        playerIsStaring = false; flashMessage = null;
    }

    // =========================================================================
    // Construction
    // =========================================================================

    public RoomManager() {
        // Anchor sits at the centre of the room in all layouts
        anchor = new AnchorPoint(400, 280);

        // Layout 0 ---- "Standard containment"
        layouts[0] = new RoomObject[] {
            new RoomObject(RoomObject.Type.DOOR,     30,  240),   // left wall
            new RoomObject(RoomObject.Type.DESK,     520, 400),   // right-of-centre
            new RoomObject(RoomObject.Type.TERMINAL, 640, 100),   // top-right corner
        };

        // Layout 1 ---- "Mirror"
        layouts[1] = new RoomObject[] {
            new RoomObject(RoomObject.Type.DOOR,     720, 240),   // right wall
            new RoomObject(RoomObject.Type.DESK,     160, 420),   // bottom-left
            new RoomObject(RoomObject.Type.TERMINAL, 100, 80),    // top-left corner
        };

        // Layout 2 ---- "North exit"
        layouts[2] = new RoomObject[] {
            new RoomObject(RoomObject.Type.DOOR,     375,  20),   // top wall
            new RoomObject(RoomObject.Type.DESK,     560, 440),   // bottom-right
            new RoomObject(RoomObject.Type.TERMINAL, 100, 300),   // mid-left
        };
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public RoomObject[] getCurrentObjects() { return layouts[currentLayout]; }
    public AnchorPoint  getAnchor()         { return anchor; }
    public int          getOverlayAlpha()   { return overlayAlpha; }
    public boolean      isPlayerStaring()   { return playerIsStaring; }
    public int          getSwapCount()      { return swapCount; }
    public int          getCurrentLayoutIndex() { return currentLayout; }

    /** Returns true exactly once after a layout shift. */
    public boolean consumeShift() {
        if (shiftOccurred) { shiftOccurred = false; return true; }
        return false;
    }

    /** Returns a flash message if one is active, or null. */
    public String getFlashMessage() { return flashMessage; }
    public double getFlashTimer()   { return flashTimer; }
    public double getFlashDuration(){ return FLASH_DURATION; }

    /** Returns the blink countdown (seconds until next blink). */
    public double getBlinkCountdown() {
        if (blinkState == BlinkState.OPEN) return BLINK_INTERVAL - blinkTimer;
        return 0;
    }

    // =========================================================================
    // Update (called once per frame, pass delta in seconds)
    // =========================================================================

    public void update(double dt, double playerX, double playerY, double playerAngle,
                       int mouseX, int mouseY) {
        // Update anchor wandering
        anchor.update();

        // Check cursor focus on anchor
        playerIsStaring = anchor.isFocusedBy(mouseX, mouseY);

        // Flash message countdown
        if (flashMessage != null) {
            flashTimer -= dt;
            if (flashTimer <= 0) flashMessage = null;
        }

        // Blink state machine
        switch (blinkState) {
            case OPEN:
                blinkTimer += dt;
                overlayAlpha = 0;
                if (blinkTimer >= BLINK_INTERVAL) {
                    blinkTimer = 0;
                    phaseTimer = 0;
                    swapDone   = false;
                    blinkState = BlinkState.CLOSING;
                }
                break;

            case CLOSING:
                phaseTimer += dt;
                overlayAlpha = clampAlpha((int)(255 * (phaseTimer / FADE_IN_TIME)));
                if (phaseTimer >= FADE_IN_TIME) {
                    phaseTimer = 0;
                    blinkState = BlinkState.CLOSED;
                }
                break;

            case CLOSED:
                phaseTimer += dt;
                overlayAlpha = 255;

                // Attempt a swap exactly once while fully black
                if (!swapDone) {
                    swapDone = true;
                    if (!playerIsStaring) {
                        swapLayout();
                    } else {
                        flashMessage = "You held your gaze...";
                        flashTimer   = FLASH_DURATION;
                    }
                }

                if (phaseTimer >= BLACK_TIME) {
                    phaseTimer = 0;
                    blinkState = BlinkState.OPENING;
                }
                break;

            case OPENING:
                phaseTimer += dt;
                overlayAlpha = clampAlpha(255 - (int)(255 * (phaseTimer / FADE_OUT_TIME)));
                if (phaseTimer >= FADE_OUT_TIME) {
                    phaseTimer = 0;
                    overlayAlpha = 0;
                    blinkState = BlinkState.OPEN;
                }
                break;
        }
    }

    // =========================================================================
    // Layout swap
    // =========================================================================

    private void swapLayout() {
        int next;
        do {
            next = rng.nextInt(LAYOUT_COUNT);
        } while (next == currentLayout);

        currentLayout = next;
        swapCount++;
        shiftOccurred = true;

        flashMessage = "Something changed...";
        flashTimer   = FLASH_DURATION;
    }

    // =========================================================================
    // Drawing helpers
    // =========================================================================

    /** Draw all furniture in the current layout. */
    public void drawRoom(Graphics2D g2) {
        for (RoomObject obj : layouts[currentLayout]) {
            obj.draw(g2);
        }
    }

    /** Draw the anchor point. */
    public void drawAnchor(Graphics2D g2) {
        anchor.draw(g2);
    }

    /** Draw the blink overlay (black rect with variable alpha). */
    public void drawBlinkOverlay(Graphics2D g2, int width, int height) {
        if (overlayAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.fillRect(0, 0, width, height);
        }
    }

    /** Draw the flash message (e.g. "Something changed..."). */
    public void drawFlashMessage(Graphics2D g2, int panelWidth, int panelHeight) {
        if (flashMessage == null) return;

        double fade = Math.min(1.0, flashTimer / 0.5);  // fade out in last 0.5s
        int alpha = (int)(200 * fade);

        g2.setFont(new Font("Consolas", Font.ITALIC, 16));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(flashMessage);

        g2.setColor(new Color(200, 60, 50, alpha));
        g2.drawString(flashMessage, (panelWidth - tw) / 2, panelHeight / 2 + 80);
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private static int clampAlpha(int a) {
        return Math.max(0, Math.min(255, a));
    }
}
