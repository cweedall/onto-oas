package edu.isi.oba.config.flags;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalFlagsTest extends BaseTest {

	@Test
	void testPrivateConstructor() throws Exception {
		Constructor<GlobalFlags> constructor = GlobalFlags.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
	}

	@BeforeEach
	void setUp() {
		GlobalFlags.clearFlags();
	}

	@Test
	void testSetAndGetFlag() {
		GlobalFlags.setFlag("featureX", true);
		assertTrue(GlobalFlags.getFlag("featureX"));

		GlobalFlags.setFlag("featureY", false);
		assertFalse(GlobalFlags.getFlag("featureY"));

		GlobalFlags.setFlag("featureZ", null);
		assertFalse(GlobalFlags.getFlag("featureZ"));
	}

	@Test
	void testContainsKey() {
		GlobalFlags.setFlag("debugMode", true);
		assertTrue(GlobalFlags.containsKey("debugMode"));
		assertFalse(GlobalFlags.containsKey("nonexistent"));
	}

	@Test
	void testClearFlags() {
		GlobalFlags.setFlag("flag1", true);
		GlobalFlags.setFlag("flag2", false);
		GlobalFlags.clearFlags();
		assertFalse(GlobalFlags.containsKey("flag1"));
		assertFalse(GlobalFlags.containsKey("flag2"));
	}

	@Test
	void testSetFlags() {
		Map<String, Boolean> flags = new HashMap<>();
		flags.put("alpha", true);
		flags.put("beta", false);
		flags.put("gamma", null);

		GlobalFlags.setFlags(flags);

		assertTrue(GlobalFlags.containsKey("alpha"));
		assertTrue(GlobalFlags.getFlag("alpha"));
		assertFalse(GlobalFlags.getFlag("beta"));
		assertFalse(GlobalFlags.getFlag("gamma"));
	}

	@Test
	void testGetFlagsSnapshot() {
		GlobalFlags.setFlag("snapshotFlag", true);
		Map<String, Boolean> snapshot = GlobalFlags.getFlagsSnapshot();
		assertTrue(snapshot.containsKey("snapshotFlag"));
		assertTrue(snapshot.get("snapshotFlag"));

		// Ensure snapshot is unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> snapshot.put("newFlag", false));
	}

	@Test
	void testEmptySnapshotAfterClear() {
		GlobalFlags.setFlag("tempFlag", true);
		GlobalFlags.clearFlags();
		Map<String, Boolean> snapshot = GlobalFlags.getFlagsSnapshot();
		assertTrue(snapshot.isEmpty());
	}

	@Test
	void testNullAndEmptyKeys() {
		// Null key handling (may throw NullPointerException depending on usage)
		assertThrows(NullPointerException.class, () -> GlobalFlags.setFlag(null, true));
		assertThrows(NullPointerException.class, () -> GlobalFlags.getFlag(null));
		assertThrows(NullPointerException.class, () -> GlobalFlags.containsKey(null));

		// Empty string key
		GlobalFlags.setFlag("", true);
		assertTrue(GlobalFlags.getFlag(""));
		assertTrue(GlobalFlags.containsKey(""));
	}

	@Test
	void testMissingKeys() {
		GlobalFlags.clearFlags();
		assertFalse(GlobalFlags.containsKey("missing"));
		assertFalse(GlobalFlags.getFlag("missing")); // triggers the false branch
	}
}
