package edu.isi.oba.ontology.schema;

import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.ontology.annotation.markdown.MarkdownAnnotationProcessor;
import edu.isi.oba.ontology.visitor.VisitorContext;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.search.EntitySearcher;

public class SchemaOrchestrator {
	private final Logger logger;
	private final VisitorContext context;
	private final Schema schema;
	private final MarkdownAnnotationProcessor mdProcessor;

	public SchemaOrchestrator(VisitorContext context, Logger logger) {
		Objects.requireNonNull(context.getClassSchema(), "The context's class schema must not be null");

		this.context = context;
		this.schema = this.context.getClassSchema();
		this.logger = logger;
		this.mdProcessor = new MarkdownAnnotationProcessor(this.context, this.logger);
	}

	/**
	 * Generates the OpenAPI schema for the current OWL class. This method orchestrates the schema
	 * construction process by:
	 *
	 * <ul>
	 *   <li>Cleaning up enum properties and handling OWL complement logic
	 *   <li>Determining required properties based on cardinality
	 *   <li>Adjusting array schemas based on configuration
	 *   <li>Adding inheritance references using OWL superclass relationships
	 *   <li>Pruning unused referenced classes from the schema
	 *   <li>Embedding Markdown content from axiom annotations
	 * </ul>
	 *
	 * @return the constructed {@link Schema} object for the OWL class
	 */
	public Schema generateSchema() {
		logger.log(Level.FINE, "Starting schema cleanup for enum and complement properties...");
		this.cleanUpEnumProperties();

		logger.log(Level.FINE, "Generating required properties based on cardinality...");
		this.generateRequiredProperties();

		logger.log(Level.FINE, "Converting array properties based on configuration...");
		this.convertArrayProperties();

		logger.log(Level.FINE, "Handling inheritance references from OWL superclasses...");
		this.handleInheritanceReferences();

		logger.log(Level.FINE, "Pruning unused referenced classes from schema...");
		this.pruneUnusedReferencedClasses();

		logger.log(Level.FINE, "Setting Markdown content from axiom annotations...");
		this.mdProcessor.setMarkdownContentFromAxiomAnnotations();

		logger.log(Level.FINE, "Schema generation complete.");

		return this.schema;
	}

	private void cleanUpEnumProperties() {
		final Map<String, Schema> classProperties = this.schema.getProperties();
		if (classProperties != null) {
			classProperties
					.values()
					.forEach(
							(propertySchema) -> {
								final var propSchema = (Schema) propertySchema;
								final var itemsSchema = propSchema.getItems();
								if (propSchema.getItems() == null) {
									MapperProperty.setSchemaType(propSchema, "object");
								} else {
									// If a property has a default value AND an enum list with only one element, then
									// remove the enum list.
									final var enumList = itemsSchema.getEnum();
									if (enumList != null
											&& enumList.size() == 1
											&& itemsSchema.getDefault() != null) {
										itemsSchema.setEnum(null);
									}
								}
							});
		}
	}

	private void generateRequiredProperties() {
		// Enums are already set up.  They should also never have required properties (even if
		// inherited somehow).
		if (this.schema.getEnum() != null) {
			this.context.clearRequiredProperties();
		}

		// Generate the required properties for the class, if applicable.
		if (GlobalFlags.getFlag(ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY)) {
			SchemaBuilder.generateRequiredPropertiesForClassSchemas(
					this.schema, this.context.getFunctionalProperties());
		}
	}

