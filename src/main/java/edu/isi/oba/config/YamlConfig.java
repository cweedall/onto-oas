package edu.isi.oba.config;

import static edu.isi.oba.Oba.logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.config.ontology.annotations.AnnotationConfig;
import edu.isi.oba.config.paths.PathConfig;
import edu.isi.oba.utils.ObaUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

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

	@JsonSetter(ConfigPropertyNames.ONTOLOGIES)
	public Set<String> ontologies = new HashSet<>();

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

	// ---- Computed properties:
	private final Set<OWLOntology> owlOntologies = new HashSet<>();
	private final Set<OWLClass> allowedClasses = new HashSet<>();
	private final Set<OWLClass> allReferencedClasses = new HashSet<>();

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

	public void processConfig() throws OWLOntologyCreationException, IOException {
		// Create path for output
		this.createOutputDir();

		this.setOwlOntologies();

		// Set the allowed classes for the OpenAPI based on configuration file.  If no restrictions set,
		// all classes are added from each ontology.
		this.setClassesAllowedByYamlConfig();
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

	// --------------------

	public void createOutputDir() {
		String destinationDir =
				this.outputDir
						+ File.separator
						+ this.name.replaceAll("[.\\+\\*\\?\\^\\$\\(\\)\\[\\]\\{\\}\\|\\\\\\s]", "_");
		this.outputFilePath = new File(destinationDir);
		if (!outputFilePath.exists()) {
			outputFilePath.mkdirs();
		}
	}

	/**
	 * Convenience method for unit testing.
	 *
	 * @return a {@link Set} of {@link OWLOntology}
	 */
	private final void setOwlOntologies() throws OWLOntologyCreationException, IOException {
		// Load the ontology into the manager
		this.downloadOntologies();

		// set ontology paths in YAML to the ones we have downloaded
		this.owlOntologies.addAll(this.manager.ontologies().collect(Collectors.toSet()));

		final var reasonerFactory = new StructuralReasonerFactory();

		// Before any data processing, loop through all the ontologies and fail if any are inconsistent.
		this.owlOntologies.stream()
				.forEach(
						(ontology) -> {
							final var reasoner = reasonerFactory.createReasoner(ontology);
							if (!reasoner.isConsistent()) {
								logger.severe(
										"Please fix errors with inconsistent ontology.  IRI:  "
												+ ontology.getOntologyID());
								System.exit(1);
							}

							final var format = ontology.getFormat();
							if (format == null) {
								logger.severe("No ontology format found.  Unable to proceed.");
								System.exit(1);
							}
						});
	}

	private void downloadOntologies() throws OWLOntologyCreationException, IOException {
		Set<String> ontologyPaths = new HashSet<>();
		int i = 0;
		for (final var ontologyPath : this.ontologies) {
			// copy the ontologies used in the destination folder
			final var ontologyDestinationPath =
					this.outputFilePath.getAbsolutePath() + File.separator + "ontology" + i + ".owl";
			final var ontologyFile = new File(ontologyDestinationPath);

			// content negotiation + download in case a URI is added
			if (ontologyPath.startsWith("http://") || ontologyPath.startsWith("https://")) {
				// download ontology to local path
				ObaUtils.downloadOntology(ontologyPath, ontologyDestinationPath);
			} else {
				try {
					// copy to the right folder
					Files.copy(
							new File(ontologyPath).toPath(),
							ontologyFile.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ex) {
					Logger.getLogger(this.getClass().getSimpleName())
							.log(Level.SEVERE, "ERROR while loading file: " + ontologyPath, ex);
					throw ex;
				}
			}

			logger.info("Loaded working ontology file:  " + ontologyDestinationPath.replace("\\", "/"));
			ontologyPaths.add(ontologyDestinationPath);

			// Set to silent so missing imports don't make the program fail.
			OWLOntologyLoaderConfiguration loadingConfig = new OWLOntologyLoaderConfiguration();
			loadingConfig =
					loadingConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

			try {
				this.manager.loadOntologyFromOntologyDocument(
						new FileDocumentSource(new File(ontologyDestinationPath)), loadingConfig);
			} catch (OWLOntologyCreationException ex) {
				Logger.getLogger(this.getClass().getSimpleName())
						.log(Level.SEVERE, "ERROR while creating ontology file: " + ontologyFile, ex);
				throw ex;
			}

			++i;
		}
	}

	/**
	 * Get set of allowed classes. Returns all classes, if no restrictions in configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed by the config file.
	 */
	private final void setClassesAllowedByYamlConfig() {
		if (this.owlOntologies == null || this.owlOntologies.isEmpty()) {
			logger.severe(
					"No ontologies specified in the YAML configuration file.  Nothing can be processed.");
			System.exit(1);
		} else {
			final var allowedPathClassesByIRI = this.pathConfig.getPathClasses();

			this.owlOntologies.forEach(
					(ontology) -> {
						// If the configuration contains no allowed classes, then add all classes from the
						// ontology.
						if (allowedPathClassesByIRI == null || allowedPathClassesByIRI.isEmpty()) {
							this.allowedClasses.addAll(ontology.getClassesInSignature());
						} else {
							ontology
									.classesInSignature()
									.filter(
											owlClass ->
													allowedPathClassesByIRI.contains(owlClass.getIRI())
															|| this.classes.contains(owlClass.getIRI().toString()))
									.forEach(
											(allowedClass) -> {
												this.allowedClasses.add(allowedClass);
											});
						}
					});

			this.allReferencedClasses.addAll(this.allowedClasses);
		}
	}

	/**
	 * Get set of allowed classes. Returns all classes, if no restrictions in configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed by the config file.
	 */
	public final Set<OWLClass> getAllowedOwlClasses() {
		return new HashSet<>(this.allowedClasses);
	}

	/**
	 * Get set of all classes referenced (allowed classes and any referenced classes). Returns all
	 * classes, if no restrictions in configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed/referenced by the config file.
	 */
	public final Set<OWLClass> getAllReferencedOwlClasses() {
		return new HashSet<>(this.allReferencedClasses);
	}

	public void addAllReferencedOwlClasses(Set<OWLClass> owlClasses) {
		this.allReferencedClasses.addAll(owlClasses);
	}

	public final Set<OWLOntology> getOwlOntologies() {
		return new HashSet<>(this.owlOntologies);
	}

	public final Set<String> getAllowedClasses() {
		return new HashSet<>(
				this.allowedClasses.stream()
						.map(k -> k.getIRI().getShortForm())
						.collect(Collectors.toSet()));
	}

	public final Set<String> getAllReferencedClasses() {
		return new HashSet<>(
				this.allReferencedClasses.stream()
						.map(k -> k.getIRI().getShortForm())
						.collect(Collectors.toSet()));
	}

	/**
	 * Convenience method for unit testing.
	 *
	 * @return an {@link OWLOntologyManager}
	 */
	public OWLOntologyManager getManager() {
		return this.manager;
	}
}
