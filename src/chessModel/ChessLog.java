package chessModel;

import chessModel.piece.Pawn;
import chessModel.piece.Piece;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import util.ChessUtil;

public class ChessLog {

    private int fullMoveClock;
    private int halfMoveClock;
    private final ArrayList<Integer[]> rawLog;
    private final ArrayList<String> sanLog;
    private String player1;
    private String player2;
    private final String time;
    private final String date;

    public ChessLog() {
        rawLog = new ArrayList<>();
        sanLog = new ArrayList<>();
        fullMoveClock = 1;
        halfMoveClock = 0;
        date = new SimpleDateFormat("yyyy:MM:dd").format(new Date());
        time = new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    public void setPlayerNames(String player1Name, String player2Name) {
        this.player1 = player1Name;
        this.player2 = player2Name;
    }

    public void copyFrom(ChessLog other) {
        this.fullMoveClock = other.fullMoveClock;
        this.halfMoveClock = other.halfMoveClock;
        this.rawLog.clear();
        this.rawLog.addAll(other.getRawLog());
        this.sanLog.clear();
        this.sanLog.addAll(other.getSanLog());
        this.player1 = other.player1;
        this.player2 = other.player2;
    }

    public ArrayList<Integer[]> getRawLog() {
        return new ArrayList<>(rawLog);
    }

    public ArrayList<String> getSanLog() {
        return new ArrayList<>(sanLog);
    }

    public void addToLog(int oldX, int oldY, int newX, int newY, Piece piece, Piece capture) {
        rawLog.add(new Integer[]{oldX, oldY, newX, newY});

        if (rawLog.size() % 2 == 0) {
            fullMoveClock++;
        }

        String center = (capture != null) ? "x" : "-";
        if (piece instanceof Pawn && Math.abs(oldX - newX) == 2) {
            sanLog.add(ChessUtil.convertLocation(newX, newY));
        } else {
            sanLog.add(piece.getChar().toUpperCase() + ChessUtil.convertLocation(oldX, oldY) + center + ChessUtil.convertLocation(newX, newY));
        }
    }

    public void resetHalfMoveClock() {
        halfMoveClock = 0;
    }

    public void incrementHalfMoveClock() {
        halfMoveClock++;
    }

    @Override
    public String toString() {
        return toPGN();
    }

    public String toPGN() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Date \"").append(date).append("\"]\n");
        sb.append("[Time \"").append(time).append("\"]\n");
        if (player1 != null) {
            sb.append("[White \"").append(player1).append("\"]\n");
        }
        if (player2 != null) {
            sb.append("[Black \"").append(player2).append("\"]\n");
        }
        sb.append("\n");

        int moveNumber = 1;
        for (String move : sanLog) {
            if (moveNumber % 2 == 1) {
                sb.append((moveNumber + 1) / 2).append(". ");
            }
            sb.append(move).append(" ");
            if (moveNumber % 6 == 0) {
                sb.append("\n");
            }
            moveNumber++;
        }
        return sb.toString();
    }

    public int getFullMoveCount() {
        return fullMoveClock;
    }

    public int getHalfMoveCount() {
        return halfMoveClock;
    }
    
    public int getHalfMoveClock() {
        return halfMoveClock;
    }
}
