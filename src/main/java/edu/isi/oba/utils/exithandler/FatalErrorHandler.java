package edu.isi.oba.utils.exithandler;

import static edu.isi.oba.Oba.logger;

import java.util.logging.Level;

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
		RuntimeException ex = new RuntimeException(message);
		fatal(message, ex);
	}

	public static void fatal(String message, Throwable cause) {
		logger.log(Level.SEVERE, "FATAL ERROR: " + message);
		cause.printStackTrace(System.err);
		exitHandler.exit(1);
	}
}
