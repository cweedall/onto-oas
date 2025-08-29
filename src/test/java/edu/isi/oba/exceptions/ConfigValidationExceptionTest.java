package edu.isi.oba.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import org.junit.jupiter.api.Test;

class ConfigValidationExceptionTest extends BaseTest {

	@Test
	void testConstructorWithMessage() {
		String message = "Invalid config";
		ConfigValidationException ex = new ConfigValidationException(message);
		assertEquals(message, ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Invalid config";
		Throwable cause = new RuntimeException("Root cause");
		ConfigValidationException ex = new ConfigValidationException(message, cause);
		assertEquals(message, ex.getMessage());
		assertEquals(cause, ex.getCause());
	}
}
