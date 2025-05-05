package edu.isi.oba.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.flags.ConfigFlags;
import edu.isi.oba.config.ontology.annotations.AnnotationConfig;
import edu.isi.oba.config.paths.PathConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlConfig extends ConfigFlags {
	String DEFAULT_OUTPUT_DIRECTORY = "outputs";
	String DEFAULT_PROJECT_NAME = "default_project";
	public OpenAPI openapi;
	public String output_dir = DEFAULT_OUTPUT_DIRECTORY;
	public String name = DEFAULT_PROJECT_NAME;
	public Set<String> paths;
	public Set<String> ontologies;
	private EndpointConfig endpoint;
	private AuthConfig auth;
	public FirebaseConfig firebase;
	public Map<String, List<RelationConfig>> relations;
	private LinkedHashMap<String, PathItem> custom_paths = null;
	public Set<String> classes;
	public AnnotationConfig annotation_config;
	public PathConfig path_config;

	public YamlConfig() {
		this.configFlags.putAll(
				Map.ofEntries(
						Map.entry(ConfigFlagType.ALWAYS_GENERATE_ARRAYS, true),
						Map.entry(ConfigFlagType.DEFAULT_DESCRIPTIONS, true),
						Map.entry(ConfigFlagType.DEFAULT_PROPERTIES, true),
						Map.entry(ConfigFlagType.FIX_SINGULAR_PLURAL_PROPERTY_NAMES, false),
						Map.entry(ConfigFlagType.FOLLOW_REFERENCES, true),
						Map.entry(ConfigFlagType.GENERATE_JSON_FILE, false),
						Map.entry(ConfigFlagType.REQUIRED_PROPERTIES_FROM_CARDINALITY, false),
						Map.entry(ConfigFlagType.USE_INHERITANCE_REFERENCES, false),
						Map.entry(ConfigFlagType.VALIDATE_GENERATED_OPENAPI_FILE, true)));
	}

	/**
	 * Get the {@link PathConfig}.
	 *
	 * @return a {@link PathConfig}
	 */
	public PathConfig getPath_config() {
		return this.path_config;
	}

	/**
	 * Set the PathConfig, if it exists in the config file. This is the configuration for all
	 * paths/operations.
	 *
	 * @param {path_config} a {@link PathConfig}
	 */
	public void setPath_config(PathConfig path_config) {
		if (path_config == null) {
			this.path_config = new PathConfig();
		} else {
			this.path_config = path_config;
		}

		this.configFlags.putAll(this.path_config.getConfigFlags());
	}

	public String getOutput_dir() {
		return this.output_dir;
	}

	public void setOutput_dir(String output_dir) {
		this.output_dir = output_dir;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getPaths() {
		return this.paths;
	}

	public void setPaths(Set<String> paths) {
		this.paths = paths;
	}

	public Set<String> getOntologies() {
		return this.ontologies;
	}

	public void setOntologies(Set<String> ontologies) {
		this.ontologies = ontologies;
	}

	public EndpointConfig getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(EndpointConfig endpoint) {
		this.endpoint = endpoint;
	}

	public FirebaseConfig getFirebase() {
		return this.firebase;
	}

	public void setFirebase(FirebaseConfig firebase) {
		this.firebase = firebase;
	}

	public Map<String, List<RelationConfig>> getRelations() {
		return this.relations;
	}

	public void setRelations(Map<String, List<RelationConfig>> relations) {
		this.relations = relations;
	}

	public LinkedHashMap<String, PathItem> getCustom_paths() {
		return this.custom_paths;
	}

	public void setCustom_paths(LinkedHashMap<String, PathItem> custom_paths) {
		this.custom_paths = custom_paths;
	}

	public OpenAPI getOpenapi() {
		return this.openapi;
	}

	public void setOpenapi(OpenAPI openapi) {
		this.openapi = openapi;
	}

	public Set<String> getClasses() {
		return this.classes;
	}

	public void setClasses(Set<String> classes) {
		this.classes = classes;
	}

	public Boolean getAlways_generate_arrays() {
		return this.configFlags.get(ConfigFlagType.ALWAYS_GENERATE_ARRAYS);
	}

	public void setAlways_generate_arrays(Boolean always_generate_arrays) {
		this.configFlags.put(ConfigFlagType.ALWAYS_GENERATE_ARRAYS, always_generate_arrays);
	}

	public Boolean getFix_singular_plural_property_names() {
		return this.configFlags.get(ConfigFlagType.FIX_SINGULAR_PLURAL_PROPERTY_NAMES);
	}

	public void setFix_singular_plural_property_names(Boolean fix_singular_plural_property_names) {
		this.configFlags.put(
				ConfigFlagType.FIX_SINGULAR_PLURAL_PROPERTY_NAMES, fix_singular_plural_property_names);
	}

	public Boolean getFollow_references() {
		return this.configFlags.get(ConfigFlagType.FOLLOW_REFERENCES);
	}

	public void setFollow_references(Boolean follow_references) {
		this.configFlags.put(ConfigFlagType.FOLLOW_REFERENCES, follow_references);
	}

	public Boolean getUse_inheritance_references() {
		return this.configFlags.get(ConfigFlagType.USE_INHERITANCE_REFERENCES);
	}

	public void setUse_inheritance_references(Boolean use_inheritance_references) {
		this.configFlags.put(ConfigFlagType.USE_INHERITANCE_REFERENCES, use_inheritance_references);
	}

	public Boolean getDefault_descriptions() {
		return this.configFlags.get(ConfigFlagType.DEFAULT_DESCRIPTIONS);
	}

	public void setDefault_descriptions(Boolean default_descriptions) {
		this.configFlags.put(ConfigFlagType.DEFAULT_DESCRIPTIONS, default_descriptions);
	}

	public Boolean getDefault_properties() {
		return this.configFlags.get(ConfigFlagType.DEFAULT_PROPERTIES);
	}

	public void setDefault_properties(Boolean default_properties) {
		this.configFlags.put(ConfigFlagType.DEFAULT_PROPERTIES, default_properties);
	}

	public Boolean getRequired_properties_from_cardinality() {
		return this.configFlags.get(ConfigFlagType.REQUIRED_PROPERTIES_FROM_CARDINALITY);
	}

	public void setRequired_properties_from_cardinality(
			Boolean required_properties_from_cardinality) {
		this.configFlags.put(
				ConfigFlagType.REQUIRED_PROPERTIES_FROM_CARDINALITY, required_properties_from_cardinality);
	}

	public Boolean getGenerate_json_file() {
		return this.configFlags.get(ConfigFlagType.GENERATE_JSON_FILE);
	}

	public void setGenerate_json_file(Boolean generate_json_file) {
		this.configFlags.put(ConfigFlagType.GENERATE_JSON_FILE, generate_json_file);
	}

	public Boolean getValidate_generated_openapi_file() {
		return this.configFlags.get(ConfigFlagType.VALIDATE_GENERATED_OPENAPI_FILE);
	}

	public void setValidate_generated_openapi_file(Boolean validate_generated_openapi_file) {
		this.configFlags.put(
				ConfigFlagType.VALIDATE_GENERATED_OPENAPI_FILE, validate_generated_openapi_file);
	}

	public AuthConfig getAuth() {
		return this.auth;
	}

	public void setAuth(AuthConfig auth) {
		this.auth = auth;
	}

	/**
	 * Get the {@link AnnotationConfig}
	 *
	 * @return a {@link AnnotationConfig}
	 */
	public AnnotationConfig getAnnotation_config() {
		return this.annotation_config;
	}

	public void setAnnotation_config(AnnotationConfig annotation_config) {
		this.annotation_config = annotation_config;
	}
}
