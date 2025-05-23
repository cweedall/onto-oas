package edu.isi.oba.config.flags;

import java.util.HashMap;
import java.util.Map;

public class GlobalFlags {

	// Declare a static HashMap to store boolean values globally
	private static Map<String, Boolean> booleanMap = new HashMap<>();

	// Method to add or update a value in the map
	public static void setFlag(String key, Boolean value) {
		booleanMap.put(key, value == null ? false : value);
	}

	// Method to retrieve a value from the map
	public static Boolean getFlag(String key) {
		return booleanMap.containsKey(key) ? booleanMap.get(key) : false;
	}

	// Method to check if the map contains a key
	public static Boolean containsKey(String key) {
		return booleanMap.containsKey(key);
	}
}
