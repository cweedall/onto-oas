package edu.isi.oba.utils.yaml;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import org.junit.jupiter.api.Test;

public class YamlUtilsTest {

	@Test
	void testPrivateConstructor() throws Exception {
		var constructor = YamlUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(
				java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());
	}

	@Test
	public void testGetYamlData() {
		String path = "src/test/config/pplan.yaml";
		YamlConfig config = YamlUtils.getYamlData(path);
		assertNotNull(config);
		assertNotNull(config.getManager());
	}

	@Test
	public void testGetYamlData_FileNotFound() {
		// Inject a no-op exit handler to prevent System.exit
		FatalErrorHandler.setExitHandlerForTesting(
				code -> {
					// Do nothing or log for verification
				});

		String nonExistentPath = "src/test/resources/non_existent_file.yaml";
		YamlConfig config = YamlUtils.getYamlData(nonExistentPath);
		assertNull(config);
	}

	@Test
	public void testGetYamlData_MalformedYaml() {
		// Inject a no-op exit handler to prevent System.exit
		FatalErrorHandler.setExitHandlerForTesting(
				code -> {
					// Do nothing or log for verification
				});

		String malformedPath = "src/test/resources/malformed_test.yaml";
		YamlConfig config = YamlUtils.getYamlData(malformedPath);
		assertNull(config);
	}

	@Test
	public void testGetYamlData_StreamReadException() {
		// Inject a no-op exit handler to prevent System.exit
		FatalErrorHandler.setExitHandlerForTesting(
				code -> {
					// Do nothing or log for verification
				});

		String path =
				"src/test/resources/invalid_stream.yaml"; // triggers StreamReadException due to invalid
		// YAML syntax
		YamlConfig config = YamlUtils.getYamlData(path);
		assertNull(config);
	}

	@Test
	public void testGetYamlData_DatabindException() {
		// Inject a no-op exit handler to prevent System.exit
		FatalErrorHandler.setExitHandlerForTesting(
				code -> {
					// Do nothing or log for verification
				});

		String path = "src/test/resources/invalid_databind.yaml"; // triggers DatabindException due to
		// incompatible structure
		YamlConfig config = YamlUtils.getYamlData(path);
		assertNull(config);
	}

	@Test
	public void testGetYamlData_GenericException() {
		// Inject a no-op exit handler to prevent System.exit
		FatalErrorHandler.setExitHandlerForTesting(
				code -> {
					// Do nothing or log for verification
				});

		String path =
				"src/test/resources/generic_exception"; // triggers generic Exception by passing a directory
		YamlConfig config = YamlUtils.getYamlData(path);
		assertNull(config);
	}
}
