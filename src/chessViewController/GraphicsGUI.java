package chessViewController;

import chessModel.Board;
import chessModel.Game;
import chessModel.Player;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

@SuppressWarnings("serial")
public class GraphicsGUI extends JFrame {

    private final JMenuItem saveItem;
    private final JMenuItem detailsItem;
    private final JLabel timer1Label = new JLabel("", SwingConstants.CENTER);
    private final JLabel timer2Label = new JLabel("", SwingConstants.CENTER);
    private final JLabel player1Score = new JLabel("", SwingConstants.CENTER);
    private final JLabel player2Score = new JLabel("", SwingConstants.CENTER);
    private final ChessView chessView;
    private final Timer updateTimer;
    private final Game game;
    private boolean coverageShown = false;

    public GraphicsGUI(int gameMode, Player player1, Player player2) {
        super("Chess");

        setMinimumSize(new Dimension(300, 300));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        saveItem = new JMenuItem("Save...");
        detailsItem = new JMenuItem("View...");
        fileMenu.add(saveItem);
        fileMenu.add(detailsItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Game + view
        game = new Game(gameMode, player1, player2);
        Board board = game.getBoard();
        chessView = new ChessView(board);

        setupHumanPlayers(player1, player2);

        // Timer & score panel
        JPanel timerScorePanel = new JPanel(new GridLayout(1, 4));
        timerScorePanel.add(timer1Label);
        timerScorePanel.add(player1Score);
        timerScorePanel.add(player2Score);
        timerScorePanel.add(timer2Label);

        add(timerScorePanel, BorderLayout.SOUTH);
        add(chessView, BorderLayout.CENTER);

        // UI updater ms
        updateTimer = new Timer(200, e -> updateUI());
        updateTimer.start();

        setupActions(board);

        pack();
        centerWindow();
        setVisible(true);
    }

    private void setupHumanPlayers(Player p1, Player p2) {
        if (p1 instanceof HumanPlayer hp1) {
            hp1.setupView(this, chessView);
        }
        if (p2 instanceof HumanPlayer hp2) {
            hp2.setupView(this, chessView);
        }
    }

    private void setupActions(Board board) {
        saveItem.addActionListener(e -> saveGame(board));
        detailsItem.addActionListener(e -> showDetails(board));
    }

    private void updateUI() {
        // clocks
        if (game.getCurrentSide() == 0) {
            timer1Label.setText(htmlLabel("P1 Time", true, game.getPlayer1Time()));
            timer2Label.setText(htmlLabel("P2 Time", false, game.getPlayer2Time()));
        } else {
            timer1Label.setText(htmlLabel("P1 Time", false, game.getPlayer1Time()));
            timer2Label.setText(htmlLabel("P2 Time", true, game.getPlayer2Time()));
        }
        // scores
        player1Score.setText("Score: " + game.getPlayer1Score());
        player2Score.setText("Score: " + game.getPlayer2Score());

        // end-of-game highlighting, one time only
        if (!coverageShown && gameOver()) {
            coverageShown = true;
            updateTimer.stop();
            highlightWinnerCoverage();
        }

        chessView.repaint();
    }

    private boolean gameOver() {
        // if the engine doesn’t think it’s over yet, do nothing
        if (!game.isGameOver()) {
            return false;
        }

        // once it is, show exactly the human‐readable reason
        popup(game.getEndReason());
        return true;
    }

    private void highlightWinnerCoverage() {
        Board board = game.getBoard();
        int side = game.getWinner();           // highlight the *winner*
        List<Point> squares = new ArrayList<>();

        // board.getAllMoves(side) returns [fromX,fromY,toX,toY] for every legal move
        for (Integer[] mv : board.getAllMoves(side)) {
            if (mv != null && mv.length >= 4) {
                int toX = mv[2], toY = mv[3];
                // toY = column, toX = row in ChessView coordinates
                squares.add(new Point(toY, toX));
            }
        }

        chessView.setHighlightSquares(squares);
    }

    private void saveGame(Board board) {
        JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
        chooser.setFileFilter(new FileNameExtensionFilter("Text File", "txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile())) {
                pw.write(board.getPGN());
            } catch (FileNotFoundException e) {
                popup("Unable to write file");
            }
        }
    }

    private void showDetails(Board board) {
        JPanel p = new JPanel(new GridLayout(3, 1, 5, 5));
        p.add(buildScrollPanel("PGN", board.getPGN()));
        p.add(buildScrollPanel("FEN", board.getFEN()));
        p.add(buildScrollPanel("Raw Moves", board.getMoveLog().toString()));
        JOptionPane.showMessageDialog(this, p, "Game Details", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel buildScrollPanel(String title, String content) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(title);
        JTextArea area = new JTextArea(content);
        area.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(label, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    public Integer[] handleLocationClicked(int xLoc, int yLoc) {
        if (chessView.getSelected() == null) {
            chessView.setSelected(game.getBoard().getPiece(xLoc, yLoc));
            chessView.repaint();
            return null;
        } else {
            Integer[] move = {
                chessView.getSelected().getX(),
                chessView.getSelected().getY(),
                xLoc, yLoc
            };
            chessView.setSelected(null);
            chessView.repaint();
            return move;
        }
    }

    public int getCurrentSide() {
        return game.getCurrentSide();
    }

    public static void popup(String message) {
        JOptionPane.showMessageDialog(null, message, "", JOptionPane.PLAIN_MESSAGE);
    }

    private void centerWindow() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2,
                (screen.height - getHeight()) / 2);
    }

    private String htmlLabel(String label, boolean highlight, String time) {
        return highlight
                ? "<html>" + label + ": <font color='red'>" + time + "</font></html>"
                : "<html>" + label + ": " + time + "</html>";
    }
}
