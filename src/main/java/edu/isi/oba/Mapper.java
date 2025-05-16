package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.generators.PathGenerator;
import edu.isi.oba.utils.ObaUtils;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

class Mapper {
	private final Map<IRI, String> schemaNames = new HashMap<>(); // URI-names of the schemas
	private final Map<String, Schema> schemas = new HashMap<>();
	private final Paths paths = new Paths();
	private final Set<OWLOntology> ontologies;
	private final Set<OWLClass> allowedClasses = new HashSet<>();
	private final YamlConfig configData;

	private final OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

	private final Map<String, Map<String, String>> fullMarkdownGenerationMap = new TreeMap<>();

	/**
	 * Constructor
	 *
	 * @param configData the configuration data
	 * @throws OWLOntologyCreationException
	 * @throws IOException
	 */
	public Mapper(YamlConfig configData) throws OWLOntologyCreationException, IOException {
		this.configData = configData;

		Set<String> configOntologies = this.configData.getOntologies();
		String destinationDir =
				this.configData.getOutput_dir()
						+ File.separator
						+ this.configData
								.getName()
								.replaceAll("[.\\+\\*\\?\\^\\$\\(\\)\\[\\]\\{\\}\\|\\\\\\s]", "_");
		File outputDir = new File(destinationDir);
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		// Load the ontology into the manager
		int i = 0;
		Set<String> ontologyPaths = new HashSet<>();
		this.downloadOntologies(configOntologies, destinationDir, i, ontologyPaths);

		// set ontology paths in YAML to the ones we have downloaded
		this.configData.setOntologies(ontologyPaths);
		this.ontologies = this.manager.ontologies().collect(Collectors.toSet());

		final var reasonerFactory = new StructuralReasonerFactory();

		// Before any data processing, loop through all the ontologies and fail if any are inconsistent.
		this.ontologies.stream()
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

		// Set the allowed classes for the OpenAPI based on configuration file.  If no restrictions set,
		// all classes are added from each ontology.
		this.allowedClasses.addAll(this.getClassesAllowedByYamlConfig());
	}

	/**
	 * Convenience method for unit testing.
	 *
	 * @return a {@link Set} of {@link OWLOntology}
	 */
	public Set<OWLOntology> getOntologies() {
		return this.ontologies;
	}

	/**
	 * Convenience method for unit testing.
	 *
	 * @return an {@link OWLOntologyManager}
	 */
	public OWLOntologyManager getManager() {
		return this.manager;
	}

	private Schema getSchema(OWLClass cls, OWLOntology ontology) {
		logger.info("=======================================================================");
		logger.info("##############################################");
		logger.info("###  Beginning schema mapping for class:");
		logger.info("###\t" + cls);
		logger.info("##############################################");

		// Convert from OWL Class to OpenAPI Schema.
		// final var objVisitor = new ObjectVisitor(ontology, this.configData);
		// cls.accept(objVisitor);
		final var objVisitor = new ObjectVisitor(this.configData);
		objVisitor.visit(ontology, cls);

		final var mappedSchema = objVisitor.getClassSchema();

		// Each time we generate a class's schema, there may be referenced classes that need to be added
		// to the set of allowed classes.
		this.allowedClasses.addAll(objVisitor.getAllReferencedClasses());

		// Ignore schema, if null.  Otherwise, save it.
		if (mappedSchema == null) {
			logger.info("");
			logger.info("--->  IGNORING EMPTY/UNREFERENCED CLASS:  \"" + cls.getIRI() + "\"");
			logger.info("=======================================================================");
			logger.info("");
		} else {
			// Create the OpenAPI schema
			logger.info("");
			logger.info("--->  SAVING SCHEMA:  \"" + mappedSchema.getName() + "\"");
			logger.info("=======================================================================");

			final var classMarkdownMap = objVisitor.getMarkdownMappings();
			if (!classMarkdownMap.isEmpty()) {
				classMarkdownMap.forEach(
						(annotationKey, propertyAnnotationValueMap) -> {
							if (this.fullMarkdownGenerationMap.containsKey(annotationKey)) {
								this.fullMarkdownGenerationMap
										.get(annotationKey)
										.putAll(propertyAnnotationValueMap);
							} else {
								this.fullMarkdownGenerationMap.put(annotationKey, propertyAnnotationValueMap);
							}
						});
			}

			logger.info("");
			this.schemas.put(mappedSchema.getName(), mappedSchema);
		}

		return mappedSchema;
	}

