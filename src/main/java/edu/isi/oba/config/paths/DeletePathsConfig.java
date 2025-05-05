package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class DeletePathsConfig extends ConfigFlags {
	private Delete_By_Key delete_by_key;

	/**
	 * Get the {@link Delete_By_Key} config (a sub-property within {@link DeletePathsConfig}).
	 *
	 * @return a {@link DeletePathsConfig}
	 */
	public Delete_By_Key getDelete_by_key() {
		return delete_by_key;
	}

	/**
	 * Set the {@link Delete_By_Key}, if it exists in the config file. This is the configuration for
	 * DELETE paths.
	 *
	 * @param {delete_by_key} A {@link Delete_By_Key}
	 */
	public void setDelete_by_key(Delete_By_Key delete_by_key) {
		if (delete_by_key == null) {
			this.delete_by_key = new Delete_By_Key();
		} else {
			this.delete_by_key = delete_by_key;
		}

		this.configFlags.putAll(this.delete_by_key.getConfigFlags());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Delete_By_Key extends ConfigFlags {
		// Default key name to be "id";
		public String key_name = "id";

		Delete_By_Key() {
			this.configFlags.putAll(Map.ofEntries(Map.entry(ConfigFlagType.PATH_DELETE_BY_ID, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(ConfigFlagType.PATH_DELETE_BY_ID);
		}

		public void setEnable(Boolean enable) {
			if (enable != null) {
				this.configFlags.put(ConfigFlagType.PATH_DELETE_BY_ID, enable);
			}
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
