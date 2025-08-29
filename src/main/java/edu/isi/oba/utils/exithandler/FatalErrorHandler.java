package edu.isi.oba.utils.exithandler;

public class FatalErrorHandler {
	private static ExitHandler exitHandler = new DefaultExitHandler();

	private FatalErrorHandler() {
		throw new UnsupportedOperationException("Static utility class");
	}

	// Only used in tests
	public static void setExitHandlerForTesting(ExitHandler handler) {
		if (handler != null) {
			exitHandler = handler;
		}
	}

	public static void fatal(String message) {
		System.err.println("FATAL ERROR: " + message);
		exitHandler.exit(1);
	}

	public static void fatal(String message, Throwable cause) {
		System.err.println("FATAL ERROR: " + message);
		cause.printStackTrace(System.err);
		exitHandler.exit(1);
	}
}
