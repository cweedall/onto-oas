package edu.isi.oba.utils.exithandler;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FatalErrorHandlerTest extends BaseTest {

	@Test
	void testPrivateConstructor() throws Exception {
		Constructor<FatalErrorHandler> constructor = FatalErrorHandler.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
	}

	private TestExitHandler testHandler;

	@BeforeEach
	void setUp() {
		testHandler = new TestExitHandler();
		FatalErrorHandler.setExitHandlerForTesting(testHandler);
	}

	@Test
	void testFatalWithMessage() {
		assertThrows(RuntimeException.class, () -> FatalErrorHandler.fatal("Something went wrong"));
		assertEquals(1, testHandler.status);
	}

	@Test
	void testFatalWithMessageAndCause() {
		assertThrows(
				RuntimeException.class,
				() -> FatalErrorHandler.fatal("Something went wrong", new RuntimeException("Cause")));
		assertEquals(1, testHandler.status);
	}

	@Test
	void testSetExitHandlerIgnoresNull() {
		FatalErrorHandler.setExitHandlerForTesting(null); // should not overwrite
		assertThrows(RuntimeException.class, () -> FatalErrorHandler.fatal("Still exits"));
		assertEquals(1, testHandler.status);
	}

	@Test
	void testFatalWithMessage_directCall() {
		FatalErrorHandler.setExitHandlerForTesting(testHandler);
		try {
			FatalErrorHandler.fatal("Something went wrong");
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals(1, testHandler.status);
		}
	}
}
