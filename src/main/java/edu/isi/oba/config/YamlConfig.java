package edu.isi.oba.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.config.ontology.annotations.AnnotationConfig;
import edu.isi.oba.config.paths.PathConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlConfig {
	String DEFAULT_OUTPUT_DIRECTORY = "outputs";
	String DEFAULT_PROJECT_NAME = "default_project";
	public OpenAPI openapi;

	@JsonProperty(ConfigPropertyNames.OUTPUT_DIR)
	public String outputDir = DEFAULT_OUTPUT_DIRECTORY;

	@JsonProperty(ConfigPropertyNames.NAME)
	public String name = DEFAULT_PROJECT_NAME;

	public Set<String> paths;
	public Set<String> ontologies;
	private EndpointConfig endpoint;
	private AuthConfig auth;
	public FirebaseConfig firebase;
	public Map<String, List<RelationConfig>> relations;
	private LinkedHashMap<String, PathItem> customPaths = null;
	public Set<String> classes;

	@JsonProperty(ConfigPropertyNames.ANNOTATION_CONFIG)
	private final AnnotationConfig annotationConfig = new AnnotationConfig();

	@JsonProperty(ConfigPropertyNames.PATH_CONFIG)
	private final PathConfig pathConfig = new PathConfig();

	public YamlConfig() {
		GlobalFlags.setFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS, true);
		GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, true);
		GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_PROPERTIES, true);
		GlobalFlags.setFlag(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES, false);
		GlobalFlags.setFlag(ConfigPropertyNames.FOLLOW_REFERENCES, true);
		GlobalFlags.setFlag(ConfigPropertyNames.GENERATE_JSON_FILE, false);
		GlobalFlags.setFlag(ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY, false);
		GlobalFlags.setFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES, false);
		GlobalFlags.setFlag(ConfigPropertyNames.VALIDATE_GENERATED_OPENAPI_FILE, true);
	}

	public String getOutputDir() {
		return this.outputDir;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Get the {@link PathConfig}.
	 *
	 * @return a {@link PathConfig}
	 */
	public PathConfig getPathConfig() {
		return this.pathConfig;
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

	public LinkedHashMap<String, PathItem> getCustomPaths() {
		return this.customPaths;
	}

	public void setCustomPaths(LinkedHashMap<String, PathItem> customPaths) {
		this.customPaths = customPaths;
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

	@JsonSetter(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS)
	public void setAlwaysGenerateArrays(Boolean alwaysGenerateArrays) {
		GlobalFlags.setFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS, alwaysGenerateArrays);
	}

	@JsonSetter(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES)
	public void setFixSingularPluralPropertyNames(Boolean fixSingularPluralPropertyNames) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES, fixSingularPluralPropertyNames);
	}

	@JsonSetter(ConfigPropertyNames.FOLLOW_REFERENCES)
	public void setFollowReferences(Boolean followReferences) {
		GlobalFlags.setFlag(ConfigPropertyNames.FOLLOW_REFERENCES, followReferences);
	}

	@JsonSetter(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)
	public void setUseInheritanceReferences(Boolean useInheritanceReferences) {
		GlobalFlags.setFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES, useInheritanceReferences);
	}

	@JsonSetter(ConfigPropertyNames.DEFAULT_DESCRIPTIONS)
	public void setDefaultDescriptions(Boolean defaultDescriptions) {
		GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, defaultDescriptions);
	}

	@JsonSetter(ConfigPropertyNames.DEFAULT_PROPERTIES)
	public void setDefaultProperties(Boolean defaultProperties) {
		GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_PROPERTIES, defaultProperties);
	}

	@JsonSetter(ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY)
	public void setRequiredPropertiesFromCardinality(Boolean requiredPropertiesFromCardinality) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY,
				requiredPropertiesFromCardinality);
	}

	@JsonSetter(ConfigPropertyNames.GENERATE_JSON_FILE)
	public void setGenerateJsonFile(Boolean generateJsonFile) {
		GlobalFlags.setFlag(ConfigPropertyNames.GENERATE_JSON_FILE, generateJsonFile);
	}

	@JsonSetter(ConfigPropertyNames.VALIDATE_GENERATED_OPENAPI_FILE)
	public void setValidateGeneratedOpenapiFile(Boolean validateGeneratedOpenapiFile) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.VALIDATE_GENERATED_OPENAPI_FILE, validateGeneratedOpenapiFile);
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
	public AnnotationConfig getAnnotationConfig() {
		return this.annotationConfig;
	}
}
