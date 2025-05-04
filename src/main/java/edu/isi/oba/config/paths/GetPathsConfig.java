package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.CONFIG_FLAG;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.Map;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetPathsConfig extends ConfigFlags {
	private Get_All get_all;
	private Get_By_Key get_by_key;

	/**
	 * Get the {@link Get_All} config (a sub-property within {@link GetPathsConfig}). We wrap it
	 * within an {@link Optional} because it may not exist in the config file.
	 *
	 * @return a {@link Get_All} parameterized {@link Optional}
	 */
	public Get_All getGet_all() {
		return this.get_all;
	}

	/**
	 * Set the {@link Get_All}, if it exists in the config file. This is the configuration for GET
	 * paths.
	 *
	 * @param {get_all} A {@link Get_All}
	 */
	public void setGet_all(Get_All get_all) {
		if (get_all == null) {
			this.get_all = new Get_All();
		} else {
			this.get_all = get_all;
		}

		this.configFlags.putAll(this.get_all.getConfigFlags());
	}

	/**
	 * Get the {@link Get_By_Key} config (a sub-property within {@link GetPathsConfig}). We wrap it
	 * within an {@link Optional} because it may not exist in the config file.
	 *
	 * @return a {@link GetPathsConfig} parameterized {@link Optional}
	 */
	public Get_By_Key getGet_by_key() {
		return get_by_key;
	}

	/**
	 * Set the {@link Get_By_Key}, if it exists in the config file. This is the configuration for GET
	 * paths.
	 *
	 * @param {get_by_key} A {@link Get_By_Key}
	 */
	public void setGet_by_key(Get_By_Key get_by_key) {
		if (get_by_key == null) {
			this.get_by_key = new Get_By_Key();
		} else {
			this.get_by_key = get_by_key;
		}

		this.configFlags.putAll(this.get_by_key.getConfigFlags());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Get_All extends ConfigFlags {
		Get_All() {
			configFlags.putAll(Map.ofEntries(Map.entry(CONFIG_FLAG.PATH_GET_ALL, true)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(CONFIG_FLAG.PATH_GET_ALL);
		}

		public void setEnable(Boolean enable) {
			this.configFlags.put(CONFIG_FLAG.PATH_GET_ALL, enable);
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Get_By_Key extends ConfigFlags {
		public String key_name;

		Get_By_Key() {
			// Default key name to be "id";
			this.key_name = "id";
			this.configFlags.putAll(
					Map.ofEntries(
							Map.entry(CONFIG_FLAG.PATH_GET_BY_ID, true),
							Map.entry(CONFIG_FLAG.PATH_GET_BY_ID_RESPONSE_ARRAY, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(CONFIG_FLAG.PATH_GET_BY_ID);
		}

		public void setEnable(Boolean enable) {
			this.configFlags.put(CONFIG_FLAG.PATH_GET_BY_ID, enable);
		}

		public Boolean getResponse_array() {
			return this.configFlags.get(CONFIG_FLAG.PATH_GET_BY_ID_RESPONSE_ARRAY);
		}

		public void setResponse_array(Boolean enable) {
			this.configFlags.put(CONFIG_FLAG.PATH_GET_BY_ID_RESPONSE_ARRAY, enable);
		}

		public String getKey_name() {
			return key_name;
		}

		public void setKey_name(String key_name) {
			if (key_name != null && !key_name.isBlank()) {
				this.key_name = key_name;
			}
		}
	}
}
