import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

/**
 * Core rendering surface and game-loop host.
 *
 * Game states: TITLE -> HALLWAY -> PLAYING (cell) -> DEAD -> HALLWAY / TITLE.
 */
public class GamePanel extends JPanel implements Runnable {

    // --- Constants -----------------------------------------------------------
    private static final int    TARGET_FPS   = 60;
    private static final long   FRAME_TIME   = 1_000_000_000L / TARGET_FPS;
    private static final double DT           = 1.0 / TARGET_FPS;
    private static final Color  BG_COLOR     = new Color(30, 30, 30);
    private static final Color  GRID_COLOR   = new Color(45, 45, 45);
    private static final int    GRID_SPACING = 40;

    // --- HUD -----------------------------------------------------------------
    private static final Color HUD_TEXT = new Color(180, 180, 180);
    private static final Color HUD_WARN = new Color(220, 80, 60);
    private static final Font  HUD_FONT = new Font("Consolas", Font.PLAIN, 13);
    private static final Font  HUD_BIG  = new Font("Consolas", Font.BOLD,  14);

    // --- Game state ----------------------------------------------------------
    private enum GameState { TITLE, HALLWAY, PLAYING, DEAD, ESCAPED }
    private GameState state = GameState.TITLE;
    private DeathScreen deathScreen;
    private double titlePulse = 0;
    private String currentCellLabel = "SCP-001";

    // --- Core objects --------------------------------------------------------
    private final int               panelWidth, panelHeight;
    private final Player            player;
    private final RoomManager       roomManager;
    private final InteractionSystem interaction;
    private final Flashlight        flashlight;
    private final SanitySystem      sanity;
    private final HallwayManager    hallway;
    private final InventorySystem   inventory;
    private ChipFragment            cellFragment;  // current cell's fragment (or null)
    private ShyGuyBehavior          shyGuy;        // non-null only when in SCP-002

    private Thread  gameThread;
    private boolean running;
    private int     currentFps;
    private int     mouseX, mouseY;

    // --- Particles -----------------------------------------------------------
    private static final int PARTICLE_COUNT = 60;
    private final double[] pX = new double[PARTICLE_COUNT];
    private final double[] pY = new double[PARTICLE_COUNT];
    private final double[] pVx = new double[PARTICLE_COUNT];
    private final double[] pVy = new double[PARTICLE_COUNT];
    private final int[]    pAlpha = new int[PARTICLE_COUNT];
    private final int[]    pSize  = new int[PARTICLE_COUNT];

    // =========================================================================
    // Construction
    // =========================================================================

