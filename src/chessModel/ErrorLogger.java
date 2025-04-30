package chessModel;

public class ErrorLogger {

    public static void logStartupError(String message) {
        System.err.println("[Startup Error] " + message);
    }

    public static void logException(Throwable t) {
        System.err.println("[Exception] " + t.getClass().getSimpleName() + ": " + t.getMessage());
        t.printStackTrace(System.err);
    }
}
