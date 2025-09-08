package edu.isi.oba.utils.file;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PathUtils}. Verifies trailing slash logic. */
public class PathUtilsTest {

	@Test
	void shouldThrowException_whenInstantiatingUtilityClass() throws Exception {
		Constructor<PathUtils> constructor = PathUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void shouldAppendSlash_whenMissing() {
		String input = "folder";
		String expected = "folder/";
		assertEquals(expected, PathUtils.checkTrailingSlash(input));
	}

	@Test
	public void shouldReturnSameString_whenSlashExists() {
		String input = "folder/";
		assertEquals(input, PathUtils.checkTrailingSlash(input));
	}
}
