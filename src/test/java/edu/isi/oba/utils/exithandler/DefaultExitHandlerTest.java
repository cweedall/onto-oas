package edu.isi.oba.utils.exithandler;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import org.junit.jupiter.api.Test;

class DefaultExitHandlerTest extends BaseTest {
	@Test
	void testImplementsExitHandler() {
		ExitHandler handler = new DefaultExitHandler();
		assertNotNull(handler);
		// We can't call handler.exit() directly without terminating the JVM
		// So we just verify the type
		assertTrue(handler instanceof ExitHandler);
	}
}
