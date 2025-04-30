package chessViewController;

import chessModel.Board;
import chessModel.Player;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HumanPlayer extends Player {

    private ChessView view;
    private GraphicsGUI gui;
    private Integer[] move;
    private final Object moveLock = new Object();

    /**
     * Sets up the human player to listen for clicks on the board.
     *
     * @param theGui The GUI controller
     * @param theView The chess view
     */
    public void setupView(GraphicsGUI theGui, ChessView theView) {
        this.view = theView;
        this.gui = theGui;

        MouseAdapter humanInput = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (gui.getCurrentSide() == side && e.getButton() == MouseEvent.BUTTON1) {
                    int cellSize = view.getCellSize();
                    int xLoc = e.getY() / cellSize;
                    int yLoc = e.getX() / cellSize;

                    Integer[] newMove = gui.handleLocationClicked(xLoc, yLoc);
                    if (newMove != null) {
                        synchronized (moveLock) {
                            move = newMove;
                            moveLock.notify();
                        }
                    }
                }
            }
        };

        view.addMouseListener(humanInput);
    }

    /**
     * Blocks until the user selects a move through the GUI.
     *
     * @param board The board to reference (not used heavily here)
     * @return The move array [oldX, oldY, newX, newY]
     */
    @Override
    public Integer[] getMove(Board board) {
        synchronized (moveLock) {
            while (move == null) {
                try {
                    moveLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            Integer[] moveCopy = move;
            move = null;
            return moveCopy;
        }
    }
}