	private void downloadOntologies(
			Set<String> configOntologies, String destinationDir, int i, Set<String> ontologyPaths)
			throws OWLOntologyCreationException, IOException {
		for (String ontologyPath : configOntologies) {
			// copy the ontologies used in the destination folder
			final var destinationPath = destinationDir + File.separator + "ontology" + i + ".owl";
			final var ontologyFile = new File(destinationPath);

			// content negotiation + download in case a URI is added
			if (ontologyPath.startsWith("http://") || ontologyPath.startsWith("https://")) {
				// download ontology to local path
				ObaUtils.downloadOntology(ontologyPath, destinationPath);
			} else {
				try {
					// copy to the right folder
					Files.copy(
							new File(ontologyPath).toPath(),
							ontologyFile.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ex) {
					Logger.getLogger(Mapper.class.getName())
							.log(Level.SEVERE, "ERROR while loading file: " + ontologyPath, ex);
					throw ex;
				}
			}

			logger.info("Loaded working ontology file:  " + destinationPath.replace("\\", "/"));
			ontologyPaths.add(destinationPath);

			// Set to silent so missing imports don't make the program fail.
			OWLOntologyLoaderConfiguration loadingConfig = new OWLOntologyLoaderConfiguration();
			loadingConfig =
					loadingConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
			this.manager.loadOntologyFromOntologyDocument(
					new FileDocumentSource(new File(destinationPath)), loadingConfig);
			i++;
		}

		logger.info("");
	}

	/**
	 * Obtain Schemas using the ontology classes The schemas includes all (object and data)
	 * properties.
	 */
	public void createSchemas() {
		final var processedClasses = new HashSet<IRI>();
		for (OWLOntology ontology : this.ontologies) {
			final var format = ontology.getFormat();
			if (format == null) {
				logger.severe("No ontology format found.  Unable to proceed.");
				System.exit(1);
			} else {
				format
						.asPrefixOWLDocumentFormat()
						.getPrefixName2PrefixMap()
						.forEach(
								(prefixName, prefix) -> {
									if (prefixName == null) {
										logger.severe(
												"Unable to proceed.  Prefix name for prefix:  \""
														+ prefix
														+ "\" is invalid.");
										System.exit(1);
									} else if (prefix == null) {
										logger.severe(
												"Unable to proceed.  Prefix for prefix name:  \""
														+ prefixName
														+ "\" is invalid.");
										System.exit(1);
									}

									// Make a copy of the original allowed classes.  Use it for comparison, until this
									// working
									// copy and the allowed classes are equal.
									var workingAllowedClasses = new HashSet<OWLClass>(this.allowedClasses);

									// Add allowed classes to OpenAPI (i.e. remove classes without default ontology
									ontology
											.classesInSignature()
											.filter(
													owlClass ->
															owlClass.getIRI() != null
																	&& !processedClasses.contains(owlClass.getIRI())
																	&& workingAllowedClasses.contains(owlClass))
											.forEach(
													(owlClass) -> {
														processedClasses.add(owlClass.getIRI());
														this.addOwlclassToOpenAPI(owlClass, ontology);
													});

									// After allowed classes have been schema-fied, repeat for all the referenced
									// classes.  If this is not done, the OpenAPI spec may contain references to
									// schemas which do not exist (because they were not explicitly in the allow
									// list).  Looping is done until no new references have been added from the
									// schema-fication process.
									while (!this.allowedClasses.equals(workingAllowedClasses)) {
										workingAllowedClasses.addAll(this.allowedClasses);

										ontology
												.classesInSignature()
												.filter(
														owlClass ->
																workingAllowedClasses.contains(owlClass)
																		&& !processedClasses.contains(owlClass.getIRI())
																		&& !this.schemas
																				.keySet()
																				.contains(owlClass.getIRI().getShortForm()))
												.forEach(
														(owlClass) -> {
															processedClasses.add(owlClass.getIRI());
															this.addOwlclassToOpenAPI(owlClass, ontology);
														});
									}

									// Add all the allowed classes to the map of schema names/IRIs.
									this.setSchemaNames(this.allowedClasses);
								});
			}
		}

		if (this.configData.getAuth().getEnable()) {
			this.addUserPath();
		}
	}

	private void addUserPath() {
		// User schema
		final var userSchema = new Schema();
		userSchema.setName("User");
		userSchema.setType("object");
		// Not using setProperties(), because it creates immutability which breaks unit tests.
		userSchema.addProperty("username", new StringSchema());
		userSchema.addProperty("password", new StringSchema());

		this.schemas.put("User", userSchema);

		this.paths.addPathItem("/user/login", PathGenerator.user_login(userSchema.getName()));
	}

	private void addOwlclassToOpenAPI(OWLClass cls, OWLOntology ontology) {
		try {
			final var mappedSchema = this.getSchema(cls, ontology);

			// If not disabled, and class is allowed, then add the OpenAPI paths
			if (!this.configData.getConfigFlagValue(ConfigFlagType.DISABLE_ALL_PATHS)) {
				if (this.getClassesAllowedByYamlConfig().contains(cls)) {
					// Generate all paths for the class/schema and add to the current Paths object.
					this.paths.putAll(
							PathGenerator.generateAllPathItemsForSchema(
									mappedSchema, cls.getIRI(), this.configData));
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Could not parse class " + cls.getIRI().toString());
			logger.log(Level.SEVERE, "\n\tdetails:\n" + e);
		}
	}

	/**
	 * Set the {@link Map} of schema names to link {@link IRI} with its (short form) name.
	 *
	 * @param classes a {@link Set} of {@link OWLClass}es from an ontology.
	 */
	private void setSchemaNames(Set<OWLClass> classes) {
		for (OWLClass cls : classes) {
			this.schemaNames.put(cls.getIRI(), cls.getIRI().getShortForm());
		}
	}

	/**
	 * Get a map of IRIs and their short form names for the schemas generated.
	 *
	 * @return a {@link Map} of {@link IRI} keys and short form {@link String} values
	 */
	public Map<IRI, String> getSchemaNames() {
		return this.schemaNames;
	}

	/**
	 * Get a map of names and schemas for each class of the ontology/ies (that are allowed, according to the configuration file).
	 *
	 * @return a {@link Map> of short form name {@link String} keys and their {@link Schema} values
	 */
	public Map<String, Schema> getSchemas() {
		return new TreeMap<>(this.schemas);
	}

	/**
	 * Get all API paths from the OpenAPI spec.
	 *
	 * @return A {@link Paths} object from Swagger's OAS model.
	 */
	public Paths getPaths() {
		return this.paths;
	}

	/**
	 * Get set of allowed classes. Returns all classes, if no restrictions in configuration file.
	 *
	 * @return a {@link Set} of {@link OWLClass}es that are allowed by the config file.
	 */
	private Set<OWLClass> getClassesAllowedByYamlConfig() {
		final var allowedClassesByIRI = this.configData.getClasses();
		final var allowedClasses = new HashSet<OWLClass>();

		this.ontologies.forEach(
				(ontology) -> {
					// If the configuration contains no allowed classes, then add all classes from the
					// ontology.
					if (allowedClassesByIRI == null || allowedClassesByIRI.isEmpty()) {
						allowedClasses.addAll(ontology.getClassesInSignature());
					} else {
						ontology
								.classesInSignature()
								.filter(owlClass -> allowedClassesByIRI.contains(owlClass.getIRI().toString()))
								.forEach(
										(allowedClass) -> {
											allowedClasses.add(allowedClass);
										});
					}
				});

		return allowedClasses;
	}

	/**
	 * Get all the markdown generation mappings for all the ontologies defined in the configuration
	 * file.
	 *
	 * @return a {@link Map} containing annotation mappings to mappings of property name and
	 *     annotation value.
	 */
	public Map<String, Map<String, String>> getFullMarkdownMappings() {
		return this.fullMarkdownGenerationMap;
	}
}
