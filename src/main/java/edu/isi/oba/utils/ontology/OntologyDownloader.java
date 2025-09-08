package edu.isi.oba.utils.ontology;

import edu.isi.oba.exceptions.OntologyDownloadException;
import edu.isi.oba.utils.constants.ObaConstants;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for downloading ontologies via HTTP with content negotiation and redirect handling.
 */
public class OntologyDownloader {
	private static final Logger logger = Logger.getLogger(OntologyDownloader.class.getName());
	private static final int MAX_REDIRECTS = 5;
	private static final Set<Integer> REDIRECT_CODES =
			Set.of(
					HttpURLConnection.HTTP_MOVED_TEMP,
					HttpURLConnection.HTTP_MOVED_PERM,
					HttpURLConnection.HTTP_SEE_OTHER);

	private OntologyDownloader() {
		throw new UnsupportedOperationException("Static utility class");
	}

	public static void downloadOntology(String uri, String downloadPath)
			throws OntologyDownloadException {
		downloadOntology(uri, downloadPath, OntologyDownloader::openConnectionWithFallback);
	}

	public static void downloadOntology(
			String uri, String downloadPath, Function<String, HttpURLConnection> connectionFactory)
			throws OntologyDownloadException {

		for (final var serialization : ObaConstants.POSSIBLE_VOCAB_SERIALIZATIONS) {
			logger.log(Level.FINE, "Attempting to download vocabulary in " + serialization);
			try {
				final var url = new URL(uri);
				var connection = connectionFactory.apply(uri);
				connection.setRequestMethod("GET");
				connection.setInstanceFollowRedirects(true);
				connection.setRequestProperty("Accept", serialization);
				connection = followRedirects(connection, serialization, connectionFactory);
				final var in = connection.getInputStream();
				Files.copy(in, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING);
				in.close();
				// break;
			} catch (IOException | RuntimeException e) {
				Throwable cause =
						e instanceof RuntimeException && e.getCause() instanceof IOException ? e.getCause() : e;
				final var message = "Failed to download vocabulary in RDF format [" + serialization + "]: ";
				logger.severe(message + cause.toString());
				throw new OntologyDownloadException(message, cause);
			}
		}
	}

	static boolean isRedirect(int status) {
		return REDIRECT_CODES.contains(status);
	}

	static HttpURLConnection followRedirects(
			HttpURLConnection connection,
			String serialization,
			Function<String, HttpURLConnection> connectionFactory)
			throws IOException {
		int status = connection.getResponseCode();
		int redirectCount = 0;
		while (isRedirect(status) && redirectCount++ < MAX_REDIRECTS) {
			final var newUrl = connection.getHeaderField("Location");
			logger.log(Level.FINE, "Redirecting to: " + newUrl);
			connection = connectionFactory.apply(newUrl);
			connection.setRequestProperty("Accept", serialization);
			status = connection.getResponseCode();
		}
		if (redirectCount >= MAX_REDIRECTS) {
			throw new IOException("Too many redirects while downloading ontology.");
		}
		return connection;
	}

	static HttpURLConnection followRedirects(HttpURLConnection connection, String serialization)
			throws IOException {
		return followRedirects(
				connection, serialization, OntologyDownloader::openConnectionWithFallback);
	}

	static HttpURLConnection openConnectionWithFallback(String urlStr) {
		try {
			return (HttpURLConnection) new URL(urlStr).openConnection();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