    public GamePanel(int w, int h) {
        panelWidth  = w;
        panelHeight = h;
        setPreferredSize(new Dimension(w, h));
        setBackground(BG_COLOR);
        setDoubleBuffered(true);
        setFocusable(true);

        player      = new Player(w / 2.0, h / 2.0);
        roomManager = new RoomManager();
        interaction = new InteractionSystem();
        flashlight  = new Flashlight(w, h);
        sanity      = new SanitySystem();
        hallway     = new HallwayManager();
        inventory   = new InventorySystem();

        initParticles();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { mouseX = e.getX(); mouseY = e.getY(); }
            @Override public void mouseDragged(MouseEvent e)  {
                mouseX = e.getX(); mouseY = e.getY();
                if (state == GameState.PLAYING) interaction.handleMouseDragged(e.getX(), e.getY());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (state == GameState.PLAYING) interaction.handleMousePressed(e.getX(), e.getY());
                if (state == GameState.DEAD && deathScreen != null) {
                    DeathScreen.Action a = deathScreen.handleClick(e.getX(), e.getY());
                    if (a == DeathScreen.Action.RETRY) {
                        resetCell();
                        player.reset(hallway.getSavedPlayerX(), hallway.getSavedPlayerY());
                        state = GameState.HALLWAY;
                    }
                    if (a == DeathScreen.Action.MAIN_MENU) {
                        resetCell();
                        state = GameState.TITLE;
                    }
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (state == GameState.PLAYING) interaction.handleMouseReleased();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e)  { handleKey(e.getKeyCode(), true); }
            @Override public void keyReleased(KeyEvent e) { handleKey(e.getKeyCode(), false); }
        });
    }

    // =========================================================================
    // Game loop
    // =========================================================================

    public void startGameLoop() {
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        int frames = 0;
        long fpsTimer = System.currentTimeMillis();

        while (running) {
            long now = System.nanoTime();
            if (now - lastTime >= FRAME_TIME) {
                lastTime = now;
                update();
                repaint();
                frames++;
            } else {
                try {
                    long ms = (FRAME_TIME - (now - lastTime)) / 1_000_000;
                    if (ms > 0) Thread.sleep(ms);
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                currentFps = frames; frames = 0;
                fpsTimer = System.currentTimeMillis();
            }
        }
    }

    // =========================================================================
    // Update
    // =========================================================================

    private void update() {
        switch (state) {
            case TITLE:
                titlePulse += DT;
                updateParticles();
                break;

            case HALLWAY:
                // Mouse position in world space for player rotation
                player.update(mouseX, (int)(mouseY + hallway.getCameraY()));
                player.clampToRect(HallwayManager.COR_LEFT, HallwayManager.COR_TOP + 50,
                                   HallwayManager.COR_RIGHT, HallwayManager.COR_BOTTOM - 50);
                hallway.update(player.getX(), player.getY(), panelHeight);
                flashlight.update(DT);
                updateParticles();
                break;

            case PLAYING:
                player.update(mouseX, mouseY);
                player.clamp(panelWidth, panelHeight);
                updateParticles();
                roomManager.update(DT, player.getX(), player.getY(), player.getAngle(), mouseX, mouseY);
                interaction.update(player.getX(), player.getY(), roomManager.getCurrentObjects());
                flashlight.update(DT);
                sanity.update(DT);
                inventory.update(DT);
                if (roomManager.consumeShift()) sanity.drainFromShift();
                // Fragment pickup
                if (cellFragment != null && !cellFragment.isCollected()) {
                    cellFragment.update();
                    if (cellFragment.checkPickup(player.getX(), player.getY())) {
                        inventory.pickUp(currentCellLabel);
                    }
                }
                if (sanity.getSanity() <= 0) triggerDeath("PSYCHOLOGICAL COLLAPSE");

                // SCP-002: Shy Guy update & kill detection
                if ("SCP-002".equals(currentCellLabel) && shyGuy != null) {
                    shyGuy.update(DT, mouseX, mouseY, player.getX(), player.getY());
                    if (shyGuy.hasKilledPlayer(player.getX(), player.getY(), 14)) {
                        triggerDeath("SCP-096 \u2014 YOU LOOKED");
                    }
                }
                break;

            case DEAD:
                if (deathScreen != null) deathScreen.update(mouseX, mouseY);
                break;

            case ESCAPED:
                hallway.update(player.getX(), player.getY(), panelHeight);
                break;
        }
    }

    private void triggerDeath(String cause) {
        state = GameState.DEAD;
        deathScreen = new DeathScreen(cause, panelWidth, panelHeight);
        inventory.dropCarrying();  // lose the carried fragment
    }

    private void resetCell() {
        roomManager.reset();
        interaction.reset();
        sanity.reset();
        deathScreen  = null;
        cellFragment = null;
        shyGuy       = null;
    }

    // =========================================================================
    // Rendering
    // =========================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (state) {
            case TITLE:   drawTitleScreen(g2); break;
            case HALLWAY: drawHallwayState(g2); break;
            case PLAYING: drawPlayingState(g2); break;
            case DEAD:    if (deathScreen != null) deathScreen.draw(g2, panelWidth, panelHeight); break;
            case ESCAPED: drawEscapedState(g2); break;
        }
    }

    // --- Title screen --------------------------------------------------------

    private void drawTitleScreen(Graphics2D g2) {
        g2.setColor(new Color(12, 12, 14));
        g2.fillRect(0, 0, panelWidth, panelHeight);
        drawParticles(g2);

        g2.setColor(new Color(0, 0, 0, 15));
        for (int y = 0; y < panelHeight; y += 3) g2.drawLine(0, y, panelWidth, y);

        float r = panelWidth * 0.6f;
        g2.setPaint(new RadialGradientPaint(panelWidth / 2f, panelHeight / 2f, r,
            new float[]{0f, 0.5f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,100), new Color(0,0,0,230)}));
        g2.fillRect(0, 0, panelWidth, panelHeight);

        g2.setFont(new Font("Courier New", Font.BOLD, 42));
        String t1 = "SCP  FOUNDATION";
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(t1);
        int tx = (panelWidth - tw) / 2;
        g2.setColor(new Color(200, 30, 20, 80));
        g2.drawString(t1, tx - 2, 230);
        g2.setColor(new Color(30, 180, 200, 80));
        g2.drawString(t1, tx + 2, 231);
        g2.setColor(new Color(220, 215, 200));
        g2.drawString(t1, tx, 230);

        g2.setFont(new Font("Courier New", Font.PLAIN, 16));
        String t2 = "CONTAINMENT  PROTOCOL";
        fm = g2.getFontMetrics();
        g2.setColor(new Color(140, 130, 110));
        g2.drawString(t2, (panelWidth - fm.stringWidth(t2)) / 2, 270);

        g2.setColor(new Color(80, 70, 55));
        g2.drawLine(panelWidth / 2 - 120, 290, panelWidth / 2 + 120, 290);

        g2.setFont(new Font("Courier New", Font.PLAIN, 11));
        g2.setColor(new Color(100, 90, 75));
        String cl = "CLEARANCE LEVEL 3 REQUIRED";
        fm = g2.getFontMetrics();
        g2.drawString(cl, (panelWidth - fm.stringWidth(cl)) / 2, 310);

        double pulse = 0.5 + 0.5 * Math.sin(titlePulse * 3);
        int alpha = (int)(120 + 120 * pulse);
        g2.setFont(new Font("Courier New", Font.BOLD, 18));
        String prompt = "PRESS  ENTER  TO  BEGIN";
        fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 60, 50, alpha));
        g2.drawString(prompt, (panelWidth - fm.stringWidth(prompt)) / 2, 400);

        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(80, 75, 65));
        g2.drawString("SECURE . CONTAIN . PROTECT", 10, panelHeight - 15);
        g2.drawString("v1.0  //  SITE-19", panelWidth - 140, panelHeight - 15);
    }

    // --- Hallway state -------------------------------------------------------

    private void drawHallwayState(Graphics2D g2) {
        // Dark background (extends beyond corridor)
        g2.setColor(new Color(15, 14, 12));
        g2.fillRect(0, 0, panelWidth, panelHeight);

        // Apply camera transform
        AffineTransform original = g2.getTransform();
        double camY = hallway.getCameraY();
        g2.translate(0, -camY);

        // Draw corridor world
        hallway.drawCorridor(g2);

        // Draw player (world space)
        player.draw(g2);

        // Reset camera
        g2.setTransform(original);

        // Screen-space overlays
        double playerScreenY = player.getY() - camY;
        flashlight.draw(g2, player.getX(), playerScreenY, mouseX, mouseY, 1.0);

        // Screen-space vignette (skip during escape for cleaner overlays)
        if (!hallway.isEscapeTriggered()) {
            float vr = Math.max(panelWidth, panelHeight) * 0.55f;
            g2.setPaint(new RadialGradientPaint(panelWidth / 2f, panelHeight / 2f, vr,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(0,0,0,0), new Color(0,0,0,60), new Color(0,0,0,200)}));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        // HUD
        hallway.drawPrompt(g2, panelWidth, panelHeight, inventory.getCollectedCount());
        hallway.drawHallwayHud(g2, panelWidth, panelHeight);
        inventory.drawCounter(g2, panelWidth);

        // Escape sequence overlay (drawn on top of everything)
        hallway.drawEscapeSequence(g2, panelWidth, panelHeight);
    }

    // --- Escaped state (final screen after escape completes) ------------------

    private void drawEscapedState(Graphics2D g2) {
        // Let the hallway draw its escape cinematic
        hallway.drawEscapeSequence(g2, panelWidth, panelHeight);
    }

    // --- Playing state -------------------------------------------------------

    private void drawPlayingState(Graphics2D g2) {
        AffineTransform original = g2.getTransform();
        g2.translate(sanity.getShakeX(), sanity.getShakeY());

        drawBackground(g2);
        drawParticles(g2);

        // SCP-096 rage overlay goes behind the room geometry
        if ("SCP-002".equals(currentCellLabel) && shyGuy != null) {
            shyGuy.drawRageOverlay(g2, panelWidth, panelHeight);
        }

        roomManager.drawRoom(g2);
        roomManager.drawAnchor(g2);
        if (cellFragment != null) cellFragment.draw(g2);

        // Draw SCP-096 entity on top of furniture
        if ("SCP-002".equals(currentCellLabel) && shyGuy != null) {
            shyGuy.draw(g2);
        }
        player.draw(g2);
        drawVignette(g2);
        flashlight.draw(g2, player.getX(), player.getY(), mouseX, mouseY, sanity.getSanityFactor());
        interaction.drawPrompt(g2);
        interaction.drawLog(g2);
        roomManager.drawBlinkOverlay(g2, panelWidth, panelHeight);
        roomManager.drawFlashMessage(g2, panelWidth, panelHeight);
        sanity.drawDistortion(g2, panelWidth, panelHeight);

        g2.setTransform(original);
        drawHud(g2);
        sanity.drawMeter(g2, panelWidth);
        inventory.drawCounter(g2, panelWidth);
        inventory.drawPickupMessage(g2, panelWidth, panelHeight);
    }

    // --- Background ----------------------------------------------------------

    private void drawBackground(Graphics2D g2) {
        g2.setColor(BG_COLOR);
        g2.fillRect(-15, -15, panelWidth + 30, panelHeight + 30);
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(1f));
        for (int x = 0; x < panelWidth; x += GRID_SPACING) g2.drawLine(x, 0, x, panelHeight);
        for (int y = 0; y < panelHeight; y += GRID_SPACING) g2.drawLine(0, y, panelWidth, y);
    }

    private void drawVignette(Graphics2D g2) {
        int cx = panelWidth / 2, cy = panelHeight / 2;
        float sf = (float) sanity.getSanityFactor();
        float radius = Math.max(panelWidth, panelHeight) * (0.45f + 0.35f * sf);
        g2.setPaint(new RadialGradientPaint(cx, cy, radius,
            new float[]{0f, 0.4f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(0,0,0,(int)(60+60*(1-sf))),
                        new Color(0,0,0,(int)(180+60*(1-sf)))}));
        g2.fillRect(-15, -15, panelWidth + 30, panelHeight + 30);
    }

    // --- HUD -----------------------------------------------------------------

    private void drawHud(Graphics2D g2) {
        g2.setFont(HUD_FONT); g2.setColor(HUD_TEXT);
        g2.drawString("CELL: " + currentCellLabel + "  |  FPS: " + currentFps, 10, 20);
        g2.drawString("Layout: " + (roomManager.getCurrentLayoutIndex() + 1)
                     + "/3   Shifts: " + roomManager.getSwapCount(), 10, 38);
        double cd = roomManager.getBlinkCountdown();
        if (cd > 0 && cd < 1.5) {
            g2.setFont(HUD_BIG); g2.setColor(HUD_WARN);
            g2.drawString(String.format("BLINK IN %.1fs", cd), 10, 58);
        }
        drawStareIndicator(g2);
        g2.setFont(HUD_FONT); g2.setColor(HUD_TEXT);
        g2.drawString("WASD - move  |  Hover EYE to focus  |  E - desk  |  ESC - exit cell", 10, panelHeight - 12);
    }

    private void drawStareIndicator(Graphics2D g2) {
        int ix = panelWidth - 60, iy = 25;
        boolean s = roomManager.isPlayerStaring();
        g2.setColor(s ? new Color(0,60,0,160) : new Color(60,0,0,160));
        g2.fillRoundRect(ix-8, iy-12, 56, 24, 12, 12);
        int ew = 16, eh = s ? 10 : 4;
        g2.setColor(s ? new Color(0,220,80) : new Color(200,60,50));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(ix, iy-eh/2, ew, eh, 0, 180);
        g2.drawArc(ix, iy-eh/2, ew, eh, 180, 180);
        if (s) g2.fillOval(ix+ew/2-3, iy-3, 6, 6);
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.drawString(s ? "FOCUS" : "LOST", ix+20, iy+4);
    }

    // --- Input ---------------------------------------------------------------

    private void handleKey(int keyCode, boolean pressed) {
        if (!pressed) {
            if (state == GameState.PLAYING || state == GameState.HALLWAY) {
                switch (keyCode) {
                    case KeyEvent.VK_W: case KeyEvent.VK_UP:    player.setMovingUp(false);    break;
                    case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  player.setMovingDown(false);  break;
                    case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  player.setMovingLeft(false);  break;
                    case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: player.setMovingRight(false); break;
                }
            }
            return;
        }

        switch (state) {
            case TITLE:
                if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
                    player.reset(400, 1900);
                    state = GameState.HALLWAY;
                }
                break;

            case HALLWAY:
                switch (keyCode) {
                    case KeyEvent.VK_W: case KeyEvent.VK_UP:    player.setMovingUp(true);    break;
                    case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  player.setMovingDown(true);  break;
                    case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  player.setMovingLeft(true);  break;
                    case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: player.setMovingRight(true); break;
                    case KeyEvent.VK_E:
                        // --- Master Console interaction ---
                        if (hallway.isNearConsole()) {
                            if (inventory.getCollectedCount() >= 5) {
                                hallway.triggerEscape();
                                state = GameState.ESCAPED;
                            }
                            // else: console shows INSUFFICIENT DATA via prompt
                            break;
                        }
                        // --- Door interaction ---
                        String entered = hallway.tryEnterDoor(player.getX(), player.getY());
                        if (entered != null) {
                            currentCellLabel = entered;
                            player.reset(panelWidth / 2.0, panelHeight / 2.0);
                            resetCell();
                            interaction.setCell(currentCellLabel);
                            boolean isScp002 = "SCP-002".equals(currentCellLabel);
                            roomManager.setBlinkEnabled(!isScp002);
                            if (isScp002) shyGuy = new ShyGuyBehavior();
                            // Spawn fragment if not already collected for this cell
                            if (!inventory.isCollected(currentCellLabel)) {
                                cellFragment = new ChipFragment(currentCellLabel);
                            }
                            state = GameState.PLAYING;
                        }
                        break;
                    case KeyEvent.VK_ESCAPE:
                        state = GameState.TITLE;
                        break;
                }
                break;

            case ESCAPED:
                // After escape sequence finishes (7s), allow returning to title
                if (keyCode == KeyEvent.VK_ENTER && hallway.getEscapeTimer() >= 7.0) {
                    state = GameState.TITLE;
                    // Reset hallway for a fresh game
                    // (HallwayManager is reinstantiated via field — we'd need a reset
                    //  but for now just go to title)
                }
                break;

            case PLAYING:
                switch (keyCode) {
                    case KeyEvent.VK_W: case KeyEvent.VK_UP:    player.setMovingUp(true);    break;
                    case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  player.setMovingDown(true);  break;
                    case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  player.setMovingLeft(true);  break;
                    case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: player.setMovingRight(true); break;
                    case KeyEvent.VK_E: interaction.onInteractKey(panelWidth, panelHeight); break;
                    case KeyEvent.VK_ESCAPE:
                        if (interaction.isLogOpen()) {
                            interaction.onEscapeKey();
                        } else {
                            inventory.saveCarrying();  // permanently save the fragment
                            player.reset(hallway.getSavedPlayerX(), hallway.getSavedPlayerY());
                            state = GameState.HALLWAY;
                        }
                        break;
                }
                break;

            case DEAD:
                break;
        }
    }

    // --- Particles -----------------------------------------------------------

    private void initParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i);
            pX[i] = Math.random() * panelWidth;
            pY[i] = Math.random() * panelHeight;
        }
    }

    private void resetParticle(int i) {
        pX[i]     = Math.random() * panelWidth;
        pY[i]     = Math.random() * panelHeight;
        pVx[i]    = (Math.random() - 0.5) * 0.6;
        pVy[i]    = (Math.random() - 0.5) * 0.6;
        pAlpha[i] = 20 + (int)(Math.random() * 50);
        pSize[i]  = 1 + (int)(Math.random() * 3);
    }

    private void updateParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            pX[i] += pVx[i]; pY[i] += pVy[i];
            if (pX[i] < 0 || pX[i] > panelWidth || pY[i] < 0 || pY[i] > panelHeight)
                resetParticle(i);
        }
    }

    private void drawParticles(Graphics2D g2) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            g2.setColor(new Color(200, 200, 200, pAlpha[i]));
            g2.fillOval((int) pX[i], (int) pY[i], pSize[i], pSize[i]);
        }
    }
}
