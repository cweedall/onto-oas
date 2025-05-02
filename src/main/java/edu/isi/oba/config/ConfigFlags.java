package edu.isi.oba.config;

import java.util.HashMap;
import java.util.Map;

abstract class ConfigFlags {
	// Map of configuration flags and their Boolean values.
	protected final Map<CONFIG_FLAG, Boolean> configFlags = new HashMap<>();

	/**
	 * Set map of all configuration flags and their Boolean values.
	 *
	 * @param {configFlags} A map of configuration flags and their Boolean values.
	 */
	protected final void setConfigFlags(Map<CONFIG_FLAG, Boolean> configFlags) {
		this.configFlags.clear();
		this.configFlags.putAll(configFlags);
	}

	/**
	 * Set a single configuration flag and its Boolean value.
	 *
	 * @param {flag} A configuration flag
	 * @param {value} Boolean value of the flag
	 */
	protected final void setConfigFlagValue(CONFIG_FLAG flag, Boolean value) {
		this.configFlags.put(flag, value);
	}

	/**
	 * Add map of configuration flags and their Boolean values to the existing map.
	 *
	 * @param configFlags A map of configuration flags and their Boolean values.
	 */
	protected final void addAllConfigFlags(Map<CONFIG_FLAG, Boolean> configFlags) {
		this.configFlags.clear();
		this.configFlags.putAll(configFlags);
	}

	/**
	 * Get the value of a particular configuration flag.
	 *
	 * @param {flag} the configuration flag name
	 * @return The flag's Boolean value (true/false/null).
	 */
	public final Boolean getConfigFlagValue(CONFIG_FLAG flag) {
		if (this.configFlags.containsKey(flag)) {
			return this.configFlags.get(flag);
		} else {
			return false;
		}
	}

	/**
	 * Get map of all configuration flags and their Boolean values.
	 *
	 * @return Map of configuration flags and their Boolean values.
	 */
	public final Map<CONFIG_FLAG, Boolean> getConfigFlags() {
		return this.configFlags;
	}
}
