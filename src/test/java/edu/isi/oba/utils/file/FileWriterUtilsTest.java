package edu.isi.oba.utils.file;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FileWriterUtils}. Verifies file writing functionality. */
public class FileWriterUtilsTest extends BaseTest {

	@Test
	void shouldThrowException_whenInstantiatingUtilityClass() throws Exception {
		Constructor<FileWriterUtils> constructor = FileWriterUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void shouldWriteContent_whenFilePathIsValid() throws Exception {
		Path tempFile = Files.createTempFile("test", ".txt");
		String content = "Hello, world!";
		FileWriterUtils.writeFile(tempFile.toString(), content);
		String readContent = Files.readString(tempFile);
		assertEquals(content, readContent);
		Files.deleteIfExists(tempFile);
	}
}
