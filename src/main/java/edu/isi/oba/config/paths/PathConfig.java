package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.IRI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PathConfig extends ConfigFlags {
	private Set<PathsForClassConfig> pathsForClasses;
	private DeletePathsConfig delete_paths;
	private GetPathsConfig get_paths;
	private PostPathsConfig post_paths;
	private PutPathsConfig put_paths;
	private SearchPathsConfig search_paths;

	public PathConfig() {
		this.configFlags.putAll(
				Map.ofEntries(
						Map.entry(ConfigFlagType.DISABLE_ALL_PATHS, false),
						Map.entry(ConfigFlagType.PATH_PATCH, false),
						Map.entry(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES, true),
						Map.entry(ConfigFlagType.USE_KEBAB_CASE_PATHS, false)));
	}

	public Boolean getDisable_all_paths() {
		return this.configFlags.get(ConfigFlagType.DISABLE_ALL_PATHS);
	}

	public void setDisable_all_paths(Boolean disable_all_paths) {
		this.configFlags.put(ConfigFlagType.DISABLE_ALL_PATHS, disable_all_paths);
	}

	public Boolean getEnable_patch_paths() {
		return this.configFlags.get(ConfigFlagType.PATH_PATCH);
	}

	public void setEnable_patch_paths(Boolean enable_patch_paths) {
		this.configFlags.put(ConfigFlagType.PATH_PATCH, enable_patch_paths);
	}

	public Boolean getUse_common_default_path_responses() {
		return this.configFlags.get(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES);
	}

	public void setUse_common_default_path_responses(Boolean use_common_default_path_responses) {
		this.configFlags.put(
				ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES, use_common_default_path_responses);
	}

	public Boolean getUse_kebab_case_paths() {
		return this.configFlags.get(ConfigFlagType.USE_KEBAB_CASE_PATHS);
	}

	public void setUse_kebab_case_paths(Boolean use_kebab_case_paths) {
		this.configFlags.put(ConfigFlagType.USE_KEBAB_CASE_PATHS, use_kebab_case_paths);
	}

	/**
	 * Get the {@link PathsForClassConfig} for a particular class IRI.
	 *
	 * @return a {@link PathsForClassConfig}
	 */
	public PathsForClassConfig getPathsForClasses(IRI classIRI) {
		return this.pathsForClasses.stream()
				.filter(k -> classIRI.equals(k.getClassIRI()))
				.findFirst()
				.orElse(new PathsForClassConfig());
	}

	/**
	 * Get all class {@link IRI}s from the {@link Set} of {@link PathsForClassConfig}.
	 *
	 * @return a {@link Set} of {@link IRI}s
	 */
	public Set<IRI> getPathClasses() {
		return this.pathsForClasses.stream().map(k -> k.getClassIRI()).collect(Collectors.toSet());
	}

	/**
	 * Set all {@link PathsForClassConfig}, if any exist in the config file.
	 *
	 * @param {pathsForClasses} a {@link Set} of {@link PathsForClassConfig}
	 */
	public void setPaths_for_classes(Set<PathsForClassConfig> pathsForClasses) {
		this.pathsForClasses = pathsForClasses;
	}

	/**
	 * Get the {@link DeletePathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link DeletePathsConfig}
	 */
	public DeletePathsConfig getDelete_paths() {
		return this.delete_paths;
	}

	/**
	 * Set the DeletePathsConfig, if it exists in the config file. This is the configuration for
	 * DELETE paths.
	 *
	 * @param {delete_paths} A {@link DeletePathsConfig}
	 */
	public void setDelete_paths(DeletePathsConfig delete_paths) {
		if (delete_paths == null) {
			this.delete_paths = new DeletePathsConfig();
		} else {
			this.delete_paths = delete_paths;
		}

		this.configFlags.putAll(this.delete_paths.getConfigFlags());
	}

	/**
	 * Get the {@link GetPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link GetPathsConfig}
	 */
	public GetPathsConfig getGet_paths() {
		return this.get_paths;
	}

	/**
	 * Set the GetPathsConfig, if it exists in the config file. This is the configuration for GET
	 * paths.
	 *
	 * @param {get_paths} A {@link GetPathsConfig}
	 */
	public void setGet_paths(GetPathsConfig get_paths) {
		if (get_paths == null) {
			this.get_paths = new GetPathsConfig();
		} else {
			this.get_paths = get_paths;
		}

		this.configFlags.putAll(this.get_paths.getConfigFlags());
	}

	/**
	 * Get the {@link PostPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link PostPathsConfig}
	 */
	public PostPathsConfig getPost_paths() {
		return this.post_paths;
	}

	/**
	 * Set the PostPathsConfig, if it exists in the config file. This is the configuration for POST
	 * paths.
	 *
	 * @param {post_paths} A {@link PostPathsConfig}
	 */
	public void setPost_paths(PostPathsConfig post_paths) {
		if (post_paths == null) {
			this.post_paths = new PostPathsConfig();
		} else {
			this.post_paths = post_paths;
		}

		this.configFlags.putAll(this.post_paths.getConfigFlags());
	}

	/**
	 * Get the {@link PutPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link PutPathsConfig}
	 */
	public PutPathsConfig getPut_paths() {
		return this.put_paths;
	}

	/**
	 * Set the PutPathsConfig, if it exists in the config file. This is the configuration for PUT
	 * paths.
	 *
	 * @param {put_paths} A {@link PutPathsConfig}
	 */
	public void setPut_paths(PutPathsConfig put_paths) {
		if (put_paths == null) {
			this.put_paths = new PutPathsConfig();
		} else {
			this.put_paths = put_paths;
		}

		this.configFlags.putAll(this.put_paths.getConfigFlags());
	}

	/**
	 * Get the {@link SearchPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link SearchPathsConfig}
	 */
	public SearchPathsConfig getSearch_paths() {
		return this.search_paths;
	}

	/**
	 * Set the SearchPathsConfig, if it exists in the config file. This is the configuration for PUT
	 * paths.
	 *
	 * @param {search_paths} A {@link SearchPathsConfig}
	 */
	public void setSearch_paths(SearchPathsConfig search_paths) {
		if (search_paths == null) {
			this.search_paths = new SearchPathsConfig();
		} else {
			this.search_paths = search_paths;
		}

		this.configFlags.putAll(this.search_paths.getConfigFlags());
	}
}
