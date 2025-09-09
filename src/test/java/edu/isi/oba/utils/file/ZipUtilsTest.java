package edu.isi.oba.utils.file;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link ZipUtils}.
 *
 * <p>Verifies ZIP extraction using Apache Commons Compress. Covers:
 *
 * <ul>
 *   <li>Valid ZIP extraction
 *   <li>Zip Slip protection
 *   <li>Directory creation
 *   <li>Parent directory creation
 * </ul>
 */
public class ZipUtilsTest extends BaseTest {

	@TempDir Path tempDir;

	@Test
	void testPrivateConstructor() throws Exception {
		var constructor = ZipUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(
				java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());
	}

	@Test
	public void shouldExtractZipContents_whenValidZipProvided() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("folder/")); // directory
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("folder/file.txt")); // file
			zos.write("Hello Zip".getBytes());
			zos.closeEntry();
		}

		byte[] zipBytes = baos.toByteArray();
		InputStream zipStream = new ByteArrayInputStream(zipBytes);
		Path outputDir = Files.createTempDirectory("ziptest");

		ZipUtils.unZipIt(zipStream, outputDir.toString());

		Path extractedFile = outputDir.resolve("folder/file.txt");
		assertTrue(Files.exists(extractedFile));
		assertEquals("Hello Zip", Files.readString(extractedFile));

		Files.walk(outputDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
	}

	@Test
	public void shouldThrowFatal_whenZipSlipDetected() throws Exception {
		File outputDir = tempDir.resolve("output").toFile();
		outputDir.mkdir();

		try (InputStream zipStream = getClass().getResourceAsStream("/test-malicious.zip"); ) {

			assertNotNull(zipStream, "test-malicious.zip not found in resources");

			assertThrows(Exception.class, () -> ZipUtils.unZipIt(zipStream, outputDir.getAbsolutePath()));
		}
	}

	@Test
	public void shouldCreateParentDirectory_whenMissing() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("nested/dir/file.txt"));
			zos.write("Nested content".getBytes());
			zos.closeEntry();
		}

		byte[] zipBytes = baos.toByteArray();
		InputStream zipStream = new ByteArrayInputStream(zipBytes);
		Path outputDir = Files.createTempDirectory("ziptest");

		ZipUtils.unZipIt(zipStream, outputDir.toString());

		Path nestedFile = outputDir.resolve("nested/dir/file.txt");
		assertTrue(Files.exists(nestedFile));
		assertEquals("Nested content", Files.readString(nestedFile));

		Files.walk(outputDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> p.toFile().delete());
	}

	@Test
	public void shouldHandleZipEntryWithNoParentDirectory() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("file.txt")); // No parent directory
			zos.write("content".getBytes());
			zos.closeEntry();
		}

		InputStream zipStream = new ByteArrayInputStream(baos.toByteArray());
		Path outputDir = Files.createTempDirectory("ziptest");

		// Ensure parent doesn't exist
		Path parentDir = outputDir.resolve("nested/dir");
		assertFalse(Files.exists(parentDir));

		ZipUtils.unZipIt(zipStream, outputDir.toString());

		assertTrue(Files.exists(outputDir.resolve("file.txt")));

		Files.walk(outputDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
	}
}
