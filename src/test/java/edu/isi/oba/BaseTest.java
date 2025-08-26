package edu.isi.oba;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {
	protected static Logger logger;

	/**
	 * This method allows you to configure the logger variable that is required to print several
	 * messages during the OBA execution.
	 */
	@BeforeAll
	public static void initializeLogger() throws Exception {
		final var stream = Oba.class.getClassLoader().getResourceAsStream("logging.properties");

		try {
			LogManager.getLogManager().readConfiguration(stream);
			edu.isi.oba.Oba.logger = Logger.getLogger(Oba.class.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// We don't need all the fine-grained logging each time we build and run unit tests.
		edu.isi.oba.Oba.logger.setLevel(Level.SEVERE);
		edu.isi.oba.Oba.logger.addHandler(new ConsoleHandler());
	}
}
