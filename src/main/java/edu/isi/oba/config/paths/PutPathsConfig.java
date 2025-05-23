package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.PUT_PATHS)
public final class PutPathsConfig {
	@JsonProperty(ConfigPropertyNames.PUT_BULK)
	public final PutBulk putBulk = new PutBulk();

	@JsonProperty(ConfigPropertyNames.PUT_BY_KEY)
	public final PutByKey putByKey = new PutByKey();

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.PUT_BULK)
	public static class PutBulk {
		// Default path suffix to be "_bulk";
		@JsonProperty(ConfigPropertyNames.PATH_SUFFIX)
		private String pathSuffix = "_bulk";

		PutBulk() {
			GlobalFlags.setFlag(ConfigPropertyNames.PUT_BULK_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.PUT_BULK_ENABLE, enable);
			}
		}

		public String getPathSuffix() {
			return pathSuffix;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.PUT_BY_KEY)
	public static class PutByKey {
		// Default key name to be "id";
		@JsonProperty(ConfigPropertyNames.KEY_NAME)
		private String keyName = "id";

		@JsonProperty(ConfigPropertyNames.KEY_NAME_IN_TEXT)
		private String keyNameInText = this.keyName;

		// Default key type to be STRING.  Used within the application for enum convenience.
		private PathKeyType keyType = PathKeyType.STRING;

		PutByKey() {
			GlobalFlags.setFlag(ConfigPropertyNames.PUT_BY_KEY_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.PUT_BY_KEY_ENABLE, enable);
			}
		}

		public String getKeyName() {
			return keyName;
		}

		public String getKeyNameInText() {
			return this.keyNameInText;
		}

		public PathKeyType getKeyType() {
			return this.keyType;
		}

		@JsonSetter(ConfigPropertyNames.KEY_TYPE)
		private void setKeyType(String keyType) {
			if (keyType != null && !keyType.isBlank()) {
				this.keyType = PathKeyType.valueOfLabel(keyType);
			}
		}
	}
}
