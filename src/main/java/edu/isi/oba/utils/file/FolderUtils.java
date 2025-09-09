package edu.isi.oba.utils.file;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility class for recursively copying folders and their contents. Uses
 * java.nio.file.Files.walkFileTree for robust folder copying.
 */
public final class FolderUtils {

	private FolderUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Recursively copies the contents of the source folder to the destination folder.
	 *
	 * @param source the source folder path
	 * @param destination the destination folder path
	 * @throws IOException if an I/O error occurs
	 */
	public static void copyFolder(Path source, Path destination) throws IOException {
		Files.walkFileTree(
				source,
				new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
							throws IOException {
						Path targetDir = destination.resolve(source.relativize(dir));
						Files.createDirectories(targetDir);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						Path targetFile = destination.resolve(source.relativize(file));
						Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
	}
}
