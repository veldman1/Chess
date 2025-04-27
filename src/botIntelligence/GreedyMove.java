package botIntelligence;

import chessModel.Board;
import chessModel.Player;
import chessModel.SquareStatus;
import chessModel.piece.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GreedyMove extends Player {

    private int delayMillis = 0;

    @Override
    public Integer[] getMove(Board board) {
        List<Integer[]> legalMoves = new ArrayList<>();
        boolean inCheck = board.isInCheck(side);

        for (Piece piece : board.getPieces()) {
            if (piece.getSide() != side) {
                continue;
            }

            int startX = piece.getX();
            int startY = piece.getY();

            for (int destX = 0; destX < board.boardHeight; destX++) {
                for (int destY = 0; destY < board.boardWidth; destY++) {
                    SquareStatus status = board.getSquareStatus(destX, destY, side);

                    if (status != SquareStatus.TEAM
                            && piece.validMove(destX, destY, status)
                            && !board.isObstructed(piece, destX, destY)) {

                        Board clone = board.cloneBoard();
                        Piece clonePiece = clone.getPiece(startX, startY);
                        if (clonePiece == null) {
                            continue;
                        }
                        clonePiece.forceMove(destX, destY);

                        boolean kingSafe = !clone.isInCheck(side);

                        if (!inCheck || kingSafe) {
                            legalMoves.add(new Integer[]{startX, startY, destX, destY});
                        }
                    }
                }
            }
        }

        if (legalMoves.isEmpty()) {
            return null;
        }

        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (inCheck) {
            // If in check, pick any random legal move that escapes
            return legalMoves.get(ThreadLocalRandom.current().nextInt(legalMoves.size()));
        } else {
            // Otherwise pick the move that maximizes score
            Integer[] bestMove = null;
            int bestScore = Integer.MIN_VALUE;

            for (Integer[] move : legalMoves) {
                Board clone = board.cloneBoard();
                Piece clonePiece = clone.getPiece(move[0], move[1]);
                if (clonePiece == null) {
                    continue;
                }
                clonePiece.forceMove(move[2], move[3]);

                int score = clone.getScore(side);

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }

            return bestMove != null
                    ? bestMove
                    : legalMoves.get(ThreadLocalRandom.current().nextInt(legalMoves.size()));
        }
    }

    public void setDelay(int delayMillis) {
        this.delayMillis = delayMillis;
    }
}
