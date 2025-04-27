package chessModel.piece;

import chessModel.SquareStatus;

public class Bishop extends Piece {

    public Bishop(int x, int y, int side) {
        this.x = x;
        this.y = y;
        this.side = side;
    }

    @Override
    public Piece deepCopy() {
        return new Bishop(this.x, this.y, this.side);
    }

    public String getChar() {
        if (side == 0) {
            return "B";
        }
        return "b";
    }

    public boolean validMove(int x, int y, SquareStatus status) {
        return ((Math.abs(this.x - x) == Math.abs(this.y - y)));
    }

    public int getValue() {
        return 3;
    }
}
