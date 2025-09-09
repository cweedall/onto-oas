package edu.isi.oba.utils.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for writing content to files. Uses java.nio.file.Files for safe and efficient file
 * writing.
 */
public final class FileWriterUtils {

	private FileWriterUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Writes the given content to the specified file path.
	 *
	 * @param filePath the path to the file
	 * @param content the content to write
	 * @throws IOException if an I/O error occurs
	 */
	public static void writeFile(String filePath, String content) throws IOException {
		Files.write(Path.of(filePath), content.getBytes(StandardCharsets.UTF_8));
	}
}
