package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.SEARCH_PATHS)
public final class SearchPathsConfig {
	@JsonProperty(ConfigPropertyNames.SEARCH_BY_POST)
	public final SearchByPost searchByPost = new SearchByPost();

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonRootName(ConfigPropertyNames.SEARCH_BY_POST)
	public static class SearchByPost {
		// Default path suffix to be "_search";
		@JsonProperty(ConfigPropertyNames.PATH_SUFFIX)
		private String pathSuffix = "_search";

		@JsonProperty(ConfigPropertyNames.SEARCH_PROPERTIES)
		private List<String> searchProperties = new ArrayList<>();

		private List<PathKeyType> searchPropertyTypes = new ArrayList<>();

		SearchByPost() {
			GlobalFlags.setFlag(ConfigPropertyNames.SEARCH_BY_POST_ENABLE, false);
		}

		@JsonSetter(ConfigPropertyNames.ENABLE)
		private void setEnable(Boolean enable) {
			if (enable != null) {
				GlobalFlags.setFlag(ConfigPropertyNames.SEARCH_BY_POST_ENABLE, enable);
			}
		}

		public String getPathSuffix() {
			return pathSuffix;
		}

		public List<String> getSearchProperties() {
			return this.searchProperties;
		}

		public List<PathKeyType> getSearchPropertyTypes() {
			return this.searchPropertyTypes;
		}

		@JsonSetter(ConfigPropertyNames.SEARCH_PROPERTY_TYPES)
		public void setSearchPropertyTypes(List<String> searchPropertyTypes) {
			if (searchPropertyTypes != null && !searchPropertyTypes.isEmpty()) {
				searchPropertyTypes.forEach(
						(datatype) -> {
							this.searchPropertyTypes.add(PathKeyType.valueOfLabel(datatype));
						});
			}
		}
	}
}
