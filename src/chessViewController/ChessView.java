// src/chessViewController/ChessView.java
package chessViewController;

import chessModel.Board;
import chessModel.piece.Piece;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import util.ChessUtil;

@SuppressWarnings("serial")
public class ChessView extends JPanel {

    private static final Map<String, Character> CHAR_MAP = Map.ofEntries(
            Map.entry("K", '\u2654'), Map.entry("Q", '\u2655'),
            Map.entry("R", '\u2656'), Map.entry("B", '\u2657'),
            Map.entry("N", '\u2658'), Map.entry("P", '\u2659'),
            Map.entry("k", '\u265A'), Map.entry("q", '\u265B'),
            Map.entry("r", '\u265C'), Map.entry("b", '\u265D'),
            Map.entry("n", '\u265E'), Map.entry("p", '\u265F')
    );

    public final Board board;
    private final Color brown = new Color(130, 70, 30);
    private Piece selected;

    // squares to overlay when highlighting
    private List<Point> highlightSquares = new ArrayList<>();

    public ChessView(Board board) {
        this.board = board;
        setLayout(new FlowLayout());
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    int cellSize = getCellSize();
                    int xLoc = e.getY() / cellSize;
                    int yLoc = e.getX() / cellSize;
                    System.out.println(ChessUtil.convertLocation(xLoc, yLoc));
                }
            }
        });
    }

    /**
     * Called by the GUI when you want to highlight a set of squares
     */
    public void setHighlightSquares(List<Point> squares) {
        this.highlightSquares = new ArrayList<>(squares);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        final int cellSize = getCellSize();
        final int fontSize = (cellSize * 3) / 4;
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));

        boolean parity = false;

        for (int y = 0; y < board.boardHeight; y++) {
            for (int x = 0; x < board.boardWidth; x++) {
                // draw base square
                g.setColor(parity ? Color.LIGHT_GRAY : Color.WHITE);
                g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);

                // draw highlight overlay if requested
                for (Point p : highlightSquares) {
                    if (p.x == x && p.y == y) {
                        g.setColor(new Color(0, 255, 0, 64));
                        g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                        break;
                    }
                }

                // draw piece
                Piece p = board.getPiece(y, x);
                if (p != null) {
                    if (p.equals(selected)) {
                        g.setColor(Color.BLUE);
                    } else if (p.getSide() == 0) {
                        g.setColor(Color.RED);
                    } else {
                        g.setColor(Color.BLACK);
                    }
                    Character c = CHAR_MAP.getOrDefault(p.getChar(), p.getChar().charAt(0));
                    String ch = c.toString();
                    g.drawString(
                            ch,
                            x * cellSize + cellSize / 2 - fontSize / 2,
                            y * cellSize + cellSize / 2 + fontSize / 2
                    );
                }

                parity = !parity;
            }
            if (board.boardWidth % 2 == 0) {
                parity = !parity;
            }
        }
    }

    public int getCellSize() {
        int w = getWidth() / board.boardWidth;
        int h = getHeight() / board.boardHeight;
        return Math.min(w, h);
    }

    public void setSelected(Piece piece) {
        this.selected = piece;
    }

    public Piece getSelected() {
        return selected;
    }
}
