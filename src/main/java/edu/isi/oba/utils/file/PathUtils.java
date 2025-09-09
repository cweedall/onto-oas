package edu.isi.oba.utils.file;

/**
 * Utility class for path-related operations. Provides methods to ensure consistent path formatting.
 */
public final class PathUtils {

	private PathUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Ensures the given path string ends with a trailing slash.
	 *
	 * @param path the input path string
	 * @return the path string with a trailing slash
	 */
	public static String checkTrailingSlash(String path) {
		return path.endsWith("/") ? path : path + "/";
	}
}
