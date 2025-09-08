package edu.isi.oba.utils.file;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FolderUtils}. Verifies recursive folder copying. */
public class FolderUtilsTest {

	@Test
	void shouldThrowException_whenInstantiatingUtilityClass() throws Exception {
		Constructor<FolderUtils> constructor = FolderUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void shouldCopyFolderContents_whenSourceIsValid() throws Exception {
		Path sourceDir = Files.createTempDirectory("source");
		Path destDir = Files.createTempDirectory("dest");
		Path sourceFile = Files.createFile(sourceDir.resolve("file.txt"));
		Files.writeString(sourceFile, "Sample");

		FolderUtils.copyFolder(sourceDir, destDir);

		Path copiedFile = destDir.resolve("file.txt");
		assertTrue(Files.exists(copiedFile));
		assertEquals("Sample", Files.readString(copiedFile));

		Files.walk(sourceDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
		Files.walk(destDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
	}
}
