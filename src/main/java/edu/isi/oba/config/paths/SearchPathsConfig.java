package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class SearchPathsConfig extends ConfigFlags {
	private Search_By_Post search_by_post;

	/**
	 * Get the {@link Search_By_Post} config (a sub-property within {@link SearchPathsConfig}).
	 *
	 * @return a {@link Search_By_Post}
	 */
	public Search_By_Post getSearch_by_post() {
		return this.search_by_post;
	}

	/**
	 * Set the {@link Search_By_Post}, if it exists in the config file. This is the configuration for
	 * POST paths.
	 *
	 * @param {search_by_post} A {@link Search_By_Post}
	 */
	public void setSearch_by_post(Search_By_Post search_by_post) {
		if (search_by_post == null) {
			this.search_by_post = new Search_By_Post();
		} else {
			this.search_by_post = search_by_post;
		}

		this.configFlags.putAll(this.search_by_post.getConfigFlags());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Search_By_Post extends ConfigFlags {
		// Default path suffix to be "_search";
		private String path_suffix = "_search";
		private List<String> search_properties = new ArrayList<>();
		private List<PathKeyType> search_property_types = new ArrayList<>();
		private List<String> search_property_datatypes = new ArrayList<>();

		Search_By_Post() {
			configFlags.putAll(Map.ofEntries(Map.entry(ConfigFlagType.PATH_SEARCH_BY_POST, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(ConfigFlagType.PATH_SEARCH_BY_POST);
		}

		public void setEnable(Boolean enable) {
			if (enable != null) {
				this.configFlags.put(ConfigFlagType.PATH_SEARCH_BY_POST, enable);
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

		public List<String> getSearch_properties() {
			return this.search_properties;
		}

		public void setSearch_properties(List<String> search_properties) {
			this.search_properties = search_properties;
			if (search_properties != null && !search_properties.isEmpty()) {
				this.search_properties = search_properties;
			}
		}

		public List<PathKeyType> getSearch_property_types() {
			return this.search_property_types;
		}

		public List<String> getKey_datatype() {
			return this.search_property_datatypes;
		}

		public void setSearch_property_datatypes(List<String> search_property_datatypes) {
			if (search_property_datatypes != null && !search_property_datatypes.isEmpty()) {
				search_property_datatypes.forEach(
						(datatype) -> {
							this.search_property_types.add(PathKeyType.valueOfLabel(datatype));
						});
				this.search_property_datatypes = search_property_datatypes;
			}
		}
	}
}
