package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PathConfig extends ConfigFlags {
	private GetPathsConfig get_paths;

	public PathConfig() {
		this.configFlags.putAll(
				Map.ofEntries(
						Map.entry(ConfigFlagType.DISABLE_ALL_PATHS, false),
						Map.entry(ConfigFlagType.PATH_DELETE, false),
						Map.entry(ConfigFlagType.PATH_PATCH, false),
						Map.entry(ConfigFlagType.PATH_POST, false),
						Map.entry(ConfigFlagType.PATH_PUT, false),
						Map.entry(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES, true),
						Map.entry(ConfigFlagType.USE_KEBAB_CASE_PATHS, false)));
	}

	public Boolean getDisable_all_paths() {
		return this.configFlags.get(ConfigFlagType.DISABLE_ALL_PATHS);
	}

	public void setDisable_all_paths(Boolean disable_all_paths) {
		this.configFlags.put(ConfigFlagType.DISABLE_ALL_PATHS, disable_all_paths);
	}

	public Boolean getEnable_patch_paths() {
		return this.configFlags.get(ConfigFlagType.PATH_PATCH);
	}

	public void setEnable_patch_paths(Boolean enable_patch_paths) {
		this.configFlags.put(ConfigFlagType.PATH_PATCH, enable_patch_paths);
	}

	public Boolean getEnable_post_paths() {
		return this.configFlags.get(ConfigFlagType.PATH_POST);
	}

	public void setEnable_post_paths(Boolean enable_post_paths) {
		this.configFlags.put(ConfigFlagType.PATH_POST, enable_post_paths);
	}

	public Boolean getEnable_put_paths() {
		return this.configFlags.get(ConfigFlagType.PATH_PUT);
	}

	public void setEnable_put_paths(Boolean enable_put_paths) {
		this.configFlags.put(ConfigFlagType.PATH_PUT, enable_put_paths);
	}

	public Boolean getEnable_delete_paths() {
		return this.configFlags.get(ConfigFlagType.PATH_DELETE);
	}

	public void setEnable_delete_paths(Boolean enable_delete_paths) {
		this.configFlags.put(ConfigFlagType.PATH_DELETE, enable_delete_paths);
	}

	public Boolean getUse_common_default_path_responses() {
		return this.configFlags.get(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES);
	}

	public void setUse_common_default_path_responses(Boolean use_common_default_path_responses) {
		this.configFlags.put(
				ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES, use_common_default_path_responses);
	}

	public Boolean getUse_kebab_case_paths() {
		return this.configFlags.get(ConfigFlagType.USE_KEBAB_CASE_PATHS);
	}

	public void setUse_kebab_case_paths(Boolean use_kebab_case_paths) {
		this.configFlags.put(ConfigFlagType.USE_KEBAB_CASE_PATHS, use_kebab_case_paths);
	}

	/**
	 * Get the {@link GetPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link GetPathsConfig}
	 */
	public GetPathsConfig getGet_paths() {
		return this.get_paths;
	}

	/**
	 * Set the GetPathsConfig, if it exists in the config file. This is the configuration for GET
	 * paths.
	 *
	 * @param {get_paths} A {@link GetPathsConfig}
	 */
	public void setGet_paths(GetPathsConfig get_paths) {
		if (get_paths == null) {
			this.get_paths = new GetPathsConfig();
		} else {
			this.get_paths = get_paths;
		}

		this.configFlags.putAll(this.get_paths.getConfigFlags());
	}
}
