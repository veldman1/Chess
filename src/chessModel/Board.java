package chessModel;

import chessModel.piece.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import util.ChessUtil;

public class Board {

    public final int boardWidth;
    public final int boardHeight;
    private final ArrayList<Piece> pieces;
    private final ReentrantLock boardLock = new ReentrantLock();
    private final ChessLog movelog;
    private int whiteScore;
    private int blackScore;
    private Rook blackKingSide, whiteKingSide, blackQueenSide, whiteQueenSide;
    private King blackKing, whiteKing;
    private String enPassantTarget;

    public Board() {
        this(true);
    }

    public Board(boolean preConfigure) {
        boardWidth = 8;
        boardHeight = 8;
        pieces = new ArrayList<>();
        whiteScore = 0;
        blackScore = 0;

        if (preConfigure) {
            for (int i = 0; i < 8; i++) {
                pieces.add(new Pawn(1, i, 1));
                pieces.add(new Pawn(6, i, 0));
            }
            blackQueenSide = new Rook(0, 0, 1);
            pieces.add(blackQueenSide);
            blackKingSide = new Rook(0, 7, 1);
            pieces.add(blackKingSide);
            whiteQueenSide = new Rook(7, 0, 0);
            pieces.add(whiteQueenSide);
            whiteKingSide = new Rook(7, 7, 0);
            pieces.add(whiteKingSide);
            pieces.add(new Knight(0, 1, 1));
            pieces.add(new Knight(0, 6, 1));
            pieces.add(new Knight(7, 1, 0));
            pieces.add(new Knight(7, 6, 0));
            pieces.add(new Bishop(0, 2, 1));
            pieces.add(new Bishop(0, 5, 1));
            pieces.add(new Bishop(7, 2, 0));
            pieces.add(new Bishop(7, 5, 0));
            pieces.add(new Queen(0, 3, 1));
            pieces.add(new Queen(7, 3, 0));
            whiteKing = new King(0, 4, 1);
            pieces.add(whiteKing);
            blackKing = new King(7, 4, 0);
            pieces.add(blackKing);
        }

        enPassantTarget = "";
        movelog = new ChessLog();
    }

    public boolean move(int oldX, int oldY, int x, int y) {
        boardLock.lock();
        try {
            Piece selectedP = getPiece(oldX, oldY);
            if (selectedP == null) {
                return false;
            }

            Piece otherP = getPiece(x, y);
            SquareStatus status = SquareStatus.EMPTY;
            if (otherP != null) {
                status = (otherP.getSide() == selectedP.getSide()) ? SquareStatus.TEAM : SquareStatus.ENEMY;
            }
            if ((ChessUtil.convertFile(y) + Integer.toString(ChessUtil.convertRow(x))).equals(enPassantTarget)) {
                status = SquareStatus.ENEMY;
                otherP = selectedP.getSide() == 0 ? getPiece(x + 1, y) : getPiece(x - 1, y);
            }
            if (!status.equals(SquareStatus.TEAM) && selectedP.validMove(x, y, status) && !isObstructed(selectedP, x, y)) {
                if (isInCheck(selectedP.getSide()) && !resolvesCheck(selectedP, x, y)) {
                    return false;
                }
                selectedP.move(x, y, status);
                if (selectedP instanceof Pawn) {
                    movelog.resetHalfMoveClock();
                    if (Math.abs(oldX - x) == 2) {
                        enPassantTarget = ChessUtil.convertFile(y) + Integer.toString(ChessUtil.convertRow((oldX + x) / 2));
                    } else {
                        enPassantTarget = "";
                    }
                } else {
                    movelog.incrementHalfMoveClock();
                    enPassantTarget = "";
                }
                if (status.equals(SquareStatus.ENEMY)) {
                    int scoreEarned = otherP.getValue();
                    if (selectedP.getSide() == 0) {
                        whiteScore += scoreEarned;
                    } else {
                        blackScore += scoreEarned;
                    }
                    pieces.remove(otherP);
                    movelog.resetHalfMoveClock();
                }
                movelog.addToLog(oldX, oldY, x, y, selectedP, otherP);
                return true;
            }
            return false;
        } finally {
            boardLock.unlock();
        }
    }

    public boolean isObstructed(Piece p, int x, int y) {
        boardLock.lock();
        try {
            if (p.getX() == x) {
                int dir = y > p.getY() ? 1 : -1;
                for (int tmpY = p.getY() + dir; tmpY != y; tmpY += dir) {
                    if (getPiece(x, tmpY) != null) {
                        return true;
                    }
                }
            }
            if (p.getY() == y) {
                int dir = x > p.getX() ? 1 : -1;
                for (int tmpX = p.getX() + dir; tmpX != x; tmpX += dir) {
                    if (getPiece(tmpX, y) != null) {
                        return true;
                    }
                }
            }
            if (Math.abs(p.getX() - x) == Math.abs(p.getY() - y)) {
                int dirX = x > p.getX() ? 1 : -1;
                int dirY = y > p.getY() ? 1 : -1;
                int tmpX = p.getX() + dirX;
                int tmpY = p.getY() + dirY;
                while (tmpX != x && tmpY != y) {
                    if (getPiece(tmpX, tmpY) != null) {
                        return true;
                    }
                    tmpX += dirX;
                    tmpY += dirY;
                }
            }
            return false;
        } finally {
            boardLock.unlock();
        }
    }

