package edu.isi.oba.utils.file;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StreamUtils}. Verifies stream copying functionality. */
public class StreamUtilsTest extends BaseTest {

	@Test
	void shouldThrowException_whenInstantiatingUtilityClass() throws Exception {
		Constructor<StreamUtils> constructor = StreamUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void shouldCopyStreamToFile_whenValidInput() throws Exception {
		byte[] data = "Test data".getBytes();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
		Path tempFile = Files.createTempFile("stream", ".txt");
		StreamUtils.copy(inputStream, tempFile);
		byte[] result = Files.readAllBytes(tempFile);
		assertArrayEquals(data, result);
		Files.deleteIfExists(tempFile);
	}
}
