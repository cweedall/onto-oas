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

	static class TestExitHandler implements ExitHandler {
		int status = -1;

		@Override
		public void exit(int status) {
			this.status = status;
		}
	}

	private TestExitHandler testHandler;

	@BeforeEach
	void setUp() {
		testHandler = new TestExitHandler();
		FatalErrorHandler.setExitHandlerForTesting(testHandler);
	}

	@Test
	void testFatalWithMessage() {
		FatalErrorHandler.fatal("Something went wrong");
		assertEquals(1, testHandler.status);
	}

	@Test
	void testFatalWithMessageAndCause() {
		FatalErrorHandler.fatal("Something went wrong", new RuntimeException("Cause"));
		assertEquals(1, testHandler.status);
	}

	@Test
	void testSetExitHandlerIgnoresNull() {
		FatalErrorHandler.setExitHandlerForTesting(null); // should not overwrite
		FatalErrorHandler.fatal("Still exits");
		assertEquals(1, testHandler.status);
	}
}
