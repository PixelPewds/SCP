import javax.swing.*;

/**
 * Entry point for the 2D indie horror game.
 * Creates an 800x600 JFrame with a dark grey background.
 */
public class Main {

    public static final int WINDOW_WIDTH  = 800;
    public static final int WINDOW_HEIGHT = 600;
    public static final String TITLE = "SCP - Containment";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(TITLE);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel gamePanel = new GamePanel(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.add(gamePanel);
            frame.pack();

            frame.setLocationRelativeTo(null);   // centre on screen
            frame.setVisible(true);

            // Start the game loop on a dedicated thread
            gamePanel.startGameLoop();
        });
    }
}
