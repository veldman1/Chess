package chessModel;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChessLogger {

    private static final boolean VERBOSE = true;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static String timestamp() {
        return "[" + LocalTime.now().format(TIME_FORMATTER) + "]";
    }

    public static void logGameEvent(String message) {
        if (VERBOSE) {
            System.out.println(timestamp() + " [Game] " + message);
        }
    }

    public static void logUserAction(String message) {
        if (VERBOSE) {
            System.out.println(timestamp() + " [User] " + message);
        }
    }

    public static void logGame(String message) {
        if (VERBOSE) {
            System.out.println(timestamp() + " [Game] " + message);
        }
    }
}
