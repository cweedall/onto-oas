package edu.isi.oba.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.config.ontology.annotations.AnnotationConfig;
import edu.isi.oba.config.paths.PathConfig;
import edu.isi.oba.exceptions.ConfigValidationException;
import edu.isi.oba.exceptions.OntologyLoadingException;
import edu.isi.oba.ontology.reasoner.ReasonerUtil;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import edu.isi.oba.utils.ontology.OntologyDownloader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Represents the configuration loaded from a YAML file for generating OpenAPI schemas based on OWL
 * ontologies. Includes settings for paths, annotations, authentication, Firebase integration, and
 * global flags.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlConfig {
	private static final Logger logger = Logger.getLogger(YamlConfig.class.getName());

	private static final String ONTOLOGY_FILE_PREFIX = "ontology";
	private static final String ONTOLOGY_FILE_EXTENSION = ".owl";
	private static final String LOG_FILE_LOADED = "Loaded working ontology file: ";

	private static final String DEFAULT_OUTPUT_DIRECTORY = "outputs";
	private static final String DEFAULT_PROJECT_NAME = "default_project";

	@JsonProperty(ConfigPropertyNames.OPENAPI_OVERVIEW)
	private OpenAPI openapi;

	@JsonProperty(ConfigPropertyNames.OUTPUT_DIR)
	private String outputDir = DEFAULT_OUTPUT_DIRECTORY;

	@JsonProperty(ConfigPropertyNames.NAME)
	private String name = DEFAULT_PROJECT_NAME;

	@JsonSetter(ConfigPropertyNames.ONTOLOGIES)
	private final Set<String> ontologies = new HashSet<>();

	@JsonProperty(ConfigPropertyNames.ENDPOINT_CONFIG)
	private EndpointConfig endpoint;

	@JsonProperty(ConfigPropertyNames.AUTH_CONFIG)
	private AuthConfig auth;

	@JsonProperty(ConfigPropertyNames.FIREBASE_CONFIG)
	private FirebaseConfig firebase;

	@JsonProperty(ConfigPropertyNames.RELATIONS_CONFIG)
	private final Map<String, List<RelationConfig>> relations = new HashMap<>();

	@JsonProperty(ConfigPropertyNames.CUSTOM_PATHS_CONFIG)
	private final Map<String, PathItem> customPaths = new LinkedHashMap<>();

	@JsonProperty(ConfigPropertyNames.EXTRA_CLASS_SCHEMAS)
	private final Set<String> extraClassSchemas = new HashSet<>();

	@JsonProperty(ConfigPropertyNames.ANNOTATION_CONFIG)
	private final AnnotationConfig annotationConfig = new AnnotationConfig();

	@JsonProperty(ConfigPropertyNames.PATH_CONFIG)
	private final PathConfig pathConfig = new PathConfig();

	// ---- Computed properties:
	private final Set<OWLOntology> owlOntologies = Collections.synchronizedSet(new HashSet<>());
	private final Set<OWLClass> allowedClasses = Collections.synchronizedSet(new HashSet<>());
	private final Set<OWLClass> allReferencedClasses = Collections.synchronizedSet(new HashSet<>());

	private File outputFilePath;

	private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

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

	/**
	 * Validates the configuration before processing.
	 *
	 * @throws ConfigValidationException if required fields are missing or malformed.
	 */
	void validate() throws ConfigValidationException {
		if (outputDir == null || outputDir.isBlank()) {
			logger.warning("Validation failed: Output directory is missing.");
			throw new ConfigValidationException("Output directory must be specified.");
		}

		if (name == null || name.isBlank()) {
			logger.warning("Validation failed: Project name is missing.");
			throw new ConfigValidationException("Project name must be specified.");
		}

		if (ontologies == null || ontologies.isEmpty()) {
			logger.warning("Validation failed: Ontology/ies missing.");
			throw new ConfigValidationException("At least one ontology must be specified.");
		}

		pathConfig.validate();

		annotationConfig.validate();
	}

	/**
	 * Performs post-processing after deserialization.
	 *
	 * <p>This method is package-private to restrict usage to internal components like {@link
	 * YamlConfigDeserializer}.
	 *
	 * @throws ConfigValidationException
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 */
	void processConfig() throws ConfigValidationException, OWLOntologyCreationException, IOException {
		// Ensure config is structurally sound before proceeding
		this.validate();

		// Create path for output
		this.createOutputDir();

		this.setOwlOntologies();

		// Set the allowed classes for the OpenAPI based on configuration file.  If no restrictions set,
		// all classes are added from each ontology.
		this.setClassesAllowedByYamlConfig();

		// If auth config is present, verify a Firebase key also.  Default authorization config, if
		// null.
		this.handleAuth();
	}

	private void handleAuth() {
		if (this.auth != null) {
			Provider provider = this.auth.getProvider_obj();
			if (provider.equals(Provider.FIREBASE) && this.firebase.getKey() == null) {
				FatalErrorHandler.fatal("You must set up the firebase key");
			}
		} /* else {
				this.auth = new AuthConfig();
			}*/
	}

	/**
	 * Get the output directory for the project.
	 *
	 * @return a {@link String} of the output directory path
	 */
	public String getOutputDir() {
		return this.outputDir;
	}

	/**
	 * Get the name of the project.
	 *
	 * @return a {@link String} of the project name
	 */
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

	/**
	 * Get the {@link Set} of path strings to ontologies in the config.
	 *
	 * @return {@link Set} of {@link String} specifying paths to the ontologies
	 */
	public Set<String> getOntologies() {
		return Collections.unmodifiableSet(this.ontologies);
	}

	/**
	 * Get the SPARQL {@link EndpointConfig}
	 *
	 * @return a SPARQL {@link EndpointConfig}
	 */
	public EndpointConfig getEndpoint() {
		return this.endpoint;
	}

	/**
	 * Get the {@link FirebaseConfig}
	 *
	 * @return a {@link FirebaseConfig}
	 */
	public FirebaseConfig getFirebase() {
		return this.firebase;
	}

	/**
	 * Get all the {@link RelationConfig}s.
	 *
	 * @return a {@link Map} of {@link String} and {@link List}s (of {@link RelationConfig}s)
	 */
	public Map<String, List<RelationConfig>> getRelations() {
		return Collections.unmodifiableMap(this.relations);
	}

	/**
	 * Get all the custom paths.
	 *
	 * @return a {@link Map} of path names and {@link PathItems}
	 */
	public Map<String, PathItem> getCustomPaths() {
		return Collections.unmodifiableMap(this.customPaths);
	}

	/**
	 * Get the OpenAPI object initialized by the config file.
	 *
	 * @return an {@link OpenAPI} object
	 */
	public OpenAPI getOpenapi() {
		return this.openapi;
	}

	/**
	 * Set the {@link OpenAPI} object.
	 *
	 * @param openapi a {@link OpenAPI} object
	 */
	public void setOpenapi(OpenAPI openapi) {
		this.openapi = openapi;
	}

	/**
	 * Get the {@link Set} of {@link String}s indicating classes to include schemas for (but not have
	 * an path/endpoint created).
	 *
	 * @return a {@link Set} of {@link String}s
	 */
	public Set<String> getExtraClassSchemas() {
		return Collections.unmodifiableSet(this.extraClassSchemas);
	}

	/**
	 * Set the configuration flag to enable or disable always using arrays in generated schemas.
	 *
	 * @param alwaysGenerateArrays a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS)
	public void setAlwaysGenerateArrays(Boolean alwaysGenerateArrays) {
		GlobalFlags.setFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS, alwaysGenerateArrays);
	}

	/**
	 * Set the configuration flag to enable or disable automatic singular/plural naming of generated
	 * schema names.
	 *
	 * @param fixSingularPluralPropertyNames a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES)
	public void setFixSingularPluralPropertyNames(Boolean fixSingularPluralPropertyNames) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES, fixSingularPluralPropertyNames);
	}

	/**
	 * Set the configuration flag to enable or disable follow references to other schemas in generated
	 * schemas.
	 *
	 * @param followReferences a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.FOLLOW_REFERENCES)
	public void setFollowReferences(Boolean followReferences) {
		GlobalFlags.setFlag(ConfigPropertyNames.FOLLOW_REFERENCES, followReferences);
	}

	/**
	 * Set the configuration flag to enable or disable using inheritance (via references to super
	 * class/schema) in generated schemas.
	 *
	 * @param useInheritanceReferences a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)
	public void setUseInheritanceReferences(Boolean useInheritanceReferences) {
		GlobalFlags.setFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES, useInheritanceReferences);
	}

	/**
	 * Set the configuration flag to enable or disable default descriptions in generated schemas.
	 *
	 * @param defaultDescriptions a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.DEFAULT_DESCRIPTIONS)
	public void setDefaultDescriptions(Boolean defaultDescriptions) {
		GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, defaultDescriptions);
	}

	/**
	 * Set the configuration flag to enable or disable default properties in generated schemas.
	 *
	 * @param defaultProperties a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.DEFAULT_PROPERTIES)
	public void setDefaultProperties(Boolean defaultProperties) {
		GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_PROPERTIES, defaultProperties);
	}

	/**
	 * Set the configuration flag to enable or disable generate required properties based on
	 * cardinality in generated schemas.
	 *
	 * @param requiredPropertiesFromCardinality a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY)
	public void setRequiredPropertiesFromCardinality(Boolean requiredPropertiesFromCardinality) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY,
				requiredPropertiesFromCardinality);
	}

	/**
	 * Set the configuration flag to enable or disable JSON file output (default is YAML).
	 *
	 * @param generateJsonFile a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.GENERATE_JSON_FILE)
	public void setGenerateJsonFile(Boolean generateJsonFile) {
		GlobalFlags.setFlag(ConfigPropertyNames.GENERATE_JSON_FILE, generateJsonFile);
	}

	/**
	 * Set the configuration flag to enable or disable validation of the outputted OpenAPI file.
	 *
	 * @param validateGeneratedOpenapiFile a {@link Boolean} indicator
	 */
	@JsonSetter(ConfigPropertyNames.VALIDATE_GENERATED_OPENAPI_FILE)
	public void setValidateGeneratedOpenapiFile(Boolean validateGeneratedOpenapiFile) {
		GlobalFlags.setFlag(
				ConfigPropertyNames.VALIDATE_GENERATED_OPENAPI_FILE, validateGeneratedOpenapiFile);
	}

	/**
	 * Get the {@link AuthConfig}
	 *
	 * @return an {@link AuthConfig}
	 */
	public AuthConfig getAuth() {
		return this.auth;
	}

	/**
	 * Get the {@link AnnotationConfig}
	 *
	 * @return an {@link AnnotationConfig}
	 */
	public AnnotationConfig getAnnotationConfig() {
		return this.annotationConfig;
	}

	// --------------------

	private String sanitizeProjectName(String name) {
		return name.replaceAll("[.\\+\\*\\?\\^\\$\\(\\)\\[\\]\\{\\}\\|\\\\\\s]", "_");
	}

	/**
	 * createOutputDir method.
	 *
	 * @return void result of createOutputDir
	 * @throws IOException
	 */
	public void createOutputDir() throws IOException {
		final var destinationDir = this.outputDir + File.separator + sanitizeProjectName(this.name);
		this.outputFilePath = new File(destinationDir);
		if (!outputFilePath.exists()) {
			Files.createDirectories(outputFilePath.toPath());
		}
	}

	/**
	 * Downloads and stores all OWL ontologies in the config. Validates that each ontology is
	 * consistent.
	 *
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 */
	private final void setOwlOntologies() throws OWLOntologyCreationException, IOException {
		// Load the ontology into the manager
		this.downloadOntologies();

		// Set ontology paths in YAML to the ones we have downloaded
		this.owlOntologies.addAll(this.manager.ontologies().collect(Collectors.toSet()));

		// Before any data processing, loop through all the ontologies and fail if any are inconsistent.
		this.owlOntologies.stream()
				.forEach(
						(ontology) -> {
							final var reasoner = ReasonerUtil.createReasoner(ontology);

							if (!reasoner.isConsistent()) {
								FatalErrorHandler.fatal(
										"Please fix errors with inconsistent ontology.  IRI:  "
												+ ontology.getOntologyID());
							}

							final var format = ontology.getFormat();
							if (format == null) {
								FatalErrorHandler.fatal("No ontology format found.  Unable to proceed.");
							}
						});
	}

	/**
	 * Download ontologies based on the path specified in the config file.
	 *
	 * @throws IOException
	 */
	private void downloadOntologies() throws IOException {
		int index = 0;
		for (final var ontologyPath : this.ontologies) {
			final var destinationPath = getOntologyDestinationPath(index);
			try {
				if (ontologyPath.startsWith("http://") || ontologyPath.startsWith("https://")) {
					downloadOntologyFromUri(ontologyPath, destinationPath);
				} else {
					copyOntologyToDestination(ontologyPath, destinationPath);
				}

				logger.log(Level.INFO, LOG_FILE_LOADED + destinationPath.replace("\\", "/"));

				loadOntologyIntoManager(destinationPath);

			} catch (IOException | OntologyLoadingException ex) {
				logger.log(Level.SEVERE, "Error processing ontology: " + ontologyPath, ex);
				throw ex;
			}
			index++;
		}
	}

	/**
	 * Get an ontology's project destination path, based on an index number.
	 *
	 * @param index an integer specifying the number of the ontology file.
	 * @return a {@link String} of the ontology destination path
	 */
	private String getOntologyDestinationPath(int index) {
		return this.outputFilePath.getAbsolutePath()
				+ File.separator
				+ ONTOLOGY_FILE_PREFIX
				+ index
				+ ONTOLOGY_FILE_EXTENSION;
	}

	/**
	 * Copy an ontology file from its original place to the project destination folder.
	 *
	 * @param sourcePath a {@link String} of the source file's path
	 * @param destinationPath a {@link String} of the destination file's path
	 * @throws IOException
	 */
	private void copyOntologyToDestination(String sourcePath, String destinationPath)
			throws IOException {
		Files.copy(
				new File(sourcePath).toPath(),
				new File(destinationPath).toPath(),
				StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Download an ontology file from a URL to the project destination path.
	 *
	 * @param uri a {@link String} of the source file's URL/URI
	 * @param destinationPath a {@link String} of the destination file's path
	 * @throws IOException
	 */
	private void downloadOntologyFromUri(String uri, String destinationPath) throws IOException {
		OntologyDownloader.downloadOntology(uri, destinationPath);
	}

	/**
	 * Load an ontology into the {@link OWLOntologyManager}.
	 *
	 * @param path a {@link String} of the (local project) ontology file's path
	 * @throws OntologyLoadingException
	 */
	private void loadOntologyIntoManager(String path) throws OntologyLoadingException {
		OWLOntologyLoaderConfiguration config =
				new OWLOntologyLoaderConfiguration()
						.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		try {
			manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(path)), config);
		} catch (OWLOntologyCreationException ex) {
			throw new OntologyLoadingException("Failed to load ontology from path: " + path, ex);
		}
	}

	/**
	 * Get set of allowed classes. Get all classes, if no restrictions in configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed by the config file.
	 */
	private final void setClassesAllowedByYamlConfig() {
		if (this.owlOntologies == null || this.owlOntologies.isEmpty()) {
			FatalErrorHandler.fatal(
					"No ontologies specified in the YAML configuration file.  Nothing can be processed.");
			return;
		} else {
			final var allowedPathClassesByIRI = this.pathConfig.getPathClasses();

			owlOntologies.forEach(
					ontology -> {
						Set<OWLClass> classesToAdd =
								getAllowedClassesFromOntology(ontology, allowedPathClassesByIRI);
						allowedClasses.addAll(classesToAdd);
					});

			this.allReferencedClasses.addAll(this.allowedClasses);
		}
	}

	/** Determines which OWL classes from the ontology are allowed based on config restrictions. */
	private Set<OWLClass> getAllowedClassesFromOntology(
			OWLOntology ontology, Set<IRI> allowedPathClassesByIRI) {
		if (allowedPathClassesByIRI == null || allowedPathClassesByIRI.isEmpty()) {
			return ontology.getClassesInSignature();
		}

		return ontology
				.classesInSignature()
				.filter(
						owlClass ->
								allowedPathClassesByIRI.contains(owlClass.getIRI())
										|| (extraClassSchemas != null
												&& extraClassSchemas.contains(owlClass.getIRI().toString())))
				.collect(Collectors.toSet());
	}

	/**
	 * Get {@link Set} of allowed {@link OWLClass}es classes. Get all classes, if no restrictions in
	 * configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed by the config file.
	 */
	public final Set<OWLClass> getAllowedOwlClasses() {
		return Collections.unmodifiableSet(this.allowedClasses);
	}

	/**
	 * Get {@link Set} of all {@link OWLClass}es referenced (allowed classes and any referenced
	 * classes). Get all classes, if no restrictions in configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed (or referenced by allowed {@link
	 *     OWLClass}es) the config file.
	 */
	public final Set<OWLClass> getAllReferencedOwlClasses() {
		return Collections.unmodifiableSet(this.allReferencedClasses);
	}

	/**
	 * Add additional {@link OWLClass}es to the the {@link Set} of referenced {@link OWLClass}es.
	 *
	 * @param owlClasses a {@link Set} of referenced {@link OWLClass}es to be added.
	 */
	public void addAllReferencedOwlClasses(Set<OWLClass> owlClasses) {
		this.allReferencedClasses.addAll(owlClasses);
	}

	/**
	 * Get all {@link OWLOntology} items defined in the config file.
	 *
	 * @return a {@link Set} of {@link OWLOntology}
	 */
	public final Set<OWLOntology> getOwlOntologies() {
		return Collections.unmodifiableSet(this.owlOntologies);
	}

	/**
	 * Get {@link Set} of allowed {@link OWLClass} names. Get all class names, if no restrictions in
	 * configuration file.
	 *
	 * @return a {@link Set} of allowed {@link OWLClass} names
	 */
	public final Set<String> getAllowedClasses() {
		return Collections.unmodifiableSet(
				this.allowedClasses.stream()
						.map(k -> k.getIRI().getShortForm())
						.collect(Collectors.toSet()));
	}

	/**
	 * Get {@link Set} of referenced AND allowed {@link OWLClass} names. Get all class names, if no
	 * restrictions in configuration file.
	 *
	 * @return a {@link Set} of referenced AND allowed {@link OWLClass} names
	 */
	public final Set<String> getAllReferencedClasses() {
		return Collections.unmodifiableSet(
				this.allReferencedClasses.stream()
						.map(k -> k.getIRI().getShortForm())
						.collect(Collectors.toSet()));
	}

	/**
	 * Convenience method for unit testing. Not intended for production use.
	 *
	 * @return an {@link OWLOntologyManager} @TestOnly
	 */
	public OWLOntologyManager getManager() {
		return this.manager;
	}
}
