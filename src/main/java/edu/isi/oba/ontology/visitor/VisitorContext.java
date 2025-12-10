package edu.isi.oba.ontology.visitor;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.exceptions.OntologyVisitorException;
import edu.isi.oba.ontology.reasoner.ReasonerUtil;
import edu.isi.oba.ontology.schema.SchemaBuilder;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

/**
 * Internal context object used by {@link ObjectVisitor} to track mutable state during OWL
 * traversal. This class implements {@link OwlVisitorContext} to expose read-only access to helper
 * classes.
 */
public class VisitorContext implements OwlVisitorContext {

	private final Logger logger;

	/** Configuration loaded from YAML file. */
	private final YamlConfig configData;

	/** The base OWL class being visited. */
	private OWLClass baseClass;

	/** The ontology containing the base class. */
	private OWLOntology baseClassOntology;

	/** The OpenAPI schema being built for the base class. */
	private Schema classSchema;

	/** Reasoner used for ontology inference. */
	private OWLReasoner reasoner;

	/** The top-level OWL class (usually owl:Thing). */
	private OWLClass owlThing;

	/** Map of property names to their OpenAPI schemas. */
	private final Map<String, Schema> basePropertiesMap = new HashMap<>();

	/** Set of all property names encountered. */
	private final Set<String> propertyNames = new HashSet<>();

	/** Set of property names that are enums. */
	private final Set<String> enumProperties = new HashSet<>();

	/** Set of required property names. */
	private final Set<String> requiredProperties = new HashSet<>();

	/** Set of functional property names. */
	private final Set<String> functionalProperties = new HashSet<>();

	/** Set of OWL classes referenced by the base class. */
	private final Set<OWLClass> referencedClasses = new HashSet<>();

	/** Set of OWL classes already processed during traversal. */
	private final Set<OWLClass> processedClasses = new HashSet<>();

	/** Set of OWL classes already processed for restrictions. */
	private final Set<OWLClass> processedRestrictionClasses = new HashSet<>();

	/** Map of markdown annotation names to property/class mappings. */
	private final Map<String, Map<String, String>> markdownGenerationMap = new TreeMap<>();

	/**
	 * The name of the property currently being processed. Used during restriction traversal and
	 * annotation mapping.
	 */
	private String currentlyProcessedPropertyName = null;

	// === Constructor ===

	/**
	 * Constructs a new VisitorContext with the given configuration.
	 *
	 * @param configData the loaded YAML configuration
	 */
	public VisitorContext(
			OWLClass baseClass, OWLOntology baseClassOntology, YamlConfig configData, Logger logger)
			throws OntologyVisitorException {
		this.baseClass = baseClass;
		this.baseClassOntology = baseClassOntology;
		this.configData = configData;
		this.logger = logger;

		this.validateConstructorArguments();

		this.reasoner = ReasonerUtil.createReasoner(this.baseClassOntology);
		this.owlThing = this.reasoner.getTopClassNode().getRepresentativeElement();

		this.classSchema =
				SchemaBuilder.getBaseClassBasicSchema(this.baseClass, this.baseClassOntology);
	}

	private void validateConstructorArguments() throws OntologyVisitorException {
		// Cannot create a Schema without a base OWLClass
		if (this.baseClass == null) {
			throw new OntologyVisitorException(
					"You must set the OWLClass when creating a visitor context.");
		}

		// Cannot create a Reasoner without a valid OWLOntology or one that doesn't contain the base
		// OWLClass
		if (this.baseClassOntology == null) {
			throw new OntologyVisitorException(
					"Ontology was set to null when creating ObjectVisitor.  Unable to proceed.");
		} else if (!this.baseClassOntology.containsClassInSignature(this.baseClass.getIRI())) {
			throw new OntologyVisitorException(
					"Ontology used when creating ObjectVisitor does not contain the class you are"
							+ " attempting to visit.  Unable to proceed.");
		}
	}

	// === Configuration ===
	@Override
	public OWLOntology getBaseClassOntology() {
		return this.baseClassOntology;
	}

	/**
	 * Sets the ontology that contains the base OWL class.
	 *
	 * @param baseClassOntology the OWL ontology to set
	 */
	public void setBaseClassOntology(OWLOntology baseClassOntology) {
		this.baseClassOntology = baseClassOntology;
	}

	@Override
	public OWLClass getBaseClass() {
		return this.baseClass;
	}

	/**
	 * Sets the base OWL class being visited.
	 *
	 * @param baseClass the OWL class to set
	 */
	public void setBaseClass(OWLClass baseClass) {
		this.baseClass = baseClass;
	}

