package edu.isi.oba.config.flags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe utility class for managing global {@link Boolean} flags using a static {@link Map}.
 * This class allows setting, retrieving, checking, clearing, and bulk updating flags identified by
 * string keys.
 *
 * <p>This class is intended to be used during configuration loading (e.g., via Jackson) and in unit
 * tests where global flags may need to be reset or overridden.
 *
 * <p><strong>Thread Safety:</strong> This implementation uses {@link ConcurrentHashMap} to support
 * concurrent access.
 */
public class GlobalFlags {

	/**
	 * A static {@link Map} to store {@link Boolean} flags globally. Keys are {@link String}s, and
	 * values are {@link Boolean}s.
	 */
	private static Map<String, Boolean> booleanMap = new ConcurrentHashMap<>();

	/**
	 * Sets or updates a boolean flag in the global map.
	 *
	 * @param key the name of the flag
	 * @param value the {@link Boolean} value to associate with the key; if {@code null}, the value is
	 *     treated as {@code false}
	 */
	public static void setFlag(String key, Boolean value) {
		booleanMap.put(key, value == null ? false : value);
	}

	/**
	 * Retrieves the value of a boolean flag.
	 *
	 * @param key the name of the flag
	 * @return the {@link Boolean} value associated with the key, or {@code false} if the key is not
	 *     present
	 */
	public static Boolean getFlag(String key) {
		return booleanMap.containsKey(key) ? booleanMap.get(key) : false;
	}

	/**
	 * Checks whether a flag with the given key exists in the {@link Map}.
	 *
	 * @param key the name of the flag
	 * @return {@code true} if the key exists, {@code false} otherwise
	 */
	public static Boolean containsKey(String key) {
		return booleanMap.containsKey(key);
	}

	/**
	 * Clears all flags from the global {@link Map}.
	 *
	 * <p>This is useful for resetting state during unit tests or reinitializing configuration.
	 */
	public static void clearFlags() {
		booleanMap.clear();
	}

	/**
	 * Bulk updates the global flags using the provided map.
	 *
	 * <p>Existing flags are cleared before the new ones are added.
	 *
	 * @param flags a {@link Map} of {@link String} keys to {@link Boolean} values to populate the
	 *     global flag map
	 */
	public static void setFlags(Map<String, Boolean> flags) {
		booleanMap.clear();
		for (final var entry : flags.entrySet()) {
			setFlag(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Returns an unmodifiable snapshot of the current global flags.
	 *
	 * <p>This is useful for inspection or logging without exposing the internal map to modification.
	 *
	 * @return an unmodifiable copy of the current flag {@link Map}
	 */
	public static Map<String, Boolean> getFlagsSnapshot() {
		return Map.copyOf(booleanMap);
	}
}
