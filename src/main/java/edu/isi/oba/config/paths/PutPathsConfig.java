package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class PutPathsConfig extends ConfigFlags {
	private Put_Bulk put_bulk;
	private Put_By_Key put_by_key;

	/**
	 * Get the {@link Put_Bulk} config (a sub-property within {@link PutPathsConfig}).
	 *
	 * @return a {@link Put_Bulk}
	 */
	public Put_Bulk getPut_bulk() {
		return this.put_bulk;
	}

	/**
	 * Set the {@link Put_Bulk}, if it exists in the config file. This is the configuration for PUT
	 * paths.
	 *
	 * @param {put_bulk} A {@link Put_Bulk}
	 */
	public void setPut_bulk(Put_Bulk put_bulk) {
		if (put_bulk == null) {
			this.put_bulk = new Put_Bulk();
		} else {
			this.put_bulk = put_bulk;
		}

		this.configFlags.putAll(this.put_bulk.getConfigFlags());
	}

	/**
	 * Get the {@link Put_By_Key} config (a sub-property within {@link PutPathsConfig}).
	 *
	 * @return a {@link PutPathsConfig}
	 */
	public Put_By_Key getPut_by_key() {
		return put_by_key;
	}

	/**
	 * Set the {@link Put_By_Key}, if it exists in the config file. This is the configuration for PUT
	 * paths.
	 *
	 * @param {put_by_key} A {@link Put_By_Key}
	 */
	public void setPut_by_key(Put_By_Key put_by_key) {
		if (put_by_key == null) {
			this.put_by_key = new Put_By_Key();
		} else {
			this.put_by_key = put_by_key;
		}

		this.configFlags.putAll(this.put_by_key.getConfigFlags());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Put_Bulk extends ConfigFlags {
		// Default path suffix to be "_bulk";
		private String path_suffix = "_bulk";

		Put_Bulk() {
			this.configFlags.putAll(Map.ofEntries(Map.entry(ConfigFlagType.PATH_PUT_BULK, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(ConfigFlagType.PATH_PUT_BULK);
		}

		public void setEnable(Boolean enable) {
			if (enable != null) {
				this.configFlags.put(ConfigFlagType.PATH_PUT_BULK, enable);
			}
		}

		public String getPath_suffix() {
			return path_suffix;
		}

		public void setPath_suffix(String path_suffix) {
			if (path_suffix != null && !path_suffix.isBlank()) {
				this.path_suffix = path_suffix;
			}
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Put_By_Key extends ConfigFlags {
		// Default key name to be "id";
		private String key_name = "id";
		private String key_name_in_text = this.key_name;

		// Default key type to be STRING.  Used within the application for enum convenience.
		private PathKeyType key_type = PathKeyType.STRING;
		// Default key type to be "string".  Used as a convenience for setting the values from the
		// configuration file.
		private String key_datatype = key_type.toString();

		Put_By_Key() {
			this.configFlags.putAll(Map.ofEntries(Map.entry(ConfigFlagType.PATH_PUT_BY_ID, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(ConfigFlagType.PATH_PUT_BY_ID);
		}

		public void setEnable(Boolean enable) {
			if (enable != null) {
				this.configFlags.put(ConfigFlagType.PATH_PUT_BY_ID, enable);
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