	@Override
	public Schema getClassSchema() {
		return this.classSchema;
	}

	/**
	 * Sets the OpenAPI schema for the base OWL class.
	 *
	 * @param classSchema the schema to set
	 */
	public void setClassSchema(Schema classSchema) {
		this.classSchema = classSchema;
	}

	@Override
	public YamlConfig getConfigData() {
		return this.configData;
	}

	@Override
	public String getCurrentlyProcessedPropertyName() {
		return this.currentlyProcessedPropertyName;
	}

	// === Reasoner Access ===
	@Override
	public OWLReasoner getReasoner() {
		return this.reasoner;
	}

	/**
	 * Sets the OWL reasoner used for inference.
	 *
	 * @param reasoner the OWL reasoner to set
	 */
	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	@Override
	public OWLClass getOwlThing() {
		return this.owlThing;
	}

	// === Schema Management ===
	@Override
	public Map<String, Schema> getBasePropertiesMap() {
		return Collections.unmodifiableMap(this.basePropertiesMap);
	}

	/**
	 * Adds all entries to the base properties map.
	 *
	 * @param addedPropertiesMap the map of properties to add
	 */
	public void addAllBasePropertiesMap(Map<String, Schema> addedPropertiesMap) {
		this.basePropertiesMap.putAll(addedPropertiesMap);
	}

	/**
	 * Adds a property schema to the base class schema and internal property map.
	 *
	 * @param name the name of the property
	 * @param schema the OpenAPI schema for the property
	 */
	public void addPropertyToSchema(String name, Schema schema) {
		this.basePropertiesMap.put(name, schema);
		if (this.classSchema == null) {
			this.logger.log(Level.WARNING, "classSchema IS NULL WHEN ADDING PROPERTY: " + name);
		}

		if (this.classSchema != null) {
			this.classSchema.addProperty(name, schema);
		}
	}

	// === Required Properties ===
	/**
	 * Returns the set of required property names for the base class.
	 *
	 * @return a mutable set of required property names
	 */
	@Override
	public Set<String> getRequiredProperties() {
		return Collections.unmodifiableSet(this.requiredProperties);
	}

	/** Clears all required property names. */
	public void clearRequiredProperties() {
		this.requiredProperties.clear();
	}

	/**
	 * Adds a single required property name.
	 *
	 * @param requiredProperty the property name to add
	 */
	public void addRequiredProperty(String requiredProperty) {
		this.requiredProperties.add(requiredProperty);
	}

	/**
	 * Adds multiple required property names.
	 *
	 * @param requiredProperties the set of property names to add
	 */
	public void addAllRequiredProperties(Set<String> requiredProperties) {
		this.requiredProperties.addAll(requiredProperties);
	}

	/**
	 * Marks a property as required in both internal tracking and the OpenAPI schema.
	 *
	 * @param name the name of the required property
	 */
	public void markPropertyAsRequired(String name) {
		this.requiredProperties.add(name);
		if (this.classSchema != null) {
			List<String> required = this.classSchema.getRequired();
			if (required == null) {
				required = new ArrayList<>();
			}

			final var requiredSet = required.stream().collect(Collectors.toSet());
			requiredSet.add(name);
			this.classSchema.setRequired(requiredSet.stream().collect(Collectors.toList()));
		}
	}

	// === Property Tracking ===
	@Override
	public Set<String> getFunctionalProperties() {
		return Collections.unmodifiableSet(this.functionalProperties);
	}

	/**
	 * Adds a functional property name.
	 *
	 * @param name the property name to add
	 */
	public void addFunctionalProperty(String name) {
		this.functionalProperties.add(name);
	}

