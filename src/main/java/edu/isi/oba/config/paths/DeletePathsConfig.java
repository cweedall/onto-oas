package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.DELETE_PATHS)
public final class DeletePathsConfig {
	@JsonProperty(ConfigPropertyNames.DELETE_BY_KEY)
	public final DeleteByKey deleteByKey = new DeleteByKey();

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.DELETE_BY_KEY)
	public static class DeleteByKey {
		// Default key name to be "id";
		@JsonProperty(ConfigPropertyNames.KEY_NAME)
		private String keyName = "id";

		@JsonProperty(ConfigPropertyNames.KEY_NAME_IN_TEXT)
		private String keyNameInText = this.keyName;

		// Default key type to be STRING.  Used within the application for enum convenience.
		private PathKeyType keyType = PathKeyType.STRING;

		DeleteByKey() {
			GlobalFlags.setFlag(ConfigPropertyNames.DELETE_BY_KEY_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.DELETE_BY_KEY_ENABLE, enable);
			}
		}

		public String getKeyName() {
			return this.keyName;
		}

		public String getKeyNameInText() {
			return this.keyNameInText;
		}

		public PathKeyType getKeyType() {
			return this.keyType;
		}

		@JsonSetter(ConfigPropertyNames.KEY_TYPE)
		private void setKeyDatatype(String keyType) {
			if (keyType != null && !keyType.isBlank()) {
				this.keyType = PathKeyType.valueOfLabel(keyType);
			}
		}
	}
}
