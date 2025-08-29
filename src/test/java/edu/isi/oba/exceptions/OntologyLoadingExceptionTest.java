package edu.isi.oba.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import org.junit.jupiter.api.Test;

class OntologyLoadingExceptionTest extends BaseTest {

	@Test
	void testConstructorWithMessage() {
		String message = "Failed to load ontology";
		OntologyLoadingException ex = new OntologyLoadingException(message);
		assertEquals(message, ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Failed to load ontology";
		Throwable cause = new RuntimeException("Root cause");
		OntologyLoadingException ex = new OntologyLoadingException(message, cause);
		assertEquals(message, ex.getMessage());
		assertEquals(cause, ex.getCause());
	}
}
