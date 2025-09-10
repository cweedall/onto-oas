package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.generators.PathGenerator;
import edu.isi.oba.ontology.visitor.ObjectVisitor;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import edu.isi.oba.utils.schema.SchemaRefUtils;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

class Mapper {
	private final Map<IRI, String> schemaNames = new HashMap<>(); // URI-names of the schemas
	private final Map<String, Schema> schemas = new HashMap<>();
	private final Map<IRI, Schema> iriSchemaMap = new HashMap<>();
	private final Paths paths = new Paths();
	private final YamlConfig configData;

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
	}

	private Schema getSchema(OWLClass cls, OWLOntology ontology) {
		logger.info("=======================================================================");
		logger.info("##############################################");
		logger.info("###  Beginning schema mapping for class:");
		logger.info("###\t" + cls);
		logger.info("##############################################");

		// Convert from OWL Class to OpenAPI Schema.
		final var objVisitor = new ObjectVisitor(this.configData);
		objVisitor.visit(ontology, cls);

		final var mappedSchema = objVisitor.getClassSchema();

		// Each time we generate a class's schema, there may be referenced classes that need to be added
		// to the set of allowed classes.
		this.configData.addAllReferencedOwlClasses(objVisitor.getAllReferencedClasses());

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
			this.iriSchemaMap.put(cls.getIRI(), mappedSchema);
		}

		return mappedSchema;
	}

	/**
	 * Obtain Schemas using the ontology classes The schemas includes all (object and data)
	 * properties.
	 */
	public void createSchemas() {
		final var processedClasses = new HashSet<IRI>();
		for (OWLOntology ontology : this.configData.getOwlOntologies()) {
			final var format = ontology.getFormat();
			if (format == null) {
				FatalErrorHandler.fatal("No ontology format found.  Unable to proceed.");
			} else {
				format
						.asPrefixOWLDocumentFormat()
						.getPrefixName2PrefixMap()
						.forEach(
								(prefixName, prefix) -> {
									if (prefixName == null) {
										FatalErrorHandler.fatal(
												"Unable to proceed.  Prefix name for prefix:  \""
														+ prefix
														+ "\" is invalid.");

									} else if (prefix == null) {
										FatalErrorHandler.fatal(
												"Unable to proceed.  Prefix for prefix name:  \""
														+ prefixName
														+ "\" is invalid.");
									}

									// Make a copy of the original allowed classes.  Use it for comparison, until this
									// working
									// copy and the allowed classes are equal.
									var workingAllowedClasses = this.configData.getAllReferencedOwlClasses();

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
									while (!this.configData
											.getAllReferencedOwlClasses()
											.equals(workingAllowedClasses)) {
										workingAllowedClasses.addAll(this.configData.getAllReferencedOwlClasses());

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
									this.setSchemaNames(this.configData.getAllReferencedOwlClasses());
								});
			}
		}

		if (this.configData.getAuth() != null && this.configData.getAuth().getEnable()) {
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
			if (!GlobalFlags.getFlag(ConfigPropertyNames.DISABLE_ALL_PATHS)) {
				if (this.configData.getPathConfig().getPathClasses().contains(cls.getIRI())) {
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
		// If we are removing references, call the utility to do so.  Then remove all schemas not
		// explicitly allowed in the config file.
		if (!GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)) {
			final var derefSchemas = SchemaRefUtils.getDereferencedSchemasParallel(this.schemas);
			this.schemas.clear();
			derefSchemas.keySet().retainAll(configData.getAllowedClasses());
			this.schemas.putAll(derefSchemas);
		}

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
