package edu.isi.oba.utils.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileUtilsTest {

	@Test
	public void testCheckTrailingSlash() {
		assertEquals("path/", FileUtils.checkTrailingSlash("path"));
		assertEquals("path/", FileUtils.checkTrailingSlash("path/"));
	}

	@Test
	public void testWriteFile(@TempDir Path tempDir) throws Exception {
		String content = "Hello, world!";
		File file = tempDir.resolve("test.txt").toFile();
		FileUtils.writeFile(file.getAbsolutePath(), content);
		String readContent = Files.readString(file.toPath());
		assertEquals(content, readContent);
	}

	@Test
	public void testCopy(@TempDir Path tempDir) throws Exception {
		File source = tempDir.resolve("source.txt").toFile();
		Files.writeString(source.toPath(), "copy this");
		File dest = tempDir.resolve("dest.txt").toFile();
		try (InputStream is = Files.newInputStream(source.toPath())) {
			FileUtils.copy(is, dest);
		}
		assertEquals("copy this", Files.readString(dest.toPath()));
	}

	@Test
	public void testCopyFolder(@TempDir Path tempDir) throws Exception {
		File sourceDir = tempDir.resolve("source").toFile();
		File destDir = tempDir.resolve("dest").toFile();
		sourceDir.mkdir();
		File file = new File(sourceDir, "file.txt");
		Files.writeString(file.toPath(), "folder copy");
		FileUtils.copyFolder(sourceDir, destDir);
		File copiedFile = new File(destDir, "file.txt");
		assertTrue(copiedFile.exists());
		assertEquals("folder copy", Files.readString(copiedFile.toPath()));
	}

	@Test
	public void testUnZipIt(@TempDir Path tempDir) throws Exception {
		InputStream zipStream = getClass().getResourceAsStream("/test.zip");
		assertNotNull(zipStream, "test.zip not found in resources");

		File outputDir = tempDir.resolve("output").toFile();
		outputDir.mkdir();

		FileUtils.unZipIt(zipStream, outputDir.getAbsolutePath());

		File extracted = new File(outputDir, "file.txt");
		assertTrue(extracted.exists());
		assertEquals("unzipped content", Files.readString(extracted.toPath()));
	}
}
