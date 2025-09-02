package edu.isi.oba.utils.file;

import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
	private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

	public static String checkTrailingSlash(String string) {
		return string.endsWith("/") ? string : string + "/";
	}

	public static void writeFile(String file_path, String content) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file_path));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Code to unzip a file. Inspired from
	 * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/ Taken from
	 *
	 * @param resourceName
	 * @param outputFolder
	 */
	public static void unZipIt(InputStream zipStream, String outputFolder) {
		byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(zipStream);
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(outputFolder + File.separator + fileName);

				// Check whether bad or malicious entry exists in zip file.  Log an exit, if so.
				if (!newFile.toPath().normalize().startsWith(Path.of(outputFolder, File.separator))) {
					FatalErrorHandler.fatal(
							"Bad zip entry. Possibly malicious. Exiting to avoid 'Zip Slip'.");
				}

				File canonicalOutputDir = new File(outputFolder).getCanonicalFile();
				File canonicalDestFile = newFile.getCanonicalFile();
				if (!canonicalDestFile
						.getPath()
						.startsWith(canonicalOutputDir.getPath() + File.separator)) {
					FatalErrorHandler.fatal(
							"Bad zip entry. Possibly malicious. Exiting to avoid 'Zip Slip'.");
				}

				if (ze.isDirectory()) {
					new File(newFile.getAbsolutePath()).mkdirs();
				} else {
					File parentDir = newFile.getParentFile();
					if (parentDir != null && !parentDir.exists()) {
						parentDir.mkdirs();
					}
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Exception while unzipping: " + ex.getMessage());
		}
	}

	public static void copy(InputStream is, File dest) throws Exception {
		OutputStream os = null;
		try {
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while extracting the reosurces: " + e.getMessage());
			throw e;
		} finally {
			if (is != null) is.close();
			if (os != null) os.close();
		}
	}

	/**
	 * This function recursively copy all the sub folder and files from sourceFolder to
	 * destinationFolder
	 */
	public static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
		// Check if sourceFolder is a directory or file
		// If sourceFolder is file; then copy the file directly to new location
		if (sourceFolder.isDirectory()) {
			// Verify if destinationFolder is already present; If not then create it
			if (!destinationFolder.exists()) {
				destinationFolder.mkdir();
				System.out.println("Directory created :: " + destinationFolder);
			}

			// Get all files from source directory
			String files[] = sourceFolder.list();

			// Iterate over all files and copy them to destinationFolder one by one
			for (String file : files) {
				File srcFile = new File(sourceFolder, file);
				File destFile = new File(destinationFolder, file);

				// Recursive function call
				copyFolder(srcFile, destFile);
			}
		} else {
			// Copy the file content from one place to another
			Files.copy(
					sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("File copied :: " + destinationFolder);
		}
	}
}
