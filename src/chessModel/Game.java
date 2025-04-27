package chessModel;

import chessModel.piece.Piece;
import chessViewController.HumanPlayer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Game {

    private final Board board;
    private final ScheduledExecutorService scheduler;
    private final Player player1, player2;

    // move counters
    private int moveCount = 0;
    private static final int MAX_HALF_MOVE_COUNT = 100;            // 50 full moves
    private static final int MAX_MOVE_COUNT = Integer.MAX_VALUE; // or 500, if you like

    // why the game ended
    private String endReason = "";

    // game state
    private int currentSide;
    private int invalidMovesCount;
    private int winner;
    private final int gameMode;

    // timing
    private final Time player1TimeLeft;
    private final Time player2TimeLeft;

    // move executor
    private Thread computeMove;

    public static final int HUMAN_VS_AI = 0;
    public static final int HUMAN_VS_HUMAN = 1;
    public static final int AI_VS_AI = 2;

    private static final int DEFAULT_TIME_SECONDS = 60 * 45;
    public static final int MAX_INVALID_MOVES = 10;

    public Game(int gameMode, Player player1, Player player2) {
        this.gameMode = gameMode;
        this.player1 = player1;
        this.player2 = player2;
        this.currentSide = 0;
        this.invalidMovesCount = 1;
        this.winner = -1;
        this.board = new Board();
        board.setPlayerNames(player1.getName(), player2.getName());

        this.player1TimeLeft = new Time(DEFAULT_TIME_SECONDS);
        this.player2TimeLeft = new Time(DEFAULT_TIME_SECONDS);
        this.scheduler = Executors.newScheduledThreadPool(2);

        ChessLogger.logGameEvent("New game started: "
                + player1.getName() + " vs. " + player2.getName()
                + " Mode=" + describeMode(gameMode));

        startClocks();
        startGameLoop();
    }

    private void startClocks() {
        scheduler.scheduleAtFixedRate(() -> {
            if (currentSide == 0) {
                player1TimeLeft.dec();
            }
        }, 1, 1, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            if (currentSide == 1) {
                player2TimeLeft.dec();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void startGameLoop() {
        // kick off the very first turn
        scheduler.schedule(this::performTurn, 0, TimeUnit.MILLISECONDS);
    }

    private void performTurn() {
        ChessLogger.logGameEvent("Starting performTurn for side " + currentSide);

        cancelPreviousMoveIfRunning();

        // log in‐check status at turn start
        if (board.isInCheck(currentSide)) {
            ChessLogger.logGameEvent(getCurrentPlayer().getName() + " is in check.");
        }

        // bail if already over
        if (isGameOver()) {
            ChessLogger.logGameEvent("Game is over at start of turn.");
            declareWinner();
            return;
        }

        ChessLogger.logGameEvent("Turn begins: " + getCurrentPlayer().getName());

        computeMove = new Thread(() -> {
            ChessLogger.logGameEvent("computeMove thread running for side " + currentSide);
            ChessLogger.logGameEvent("Calling getMove(board) for " + getCurrentPlayer().getName());

            Integer[] move = getCurrentPlayer().getMove(board);

            if (move == null) {
                ChessLogger.logGameEvent("getMove() returned null for " + getCurrentPlayer().getName());
                incrementInvalidMoves();

            } else {
                ChessLogger.logGameEvent(getCurrentPlayer().getName()
                        + " attempted move: " + moveToString(move));

                if (move.length >= 4) {
                    int oldX = move[0], oldY = move[1], newX = move[2], newY = move[3];
                    Piece piece = board.getPiece(oldX, oldY);

                    if (piece != null
                            && piece.getSide() == currentSide
                            && move(oldX, oldY, newX, newY)) {

                        ChessLogger.logGameEvent("Move succeeded: " + moveToString(move));

                        // increment our global move counter
                        moveCount++;
                        ChessLogger.logGameEvent("Move count is now " + moveCount);

                        int nextSide = binaryOpposite(currentSide);
                        if (board.isInCheck(nextSide)) {
                            ChessLogger.logGameEvent("Check to "
                                    + (nextSide == 0 ? player1.getName() : player2.getName()));
                            if (isCheckMate()) {
                                ChessLogger.logGameEvent("Checkmate! Winner: "
                                        + getCurrentPlayer().getName());
                            }
                        } else if (isDraw()) {
                            ChessLogger.logGameEvent("Draw by stalemate or no legal moves.");
                        }

                        // reset invalid‐moves counter on valid move
                        invalidMovesCount = 1;

                        // if we just triggered game‐over, handle it
                        if (isGameOver()) {
                            ChessLogger.logGameEvent("Game over after successful move.");
                            declareWinner();
                            return;
                        }

                        ChessLogger.logGameEvent("Move done, scheduling next turn.");
                        scheduleNextTurn();
                        return;
                    }
                }

                ChessLogger.logGameEvent("Move invalid or piece mismatch, incrementing invalid moves.");
                incrementInvalidMoves();
            }

            // after an invalid or null move, check again
            if (isGameOver()) {
                ChessLogger.logGameEvent("Game over after invalid move.");
                declareWinner();
            } else {
                ChessLogger.logGameEvent("Scheduling next turn after invalid move.");
                scheduleNextTurn();
            }
        });

        computeMove.start();
        ChessLogger.logGameEvent("computeMove thread started.");
    }

    private String moveToString(Integer[] move) {
        if (move == null) {
            return "null move";
        }
        if (move.length < 4) {
            return "incomplete move: " + java.util.Arrays.toString(move);
        }
        return "(" + move[0] + "," + move[1] + ") -> (" + move[2] + "," + move[3] + ")";
    }

    private void cancelPreviousMoveIfRunning() {
        if (computeMove != null && computeMove.isAlive()) {
            computeMove.interrupt();
        }
    }

    public boolean isGameOver() {
        boolean halfMoveDraw = board.getHalfMoveClock() >= MAX_HALF_MOVE_COUNT;
        boolean moveLimit = moveCount > MAX_MOVE_COUNT;
        boolean invalidDraw = invalidMovesCount > MAX_INVALID_MOVES;
        boolean mate = isCheckMate();
        boolean stalemate = isDraw();
        boolean stuck = board.isThreatenedOrStuck(currentSide);

        // DEBUG: exactly which condition is tripping
        /*ChessLogger.logGameEvent(String.format(
            "isGameOver? halfMove=%d/%d, moves=%d/%d, invalid=%d/%d, mate=%b, stalemate=%b",
            board.getHalfMoveClock(), MAX_HALF_MOVE_COUNT,
            moveCount, MAX_MOVE_COUNT,
            invalidMovesCount, MAX_INVALID_MOVES,
            mate, stalemate
        ));*/
        return halfMoveDraw
                || moveLimit
                || invalidDraw
                || mate
                || stalemate
                || stuck;
    }

    private void declareWinner() {
        // 1) 50-move rule
        if (board.getHalfMoveClock() >= MAX_HALF_MOVE_COUNT) {
            endReason = "Draw by 50-move rule (no pawn move or capture in 50 moves).";
            winner = -1;

            // 2) total-move limit
        } else if (moveCount > MAX_MOVE_COUNT) {
            endReason = "Draw due to move limit (" + moveCount + " moves).";
            winner = -1;

            // 3) checkmate (player is in check and has no moves left)
        } else if (isCheckMate()) {
            winner = binaryOpposite(currentSide);
            endReason = "Checkmate! Winner: " + getCurrentPlayer().getName();

            // 4) check or no legal moves (i.e., "stuck" condition)
        } else if (board.isThreatenedOrStuck(currentSide)) {
            // If the current side is either in check or cannot make a valid move
            endReason = "Player is stuck (in check or no legal moves).";
            winner = binaryOpposite(currentSide);

            // 5) stalemate or no moves
        } else if (isDraw()) {
            endReason = "Draw by stalemate or no legal moves.";
            winner = -1;

            // 6) fallback
        } else {
            endReason = "Game over.";
            winner = -1;
        }

        ChessLogger.logGameEvent(endReason);
        scheduler.shutdown();
    }

    public boolean move(int oldX, int oldY, int newX, int newY) {
        Piece p = board.getPiece(oldX, oldY);
        if (p == null) {
            return false;
        }
        boolean success = board.move(oldX, oldY, newX, newY);
        if (success && p.getSide() == currentSide) {
            currentSide = binaryOpposite(currentSide);
        }
        return success;
    }

    public void incrementInvalidMoves() {
        if (getCurrentPlayer() instanceof HumanPlayer) {
            return;
        }
        if (invalidMovesCount == 1) {
            ChessLogger.logGameEvent(getCurrentPlayer().getName() + " invalid 1");
        } else {
            ChessLogger.logGameEvent(" " + invalidMovesCount);
        }
        invalidMovesCount++;
    }

    public boolean isCheckMate() {
        return board.isInCheck(currentSide)
                && board.getAllMoves(currentSide).isEmpty();
    }

    public boolean isDraw() {
        return !board.isInCheck(currentSide)
                && board.getAllMoves(currentSide).isEmpty();
    }

    public int binaryOpposite(int num) {
        return (num == 0) ? 1 : 0;
    }

    public Player getCurrentPlayer() {
        return (currentSide == 0) ? player1 : player2;
    }

    public int getCurrentSide() {
        return currentSide;
    }

    public int getWinner() {
        return winner;
    }

    public int getInvalidMovesCount() {
        return invalidMovesCount;
    }

    public String getEndReason() {
        return endReason;
    }

    public int getGameMode() {
        return gameMode;
    }

    public String getPlayer1Time() {
        return player1TimeLeft.getTime();
    }

    public String getPlayer2Time() {
        return player2TimeLeft.getTime();
    }

    public int getPlayer1Score() {
        return board.getWhiteScore();
    }

    public int getPlayer2Score() {
        return board.getBlackScore();
    }

    public Board getBoard() {
        return board;
    }

    private String describeMode(int m) {
        return switch (m) {
            case HUMAN_VS_AI ->
                "Human vs AI";
            case HUMAN_VS_HUMAN ->
                "Human vs Human";
            case AI_VS_AI ->
                "AI vs AI";
            default ->
                "Unknown";
        };
    }

    private void scheduleNextTurn() {
        if (!scheduler.isShutdown()) {
            ChessLogger.logGameEvent("Scheduling next performTurn in 200ms.");
            scheduler.schedule(this::performTurn, 200, TimeUnit.MILLISECONDS);
        } else {
            ChessLogger.logGameEvent("Scheduler is shutdown; not scheduling next turn.");
        }
    }
}