	private void convertArrayProperties() {
		// Convert non-array property items, if applicable.
		if (!GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS)) {
			MapperProperty.convertArrayToNonArrayPropertySchemas(
					this.schema,
					this.context.getEnumProperties(),
					this.context.getFunctionalProperties(),
					GlobalFlags.getFlag(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES));

			// If there are required properties, they may have changes (i.e. pluralized or singularized).
			// Make sure to clear and re-populate the Set/List.  (Primiarily done to keep everything in
			// alphabetical order)
			if (!this.context.getRequiredProperties().isEmpty()) {
				this.context.clearRequiredProperties();

				if (this.schema.getRequired() != null && !this.schema.getRequired().isEmpty()) {
					this.context.addAllRequiredProperties(
							(Set<String>) this.schema.getRequired().stream().collect(Collectors.toSet()));
					this.context
							.getClassSchema()
							.setRequired(
									this.context.getRequiredProperties().stream().collect(Collectors.toList()));
				}
			}
		}
	}

	private void handleInheritanceReferences() {
		// If following references AND use inheritance references (for the class), we do not want to
		// inherit/reference the same class multiple times accidentally. (e.g. if we have Person >
		// Student > ExchangeStudent, Student already inherits everything from Person.  For
		// ExchangeStudent, we do not want to inherit from Person AND Student. We only need to inherit
		// from Student [which automatically inherits everything from Person also].)
		if (this.schema.getEnum() == null
				&& GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)
				&& GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)) {

			// If adding for the first time, need to include a "type: object" entry.
			if (this.schema.getAllOf() == null) {
				final var objSchema = new ObjectSchema();
				this.schema.addAllOfItem(objSchema);
			}

			// All processed classes, minus the base class, are the super classes.
			final var superClasses = new HashSet<OWLClass>(this.context.getProcessedClasses());
			superClasses.remove(this.context.getBaseClass());

			// Make a copy of the super classes.  Loop through all super classes and remove any
			// super-super-classes that are being inherited by a nearer/more direct super class to the
			// base class.
			final var directSuperClasses = new HashSet<OWLClass>(superClasses);
			for (OWLClass superClassA : superClasses) {
				for (OWLClass superClassB : superClasses) {
					if (!superClassA.equals(superClassB)
							&& this.context
									.getReasoner()
									.getSuperClasses(superClassA, false)
									.containsEntity(superClassB)) {
						directSuperClasses.remove(superClassB);
					}
				}
			}

			// Add all direct superclasses to allOf list.
			directSuperClasses.stream()
					.forEach(
							superClass -> {
								final var refSchema = new ObjectSchema();
								refSchema.set$ref(
										"#/components/schemas/"
												+ SchemaBuilder.getPrefixedSchemaName(
														superClass, this.context.getBaseClassOntology()));

								this.schema.addAllOfItem(refSchema);
							});
		}

		// After checking/processing, if the base class schema has properties but no AllOf list and no
		// type, set its type to "object".
		if (this.schema.getProperties() != null
				&& this.schema.getAllOf() == null
				&& this.schema.getType() == null) {
			MapperProperty.setSchemaType(this.schema, "object");
		}
	}

	private void pruneUnusedReferencedClasses() {
		for (final var refClass : new HashSet<OWLClass>(this.context.getReferencedClasses())) {
			// Indicator that referenced class has one or more equivalent classes.
			final boolean hasEquivalentClasses =
					EntitySearcher.getEquivalentClasses(refClass, this.context.getBaseClassOntology()).count()
							> 0;

			// Indicator that referenced class has (subclass) properties.
			boolean hasSubClassProperties = false;

			// Indicator that referenced class is the range class for one or more object properties.
			boolean isDomainForDataProperty = false;

			// Indicator that referenced class is the range class for one or more object properties.
			boolean isRangeForObjectProperty = false;

			// Look for all subclass axioms where the expression type is not an OWL CLass (i.e. it is a
			// restriction).  This means there is an axiom indicating a "subclass of" property
			// restriction is declared for the class.
			for (final var ax :
					this.context.getBaseClassOntology().getSubClassAxiomsForSubClass(refClass)) {
				for (final var nce : ax.getNestedClassExpressions()) {

					if (!nce.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
						hasSubClassProperties = true;
					}
				}
			}

			// Look at all data properties.  If any of them contain a domain which is the current
			// referenced class, then set flag to keep the reference class to true.
			for (final var op : this.context.getBaseClassOntology().getDataPropertiesInSignature()) {
				for (final var dataPropDomainAx :
						this.context.getBaseClassOntology().getDataPropertyDomainAxioms(op)) {
					if (dataPropDomainAx.getClassesInSignature().contains(refClass)) {
						isDomainForDataProperty = true;
					}
				}
			}

			final Map<String, Schema> classProperties = this.schema.getProperties();

			// Look at all object properties for the base class.  If any of them contain a range which is
			// the current referenced class, then set flag to keep the reference class to true.
			for (final var op : this.context.getBaseClassOntology().getObjectPropertiesInSignature()) {
				if (classProperties != null && classProperties.containsKey(op.getIRI().getShortForm())) {
					for (final var objPropRangeAx :
							this.context
									.getBaseClassOntology()
									.getObjectPropertyRangeAxioms(op.asObjectPropertyExpression())) {
						if (objPropRangeAx.getClassesInSignature().contains(refClass)) {
							isRangeForObjectProperty = true;
						}
					}
				}
			}

			// If the referenced class is empty and not actually referenced (e.g. it may have been nested
			// or a super class), the remove it.
			if (!hasSubClassProperties
					&& !hasEquivalentClasses
					&& !isDomainForDataProperty
					&& !isRangeForObjectProperty) {
				// Remove from the set of referenced classes.
				this.context.removeReferencedClass(refClass);

				// Also remove from the AllOf list of schemas, if applicable.
				if (this.schema.getEnum() == null) {
					if (this.schema.getAllOf() != null) {
						for (final var allOfSchema : new ArrayList<Schema>(this.schema.getAllOf())) {
							if (allOfSchema.get$ref() != null
									&& allOfSchema
											.get$ref()
											.contains(
													"#/components/schemas/"
															+ SchemaBuilder.getPrefixedSchemaName(
																	refClass, this.context.getBaseClassOntology()))) {
								this.schema.getAllOf().remove(allOfSchema);
							}
						}
					}
				}
			}
		}
	}
}
