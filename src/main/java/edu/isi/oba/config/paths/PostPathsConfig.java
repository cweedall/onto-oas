package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class PostPathsConfig extends ConfigFlags {
	private Post_Bulk post_bulk;
	private Post_Single post_single;

	/**
	 * Get the {@link Post_Bulk} config (a sub-property within {@link PostPathsConfig}).
	 *
	 * @return a {@link Post_Bulk}
	 */
	public Post_Bulk getPost_bulk() {
		return this.post_bulk;
	}

	/**
	 * Set the {@link Post_Bulk}, if it exists in the config file. This is the configuration for POST
	 * paths.
	 *
	 * @param {post_bulk} A {@link Post_Bulk}
	 */
	public void setPost_bulk(Post_Bulk post_bulk) {
		if (post_bulk == null) {
			this.post_bulk = new Post_Bulk();
		} else {
			this.post_bulk = post_bulk;
		}

		this.configFlags.putAll(this.post_bulk.getConfigFlags());
	}

	/**
	 * Get the {@link Post_Single} config (a sub-property within {@link PostPathsConfig}).
	 *
	 * @return a {@link PostPathsConfig}
	 */
	public Post_Single getPost_single() {
		return post_single;
	}

	/**
	 * Set the {@link Post_Single}, if it exists in the config file. This is the configuration for
	 * POST paths.
	 *
	 * @param {post_single} A {@link Post_Single}
	 */
	public void setPost_single(Post_Single post_single) {
		if (post_single == null) {
			this.post_single = new Post_Single();
		} else {
			this.post_single = post_single;
		}

		this.configFlags.putAll(this.post_single.getConfigFlags());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Post_Bulk extends ConfigFlags {
		// Default path suffix to be "_bulk";
		public String path_suffix = "_bulk";

		Post_Bulk() {
			configFlags.putAll(Map.ofEntries(Map.entry(ConfigFlagType.PATH_POST_BULK, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(ConfigFlagType.PATH_POST_BULK);
		}

		public void setEnable(Boolean enable) {
			if (enable != null) {
				this.configFlags.put(ConfigFlagType.PATH_POST_BULK, enable);
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
	public static class Post_Single extends ConfigFlags {
		Post_Single() {
			this.configFlags.putAll(Map.ofEntries(Map.entry(ConfigFlagType.PATH_POST_SINGLE, false)));
		}

		public Boolean getEnable() {
			return this.configFlags.get(ConfigFlagType.PATH_POST_SINGLE);
		}

		public void setEnable(Boolean enable) {
			if (enable != null) {
				this.configFlags.put(ConfigFlagType.PATH_POST_SINGLE, enable);
			}
		}
	}
}
