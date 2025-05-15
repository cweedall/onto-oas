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
		private String key_name = "id";
		private String key_name_in_text = this.key_name;

		// Default key type to be STRING.  Used within the application for enum convenience.
		private PathKeyType key_type = PathKeyType.STRING;
		// Default key type to be "string".  Used as a convenience for setting the values from the
		// configuration file.
		private String key_datatype = this.key_type.toString();

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
			return this.key_name;
		}

		public void setKey_name(String key_name) {
			if (key_name != null && !key_name.isBlank()) {
				this.key_name = key_name;
			}
		}

		public String getKey_name_in_text() {
			return this.key_name_in_text;
		}

		public void setKey_name_in_text(String key_name_in_text) {
			if (key_name_in_text != null && !key_name_in_text.isBlank()) {
				this.key_name_in_text = key_name_in_text;
			}
		}

		public PathKeyType getKey_type() {
			return this.key_type;
		}

		public String getKey_datatype() {
			return this.key_datatype;
		}

		public void setKey_datatype(String key_datatype) {
			if (key_datatype != null && !key_datatype.isBlank()) {
				this.key_type = PathKeyType.valueOfLabel(key_datatype);
				this.key_datatype = key_datatype;
			}
		}
	}
}
