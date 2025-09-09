package edu.isi.oba.utils.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for copying data from InputStreams to files. Uses java.nio.file.Files for safe
 * stream copying.
 */
public final class StreamUtils {

	private StreamUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Copies the contents of the input stream to the destination file.
	 *
	 * @param inputStream the input stream to copy from
	 * @param destination the destination file path
	 * @throws IOException if an I/O error occurs
	 */
	public static void copy(InputStream inputStream, Path destination) throws IOException {
		Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
	}
}
