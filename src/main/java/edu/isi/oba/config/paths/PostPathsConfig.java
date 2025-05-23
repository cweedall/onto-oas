package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.POST_PATHS)
public final class PostPathsConfig {
	@JsonProperty(ConfigPropertyNames.POST_BULK)
	public final PostBulk postBulk = new PostBulk();

	@JsonProperty(ConfigPropertyNames.POST_SINGLE)
	public final PostSingle postSingle = new PostSingle();

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.POST_BULK)
	public static class PostBulk {
		// Default path suffix to be "_bulk";
		@JsonProperty(ConfigPropertyNames.PATH_SUFFIX)
		private String path_suffix = "_bulk";

		PostBulk() {
			GlobalFlags.setFlag(ConfigPropertyNames.POST_BULK_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.POST_BULK_ENABLE, enable);
			}
		}

		public String getPath_suffix() {
			return path_suffix;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.POST_SINGLE)
	public static class PostSingle {
		PostSingle() {
			GlobalFlags.setFlag(ConfigPropertyNames.POST_SINGLE_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.POST_SINGLE_ENABLE, enable);
			}
		}
	}
}