    public boolean isThreatenedSquare(int x, int y, int side) {
        boardLock.lock();
        try {
            for (Piece p : pieces) {
                if (p.getSide() != side && p.validMove(x, y, getSquareStatus(x, y, p.getSide())) && !isObstructed(p, x, y)) {
                    return true;
                }
            }
            return false;
        } finally {
            boardLock.unlock();
        }
    }

    public boolean isInCheck(int side) {
        boardLock.lock();
        try {
            for (Piece p : pieces) {
                if (p instanceof King && p.getSide() == side) {
                    return isThreatenedSquare(p.getX(), p.getY(), side);
                }
            }
            return false;
        } finally {
            boardLock.unlock();
        }
    }

    /**
     * True if the king is in check OR the side has no legal moves. (i.e. check
     * or “stuck”.)
     */
    public boolean isThreatenedOrStuck(int side) {
        // 1) if the king is under threat, return true immediately
        if (isInCheck(side)) {
            return true;
        }

        // 2) otherwise, if there are no legal moves at all, return true
        return getAllMoves(side).isEmpty();
    }

    public boolean resolvesCheck(Piece p, int x, int y) {
        Board clone = cloneBoard();
        Piece cp = clone.getPiece(p.getX(), p.getY());
        if (cp == null) {
            return false;
        }
        cp.forceMove(x, y);
        return !clone.isInCheck(p.getSide());
    }

    private List<Integer[]> movesFor(Piece p) {
        List<Integer[]> list = new ArrayList<>();
        int side = p.getSide();
        for (int tx = 0; tx < boardHeight; tx++) {
            for (int ty = 0; ty < boardWidth; ty++) {
                SquareStatus status = getSquareStatus(tx, ty, side);
                if (status == SquareStatus.TEAM) {
                    continue;
                }
                if (!p.validMove(tx, ty, status)) {
                    continue;
                }
                if (isObstructed(p, tx, ty)) {
                    continue;
                }
                Board clone = cloneBoard();
                Piece cp = clone.getPiece(p.getX(), p.getY());
                cp.forceMove(tx, ty);
                if (!clone.isInCheck(side)) {
                    list.add(new Integer[]{p.getX(), p.getY(), tx, ty});
                }
            }
        }
        return list;
    }

    public List<Integer[]> getAllMoves(int side) {
        List<Integer[]> all = new ArrayList<>();
        for (Piece p : getPieces()) {
            if (p.getSide() != side) {
                continue;
            }
            all.addAll(movesFor(p));
        }
        return all;
    }

    public Board cloneBoard() {
        Board clone = new Board(false);
        for (Piece p : pieces) {
            clone.addPiece(p.deepCopy());
        }
        clone.whiteScore = this.whiteScore;
        clone.blackScore = this.blackScore;
        clone.enPassantTarget = this.enPassantTarget;
        clone.movelog.copyFrom(this.movelog);
        return clone;
    }

    public Piece getPiece(int x, int y) {
        boardLock.lock();
        try {
            for (Piece piece : pieces) {
                if (x == piece.getX() && y == piece.getY()) {
                    return piece;
                }
            }
            return null;
        } finally {
            boardLock.unlock();
        }
    }

    public ArrayList<Piece> getPieces() {
        boardLock.lock();
        try {
            return new ArrayList<>(pieces);
        } finally {
            boardLock.unlock();
        }
    }

    public SquareStatus getSquareStatus(int x, int y, int side) {
        boardLock.lock();
        try {
            for (Piece p : pieces) {
                if (p.getX() == x && p.getY() == y) {
                    return (p.getSide() == side) ? SquareStatus.TEAM : SquareStatus.ENEMY;
                }
            }
            return SquareStatus.EMPTY;
        } finally {
            boardLock.unlock();
        }
    }

    public void addPiece(Piece p) {
        boardLock.lock();
        try {
            pieces.add(p);
        } finally {
            boardLock.unlock();
        }
    }

    public void removePieces(Piece p) {
        boardLock.lock();
        try {
            pieces.remove(p);
        } finally {
            boardLock.unlock();
        }
    }

    public void setPlayerNames(String name1, String name2) {
        movelog.setPlayerNames(name1, name2);
    }

    public int getWhiteScore() {
        return whiteScore;
    }

    public int getBlackScore() {
        return blackScore;
    }

    public String getPGN() {
        return movelog.toString();
    }

    public String getFEN() {
        return ""; // (or build a FEN generator if you want)
    }

    public ArrayList<Integer[]> getMoveLog() {
        return movelog.getRawLog();
    }

    public int getScore(int side) {
        return (side == 0) ? getWhiteScore() : getBlackScore();
    }

    public int getHalfMoveClock() {
        return movelog.getHalfMoveClock();
    }

}
