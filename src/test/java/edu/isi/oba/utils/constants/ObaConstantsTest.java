package edu.isi.oba.utils.constants;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

public class ObaConstantsTest {

	@Test
	void testPrivateConstructor() throws Exception {
		Constructor<ObaConstants> constructor = ObaConstants.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
	}
}
