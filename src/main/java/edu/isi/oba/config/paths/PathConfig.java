package edu.isi.oba.config.paths;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.IRI;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonRootName(ConfigPropertyNames.PATH_CONFIG)
public class PathConfig {
	@JsonProperty(ConfigPropertyNames.PATHS_FOR_CLASSES)
	private Set<PathsForClassConfig> pathsForClasses = new HashSet<>();

	@JsonProperty(ConfigPropertyNames.DELETE_PATHS)
	private DeletePathsConfig deletePaths;

	@JsonProperty(ConfigPropertyNames.GET_PATHS)
	private GetPathsConfig getPaths;

	@JsonProperty(ConfigPropertyNames.POST_PATHS)
	private PostPathsConfig postPaths;

	@JsonProperty(ConfigPropertyNames.PUT_PATHS)
	private PutPathsConfig putPaths;

	@JsonProperty(ConfigPropertyNames.SEARCH_PATHS)
	private SearchPathsConfig searchPaths;

	public PathConfig() {
		GlobalFlags.setFlag(ConfigPropertyNames.DISABLE_ALL_PATHS, false);
		GlobalFlags.setFlag(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES, true);
		GlobalFlags.setFlag(ConfigPropertyNames.USE_KEBAB_CASE_PATHS, false);
	}

	@JsonSetter(ConfigPropertyNames.DISABLE_ALL_PATHS)
	private void setDisableAllPaths(Boolean disableAllPaths) {
		GlobalFlags.setFlag(ConfigPropertyNames.DISABLE_ALL_PATHS, disableAllPaths);
	}

	@JsonSetter(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES)
	private void setUseCommonDefaultPathResponses(Boolean useCommonDefaultPathResponses) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES, useCommonDefaultPathResponses);
	}

	@JsonSetter(ConfigPropertyNames.USE_KEBAB_CASE_PATHS)
	private void setUseKebabCasePaths(Boolean useKebabCasePaths) {
		GlobalFlags.setFlag(ConfigPropertyNames.USE_KEBAB_CASE_PATHS, useKebabCasePaths);
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
	 * Get the {@link DeletePathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link DeletePathsConfig}
	 */
	public DeletePathsConfig getDeletePaths() {
		return this.deletePaths;
	}

	/**
	 * Get the {@link GetPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link GetPathsConfig}
	 */
	public GetPathsConfig getGetPaths() {
		return this.getPaths;
	}

	/**
	 * Get the {@link PostPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link PostPathsConfig}
	 */
	public PostPathsConfig getPostPaths() {
		return this.postPaths;
	}

	/**
	 * Get the {@link PutPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link PutPathsConfig}
	 */
	public PutPathsConfig getPutPaths() {
		return this.putPaths;
	}

	/**
	 * Get the {@link SearchPathsConfig} may be null (because it doesn't exist in the config file).
	 *
	 * @return a {@link SearchPathsConfig}
	 */
	public SearchPathsConfig getSearchPaths() {
		return this.searchPaths;
	}
}
