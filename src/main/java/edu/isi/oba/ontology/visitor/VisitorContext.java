package edu.isi.oba.ontology.visitor;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Internal context object used by {@link ObjectVisitor} to track mutable state during OWL
 * traversal. This class implements {@link OwlVisitorContext} to expose read-only access to helper
 * classes.
 */
class VisitorContext implements OwlVisitorContext {

	/** Configuration loaded from YAML file. */
	final YamlConfig configData;

	/** The base OWL class being visited. */
	OWLClass baseClass;

	/** The ontology containing the base class. */
	OWLOntology baseClassOntology;

	/** The OpenAPI schema being built for the base class. */
	Schema classSchema;

	/** Reasoner used for ontology inference. */
	OWLReasoner reasoner;

	/** Factory used to create the reasoner. */
	OWLReasonerFactory reasonerFactory;

	/** The top-level OWL class (usually owl:Thing). */
	OWLClass owlThing;

	/** Map of property names to their OpenAPI schemas. */
	final Map<String, Schema> basePropertiesMap = new HashMap<>();

	/** Set of all property names encountered. */
	final Set<String> propertyNames = new HashSet<>();

	/** Set of property names that are enums. */
	final Set<String> enumProperties = new HashSet<>();

	/** Set of required property names. */
	final Set<String> requiredProperties = new HashSet<>();

	/** Set of functional property names. */
	final Set<String> functionalProperties = new HashSet<>();

	/** Set of OWL classes referenced by the base class. */
	final Set<OWLClass> referencedClasses = new HashSet<>();

	/** Set of OWL classes already processed during traversal. */
	final Set<OWLClass> processedClasses = new HashSet<>();

	/** Set of OWL classes already processed for restrictions. */
	final Set<OWLClass> processedRestrictionClasses = new HashSet<>();

	/** Map of markdown annotation names to property/class mappings. */
	final Map<String, Map<String, String>> markdownGenerationMap = new TreeMap<>();

	/**
	 * The name of the property currently being processed. Used during restriction traversal and
	 * annotation mapping.
	 */
	String currentlyProcessedPropertyName = null;

	/**
	 * Constructs a new VisitorContext with the given configuration.
	 *
	 * @param configData the loaded YAML configuration
	 */
	VisitorContext(YamlConfig configData) {
		this.configData = configData;
	}

	// OwlVisitorContext} interface methods

	@Override
	public OWLOntology getBaseClassOntology() {
		return baseClassOntology;
	}

	@Override
	public OWLClass getBaseClass() {
		return baseClass;
	}

	@Override
	public Schema getClassSchema() {
		return classSchema;
	}

	@Override
	public Set<OWLClass> getReferencedClasses() {
		return referencedClasses;
	}

	@Override
	public Map<String, Schema> getBasePropertiesMap() {
		return basePropertiesMap;
	}

	/**
	 * Returns the set of required property names for the base class.
	 *
	 * @return a mutable set of required property names
	 */
	@Override
	public Set<String> getRequiredProperties() {
		return requiredProperties;
	}

	@Override
	public Set<OWLClass> getProcessedClasses() {
		return processedClasses;
	}

	@Override
	public Map<String, Map<String, String>> getMarkdownGenerationMap() {
		return markdownGenerationMap;
	}

	// Add other overrides as needed

	/**
	 * Adds a referenced OWL class to the set of classes referenced by the base class.
	 *
	 * @param cls the OWL class to add
	 */
	public void addReferencedClass(OWLClass cls) {
		referencedClasses.add(cls);
	}

	/**
	 * Adds multiple referenced OWL classes to the set of classes referenced by the base class.
	 *
	 * @param classes a collection of OWL classes to add
	 */
	public void addReferencedClasses(Collection<OWLClass> classes) {
		referencedClasses.addAll(classes);
	}

	/**
	 * Marks an OWL class as processed during traversal.
	 *
	 * @param cls the OWL class to mark as processed
	 */
	public void markClassAsProcessed(OWLClass cls) {
		processedClasses.add(cls);
	}

	/**
	 * Checks whether the given OWL class has already been processed.
	 *
	 * @param cls the OWL class to check
	 * @return true if the class has been processed; false otherwise
	 */
	public boolean isClassProcessed(OWLClass cls) {
		return processedClasses.contains(cls);
	}

	/**
	 * Adds a property schema to the base class schema and internal property map.
	 *
	 * @param name the name of the property
	 * @param schema the OpenAPI schema for the property
	 */
	public void addPropertyToSchema(String name, Schema schema) {
		basePropertiesMap.put(name, schema);
		if (classSchema == null) {
			logger.warning("classSchema IS NULL WHEN ADDING PROPERTY: " + name);
		}

		if (classSchema != null) {
			classSchema.addProperty(name, schema);
		}
	}

	/**
	 * Marks a property as required in both internal tracking and the OpenAPI schema.
	 *
	 * @param name the name of the required property
	 */
	public void markPropertyAsRequired(String name) {
		requiredProperties.add(name);
		if (classSchema != null) {
			List<String> required = classSchema.getRequired();
			if (required == null) {
				required = new ArrayList<>();
				classSchema.setRequired(required);
			}
			if (!required.contains(name)) {
				required.add(name);
			}
		}
	}

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
