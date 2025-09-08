package edu.isi.oba.utils.file;

import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

/**
 * Utility class for extracting ZIP archives. Uses Apache Commons Compress to prevent Zip Slip
 * vulnerabilities.
 */
public final class ZipUtils {

	private ZipUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Extracts the contents of a ZIP archive from the given input stream to the specified output
	 * folder.
	 *
	 * @param zipStream the input stream of the ZIP archive
	 * @param outputFolder the destination folder for extracted files
	 * @throws Exception if an error occurs during extraction
	 */
	public static void unZipIt(InputStream zipStream, String outputFolder) throws Exception {
		try (ZipArchiveInputStream zis = new ZipArchiveInputStream(zipStream)) {
			ArchiveEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(outputFolder, entry.getName());
				Path normalizedPath = newFile.toPath().normalize();
				Path outputPath = Path.of(outputFolder).toAbsolutePath().normalize();

				if (!normalizedPath.startsWith(outputPath)) {
					FatalErrorHandler.fatal(
							"Bad zip entry. Possibly malicious. Exiting to avoid 'Zip Slip'.");
				}

				if (entry.isDirectory()) {
					newFile.mkdirs();
				} else {
					File parent = newFile.getParentFile();
					if (!parent.exists()) {
						parent.mkdirs();
					}

					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						byte[] buffer = new byte[8192];
						int len;
						while ((len = zis.read(buffer)) != -1) {
							fos.write(buffer, 0, len);
						}
					}
				}
			}
		}
	}
}
