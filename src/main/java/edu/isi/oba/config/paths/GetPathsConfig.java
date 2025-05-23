package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.GET_PATHS)
public final class GetPathsConfig {
	@JsonProperty(ConfigPropertyNames.GET_ALL)
	public final GetAll getAll = new GetAll();

	@JsonProperty(ConfigPropertyNames.GET_BY_KEY)
	public final GetByKey getByKey = new GetByKey();

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.GET_ALL)
	public static class GetAll {
		GetAll() {
			GlobalFlags.setFlag(ConfigPropertyNames.GET_ALL_ENABLE, true);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.GET_ALL_ENABLE, enable);
			}
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.GET_BY_KEY)
	public static class GetByKey {
		// Default key name to be "id";
		@JsonSetter(ConfigPropertyNames.KEY_NAME)
		private String keyName = "id";

		@JsonSetter(ConfigPropertyNames.KEY_NAME_IN_TEXT)
		private String keyNameInText = this.keyName;

		// Default key type to be STRING.  Used within the application for enum convenience.
		private PathKeyType keyType = PathKeyType.STRING;

		GetByKey() {
			GlobalFlags.setFlag(ConfigPropertyNames.GET_BY_KEY_ENABLE, true);
			GlobalFlags.setFlag(ConfigPropertyNames.GET_BY_KEY_RESPONSE_ARRAY_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.GET_BY_KEY_ENABLE, enable);
			}
		}

		public Boolean getResponse_array() {
			return GlobalFlags.getFlag(ConfigPropertyNames.GET_BY_KEY_RESPONSE_ARRAY_ENABLE);
		}

		@JsonSetter(ConfigPropertyNames.RESPONSE_ARRAY)
		private void setResponseArray(Boolean enable) {
			GlobalFlags.setFlag(ConfigPropertyNames.GET_BY_KEY_RESPONSE_ARRAY_ENABLE, enable);
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
