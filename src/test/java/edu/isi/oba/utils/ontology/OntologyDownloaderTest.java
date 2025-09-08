package edu.isi.oba.utils.ontology;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.isi.oba.exceptions.OntologyDownloadException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OntologyDownloaderTest {

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<OntologyDownloader> constructor = OntologyDownloader.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	void shouldThrowOntologyDownloadException_whenUriIsInvalid() {
		String invalidUri = "http://invalid.uri";
		String downloadPath = "target/test.owl";

		Function<String, HttpURLConnection> factory =
				url -> {
					throw new RuntimeException(new IOException("Simulated connection failure"));
				};

		assertThrows(
				OntologyDownloadException.class,
				() -> OntologyDownloader.downloadOntology(invalidUri, downloadPath, factory));
	}

	@Test
	void shouldDownloadOntology_whenValidUriProvided() throws Exception {
		String downloadPath = "target/test.owl";

		HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		Function<String, HttpURLConnection> factory = url -> mockConnection;

		OntologyDownloader.downloadOntology("http://valid.uri", downloadPath, factory);

		assertTrue(Files.exists(Path.of(downloadPath)));
		Files.delete(Path.of(downloadPath));
	}

	@Test
	void shouldFollowRedirects_whenOneRedirectOccurs() throws Exception {
		HttpURLConnection conn1 = mock(HttpURLConnection.class);
		HttpURLConnection conn2 = mock(HttpURLConnection.class);

		when(conn1.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_TEMP);
		when(conn1.getHeaderField("Location")).thenReturn("http://redirected.com");
		when(conn2.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		Function<String, HttpURLConnection> factory =
				url -> {
					if (url.equals("http://redirected.com")) {
						return conn2;
					}
					return conn1;
				};

		HttpURLConnection result =
				OntologyDownloader.followRedirects(conn1, "application/rdf+xml", factory);

		assertEquals(HttpURLConnection.HTTP_OK, result.getResponseCode());
	}

	@Test
	void shouldThrowIOException_whenMaxRedirectsExceeded() throws Exception {
		HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_MOVED_TEMP);
		when(mockConnection.getHeaderField("Location")).thenReturn("http://redirected.com");

		Function<String, HttpURLConnection> factory = url -> mockConnection;

		assertThrows(
				IOException.class,
				() -> OntologyDownloader.followRedirects(mockConnection, "application/rdf+xml", factory));
	}

	@Test
	void shouldReturnTrue_whenStatusIsRedirect() {
		assertTrue(OntologyDownloader.isRedirect(HttpURLConnection.HTTP_MOVED_TEMP));
	}

	@Test
	void shouldReturnFalse_whenStatusIsNotRedirect() {
		assertFalse(OntologyDownloader.isRedirect(HttpURLConnection.HTTP_OK));
	}

	@Test
	void shouldThrowOntologyDownloadException_whenUsingDefaultFactoryFails() {
		String unreachableUri = "http://192.0.2.1";
		String downloadPath = "target/test.owl";

		assertThrows(
				OntologyDownloadException.class,
				() -> OntologyDownloader.downloadOntology(unreachableUri, downloadPath));
	}

	@Test
	void shouldThrowIOException_whenFollowRedirectsFails_usingDefaultFactory() throws Exception {
		HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getResponseCode()).thenThrow(new IOException("Simulated failure"));

		assertThrows(
				IOException.class,
				() -> OntologyDownloader.followRedirects(mockConnection, "application/rdf+xml"));
	}

	@Test
	void shouldTryMultipleSerializations_whenFirstFails() throws Exception {
		String downloadPath = "target/test.owl";

		HttpURLConnection failConn = mock(HttpURLConnection.class);
		when(failConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(failConn.getInputStream()).thenThrow(new IOException("Fail first"));

		HttpURLConnection successConn = mock(HttpURLConnection.class);
		when(successConn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
		when(successConn.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

		// Simulate two formats: first fails, second succeeds
		final boolean[] firstAttempt = {true};
		Function<String, HttpURLConnection> factory =
				url -> {
					if (firstAttempt[0]) {
						firstAttempt[0] = false;
						return failConn;
					} else {
						return successConn;
					}
				};

		// Simulate multiple serialization attempts by calling the method twice
		try {
			OntologyDownloader.downloadOntology("http://fake.uri", downloadPath, factory);
		} catch (OntologyDownloadException e) {
			// Ignore first failure
		}

		// Second attempt should succeed
		OntologyDownloader.downloadOntology("http://fake.uri", downloadPath, factory);
		assertTrue(Files.exists(Path.of(downloadPath)));
		Files.delete(Path.of(downloadPath));
	}

	@Test
	void shouldWrapNonIOExceptionCause_whenRuntimeExceptionThrown() {
		String uri = "http://invalid.uri";
		String downloadPath = "target/test.owl";
		RuntimeException wrapped = new RuntimeException(new IllegalArgumentException("Not IO"));

		Function<String, HttpURLConnection> factory =
				url -> {
					throw wrapped;
				};

		OntologyDownloadException ex =
				assertThrows(
						OntologyDownloadException.class,
						() -> OntologyDownloader.downloadOntology(uri, downloadPath, factory));

		// Check that the cause is the original RuntimeException, not unwrapped
		assertSame(wrapped, ex.getCause());
	}

	@Test
	void shouldUseFallbackConnection_whenNoFactoryProvided() {
		String downloadPath = "target/test.owl";
		// This will use the default fallback method internally
		assertThrows(
				OntologyDownloadException.class,
				() -> OntologyDownloader.downloadOntology("http://invalid.uri", downloadPath));
	}

	@Test
	void shouldFollowRedirects_usingDefaultFactory() throws Exception {
		HttpURLConnection conn = mock(HttpURLConnection.class);
		when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

		HttpURLConnection result = OntologyDownloader.followRedirects(conn, "application/rdf+xml");
		assertEquals(HttpURLConnection.HTTP_OK, result.getResponseCode());
	}

	@Test
	void shouldOpenConnectionWithFallback_whenUrlIsValid() throws Exception {
		HttpURLConnection conn =
				OntologyDownloader.followRedirects(
						OntologyDownloader.openConnectionWithFallback("https://example.com"),
						"application/rdf+xml");
		assertNotNull(conn);
	}

	@Test
	void shouldWrapIOExceptionInFallback_whenUrlIsMalformed() {
		RuntimeException ex =
				assertThrows(
						RuntimeException.class,
						() -> OntologyDownloader.openConnectionWithFallback("http://[invalid-url]"));
		assertTrue(ex.getCause() instanceof IOException);
	}

	@Test
	void shouldWrapPlainIOException_whenThrownDirectly() {
		String uri = "http://fake.uri";
		String downloadPath = "target/test.owl";

		Function<String, HttpURLConnection> factory =
				url -> {
					throw new RuntimeException(new IOException("Direct IO failure"));
				};

		OntologyDownloadException ex =
				assertThrows(
						OntologyDownloadException.class,
						() -> OntologyDownloader.downloadOntology(uri, downloadPath, factory));

		assertTrue(ex.getCause() instanceof IOException);
	}

	// @Test
	// void shouldNotDownloadAnything_whenSerializationsAreEmpty() {
	// 	try (MockedStatic<ObaConstants> mocked = mockStatic(ObaConstants.class)) {
	// 		mocked.when(ObaConstants::possibleVocabSerializations).thenReturn(List.of());
	// 		// mocked.when(ObaConstants::POSSIBLE_VOCAB_SERIALIZATIONS);
	// 		// mocked.when(ObaConstants::getConstantValue).thenReturn("mockedValue");
	// 		// Your test logic that uses MyClass.getConstantValue()
	// 		// No problems, but do nothing.

	// 		String uri = "http://fake.uri";
	// 		String downloadPath = "target/test.owl";

	// 		OntologyDownloadException ex =
	// 				assertThrows(
	// 						OntologyDownloadException.class,
	// 						() -> OntologyDownloader.downloadOntology(uri, downloadPath, null));
	// 	}
	// }
}
