package edu.isi.oba.utils.cli;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

public class CliUtilsTest extends BaseTest {

	@Test
	void testPrivateConstructor() throws Exception {
		Constructor<CliUtils> constructor = CliUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
	}

	@Test
	public void testGetConfigYaml() {
		String[] args = {"-c", "config.yaml"};
		String result = CliUtils.getYamlConfigFileArgument(args);
		assertEquals("config.yaml", result);
	}

	@Test
	public void testGetConfigYaml_MissingArgument() {
		// Inject a no-op exit handler to prevent System.exit
		FatalErrorHandler.setExitHandlerForTesting(
				code -> {
					// Do nothing or log for verification
				});

		String[] args = {}; // Missing required argument
		String result = CliUtils.getYamlConfigFileArgument(args);
		assertNull(result); // Should return null after catching the exception
	}
}
