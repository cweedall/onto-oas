package edu.isi.oba.config.paths;

import java.util.HashMap;
import java.util.Map;

public enum PathKeyType {
	STRING("string"),
	NUMBER("number"),
	INTEGER("integer"),
	BOOLEAN("boolean");

	private final String key_label;
	private static final Map<String, PathKeyType> BY_LABEL = new HashMap<>();

	static {
		for (PathKeyType e : values()) {
			BY_LABEL.put(e.key_label, e);
		}
	}

	private PathKeyType(String key_label) {
		this.key_label = key_label;
	}

	public static PathKeyType valueOfLabel(String key_label) {
		return BY_LABEL.get(key_label);
	}

	@Override
	public String toString() {
		return this.key_label;
	}
}
