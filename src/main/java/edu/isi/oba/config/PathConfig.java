package edu.isi.oba.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PathConfig {
	private final Map<CONFIG_FLAG, Boolean> configFlags =
			new HashMap<>() {
				{
					put(CONFIG_FLAG.DISABLE_ALL_PATHS, false);
					put(CONFIG_FLAG.PATH_DELETE, false);
					put(CONFIG_FLAG.PATH_GET, true);
					put(CONFIG_FLAG.PATH_PATCH, false);
					put(CONFIG_FLAG.PATH_POST, false);
					put(CONFIG_FLAG.PATH_PUT, false);
					put(CONFIG_FLAG.USE_COMMON_DEFAULT_PATH_RESPONSES, true);
					put(CONFIG_FLAG.USE_KEBAB_CASE_PATHS, false);
				}
			};

	public Boolean getDisable_all_paths() {
		return this.configFlags.get(CONFIG_FLAG.DISABLE_ALL_PATHS);
	}

	public void setDisable_all_paths(Boolean disable_all_paths) {
		this.configFlags.put(CONFIG_FLAG.DISABLE_ALL_PATHS, disable_all_paths);
	}

	public Boolean getEnable_get_paths() {
		return this.configFlags.get(CONFIG_FLAG.PATH_GET);
	}

	public void setEnable_get_paths(Boolean enable_get_paths) {
		this.configFlags.put(CONFIG_FLAG.PATH_GET, enable_get_paths);
	}

	public Boolean getEnable_patch_paths() {
		return this.configFlags.get(CONFIG_FLAG.PATH_PATCH);
	}

	public void setEnable_patch_paths(Boolean enable_patch_paths) {
		this.configFlags.put(CONFIG_FLAG.PATH_PATCH, enable_patch_paths);
	}

	public Boolean getEnable_post_paths() {
		return this.configFlags.get(CONFIG_FLAG.PATH_POST);
	}

	public void setEnable_post_paths(Boolean enable_post_paths) {
		this.configFlags.put(CONFIG_FLAG.PATH_POST, enable_post_paths);
	}

	public Boolean getEnable_put_paths() {
		return this.configFlags.get(CONFIG_FLAG.PATH_PUT);
	}

	public void setEnable_put_paths(Boolean enable_put_paths) {
		this.configFlags.put(CONFIG_FLAG.PATH_PUT, enable_put_paths);
	}

	public Boolean getEnable_delete_paths() {
		return this.configFlags.get(CONFIG_FLAG.PATH_DELETE);
	}

	public void setEnable_delete_paths(Boolean enable_delete_paths) {
		this.configFlags.put(CONFIG_FLAG.PATH_DELETE, enable_delete_paths);
	}

	public Boolean getUse_common_default_path_responses() {
		return this.configFlags.get(CONFIG_FLAG.USE_COMMON_DEFAULT_PATH_RESPONSES);
	}

	public void setUse_common_default_path_responses(Boolean use_common_default_path_responses) {
		this.configFlags.put(
				CONFIG_FLAG.USE_COMMON_DEFAULT_PATH_RESPONSES, use_common_default_path_responses);
	}

	public Boolean getUse_kebab_case_paths() {
		return this.configFlags.get(CONFIG_FLAG.USE_KEBAB_CASE_PATHS);
	}

	public void setUse_kebab_case_paths(Boolean use_kebab_case_paths) {
		this.configFlags.put(CONFIG_FLAG.USE_KEBAB_CASE_PATHS, use_kebab_case_paths);
	}

	/**
	 * Get the value of a particular configuration flag.
	 *
	 * @param {flag} the configuration flag name
	 * @return The flag's value (true/false/null).
	 */
	public Boolean getConfigFlagValue(CONFIG_FLAG flag) {
		return this.configFlags.get(flag);
	}

	/**
	 * Get map of all config flags and their values.
	 *
	 * @return Map of CONFIG_FLAGs and their Boolean value (true/false/null).
	 */
	public Map<CONFIG_FLAG, Boolean> getConfigFlags() {
		return this.configFlags;
	}
}