	@Override
	public Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(this.propertyNames);
	}

	/**
	 * Adds a property name to the set of encountered properties.
	 *
	 * @param name the property name to add
	 */
	public void addPropertyName(String name) {
		this.propertyNames.add(name);
	}

	/**
	 * Adds a property {@link Schema} to the class {@link Schema}
	 *
	 * @param propertyName the property name
	 * @param propertySchema the property {@link Schema}
	 */
	public void addPropertySchemaToClassSchema(String propertyName, Schema propertySchema) {
		this.classSchema.addProperty(propertyName, propertySchema);
	}

	@Override
	public Set<String> getEnumProperties() {
		return Collections.unmodifiableSet(this.enumProperties);
	}

	/**
	 * Adds an enum property name.
	 *
	 * @param name the enum property name to add
	 */
	public void addEnumProperty(String name) {
		this.enumProperties.add(name);
	}

	// === Referenced Classes ===
	@Override
	public Set<OWLClass> getReferencedClasses() {
		return Collections.unmodifiableSet(this.referencedClasses);
	}

	public void addAllReferencedClasses(Set<OWLClass> addedReferencedClasses) {
		this.referencedClasses.addAll(addedReferencedClasses);
	}

	public void removeReferencedClass(OWLClass owlClass) {
		this.referencedClasses.remove(owlClass);
	}

	/**
	 * Adds a referenced OWL class to the set of classes referenced by the base class.
	 *
	 * @param cls the OWL class to add
	 */
	public void addReferencedClass(OWLClass cls) {
		this.referencedClasses.add(cls);
	}

	/**
	 * Adds multiple referenced OWL classes to the set of classes referenced by the base class.
	 *
	 * @param classes a collection of OWL classes to add
	 */
	public void addReferencedClasses(Collection<OWLClass> classes) {
		this.referencedClasses.addAll(classes);
	}

	// === Processed Classes ===
	@Override
	public Set<OWLClass> getProcessedClasses() {
		return Collections.unmodifiableSet(this.processedClasses);
	}

	/**
	 * Adds an OWL class to the set of processed classes.
	 *
	 * @param owlClass the OWL class to add
	 */
	public void addProcessedClass(OWLClass owlClass) {
		this.processedClasses.add(owlClass);
	}

	/**
	 * Marks an OWL class as processed during traversal.
	 *
	 * @param cls the OWL class to mark as processed
	 */
	public void markClassAsProcessed(OWLClass cls) {
		this.processedClasses.add(cls);
	}

	/**
	 * Checks whether the given OWL class has already been processed.
	 *
	 * @param cls the OWL class to check
	 * @return true if the class has been processed; false otherwise
	 */
	public boolean isClassProcessed(OWLClass cls) {
		return this.processedClasses.contains(cls);
	}

	// === Restriction Classes ===
	@Override
	public Set<OWLClass> getProcessedRestrictionClasses() {
		return Collections.unmodifiableSet(this.processedRestrictionClasses);
	}

	/**
	 * Adds an OWL class to the set of processed restriction classes.
	 *
	 * @param owlClass the OWL class to add
	 */
	public void addProcessedRestrictionClass(OWLClass owlClass) {
		this.processedRestrictionClasses.add(owlClass);
	}

	@Override
	public boolean isRestrictionClassProcessed(OWLClass cls) {
		return this.processedRestrictionClasses.contains(cls);
	}

	// === Markdown ===
	@Override
	public Map<String, Map<String, String>> getMarkdownGenerationMap() {
		return Collections.unmodifiableMap(this.markdownGenerationMap);
	}

	/**
	 * Adds a markdown annotation entry for a given name.
	 *
	 * @param name the name of the entry
	 * @param annotationMap the map of annotations
	 */
	public void addMarkdownGenerationMapEntry(String name, Map<String, String> annotationMap) {
		this.markdownGenerationMap.put(name, annotationMap);
	}

	/**
	 * Adds multiple markdown annotation entries.
	 *
	 * @param newEntries the map of entries to add
	 */
	public void addAllMarkdownGenerationMapEntries(Map<String, Map<String, String>> newEntries) {
		this.markdownGenerationMap.putAll(newEntries);
	}

	/**
	 * Adds a markdown annotation entry for a property.
	 *
	 * @param property the property name
	 * @param annotations the map of annotations
	 */
	public void addMarkdownEntry(String property, Map<String, String> annotations) {
		this.markdownGenerationMap.put(property, annotations);
	}

	// === Property Context ===

	/**
	 * Sets the name of the property currently being processed. This is used during restriction
	 * traversal and annotation mapping.
	 *
	 * @param name the name of the property
	 */
	public void setCurrentlyProcessedPropertyName(String name) {
		this.currentlyProcessedPropertyName = name;
	}

	/** Clears the name of the property currently being processed. */
	public void clearCurrentlyProcessedPropertyName() {
		this.currentlyProcessedPropertyName = null;
	}

	/**
	 * Executes a block of code with a temporary property name set for processing. Automatically
	 * clears the property name after execution.
	 *
	 * @param name the property name to set
	 * @param action the code block to execute
	 */
	public void withProcessedProperty(String name, Runnable action) {
		setCurrentlyProcessedPropertyName(name);
		try {
			action.run();
		} finally {
			clearCurrentlyProcessedPropertyName();
		}
	}
}
