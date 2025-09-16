package edu.isi.oba.ontology.visitor;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.MapperDataProperty;
import edu.isi.oba.MapperObjectProperty;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.ontology.schema.SchemaBuilder;
import edu.isi.oba.utils.constants.ObaConstants;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import edu.isi.oba.utils.ontology.OntologyDescriptionUtils;
import edu.isi.oba.utils.schema.SchemaCloneUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataRestriction;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLNaryDataRange;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectRestriction;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLObjectVisitor;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLQuantifiedDataRestriction;
import org.semanticweb.owlapi.model.OWLQuantifiedObjectRestriction;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.reasoner.InferenceDepth;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

/** Visits existential restrictions and collects the properties which are restricted. */
public class ObjectVisitor implements OWLObjectVisitor {

	private final VisitorContext context;

	/**
	 * Constructor for ObjectVisitor.
	 *
	 * <p>Initializes the visitor context using the provided configuration.
	 *
	 * @param configData a {@link YamlConfig} containing all details loaded from the configuration
	 *     file.
	 */
	public ObjectVisitor(YamlConfig configData) {
		this.context = new VisitorContext(configData);
	}

	/**
	 * Using the specified base OWLClass, determine its ontology, a reasoner for the ontology, the
	 * owl:Thing from the ontology, and the basic schema (e.g. name, description, type, default
	 * properties).
	 *
	 * @param baseClass an {@link OWLClass} which should be treated as the primary/base class for this
	 *     visitor class.
	 */
	private void initializeBaseClass(OWLClass baseClass) {
		// If the base class is already set, ignore.
		if (context.baseClass == null) {
			context.baseClass = baseClass;

			context.reasonerFactory = new StructuralReasonerFactory();

			// Basic error checking to verify ontology is valid and class exists within it.
			if (context.baseClassOntology == null) {
				FatalErrorHandler.fatal(
						"Ontology was set to null when creating ObjectVisitor.  Unable to proceed.");
			} else if (!context.baseClassOntology.containsClassInSignature(context.baseClass.getIRI())) {
				FatalErrorHandler.fatal(
						"Ontology used when creating ObjectVisitor does not contain the class you are"
								+ " attempting to visit.  Unable to proceed.");
			}

			context.reasoner = context.reasonerFactory.createReasoner(context.baseClassOntology);
			context.owlThing = context.reasoner.getTopClassNode().getRepresentativeElement();

			context.classSchema =
					SchemaBuilder.getBaseClassBasicSchema(context.baseClass, context.baseClassOntology);
		}
	}

	/**
	 * Get the prefixed name and class name, depending on the full IRI of the OWL class and list of
	 * ontology prefixes.
	 *
	 * @param owlClass an OWL class to get the fully prefixed name for.
	 * @return a {@link String} in the format of "PREFIX-CLASSNAME"
	 */
	private String getPrefixedSchemaName(OWLClass owlClass) {
		return SchemaBuilder.getPrefixedSchemaName(owlClass, context.baseClassOntology);
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
	public Schema getClassSchema() {
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
		this.setMarkdownContentFromAxiomAnnotations();

		logger.log(Level.FINE, "Schema generation complete.");
		return context.classSchema;
	}

	private void cleanUpEnumProperties() {
		final Map<String, Schema> classProperties =
				context.classSchema == null ? null : context.classSchema.getProperties();
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
		if (context.classSchema.getEnum() != null) {
			context.requiredProperties.clear();
		}

		// Generate the required properties for the class, if applicable.
		if (GlobalFlags.getFlag(ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY)) {
			SchemaBuilder.generateRequiredPropertiesForClassSchemas(
					context.classSchema, context.functionalProperties);
		}
	}

	private void convertArrayProperties() {
		// Convert non-array property items, if applicable.
		if (!GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS)) {
			MapperProperty.convertArrayToNonArrayPropertySchemas(
					context.classSchema,
					context.enumProperties,
					context.functionalProperties,
					GlobalFlags.getFlag(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES));

			// If there are required properties, they may have changes (i.e. pluralized or singularized).
			// Make sure to clear and re-populate the Set/List.  (Primiarily done to keep everything in
			// alphabetical order)
			if (!context.requiredProperties.isEmpty()) {
				context.requiredProperties.clear();

				if (context.classSchema.getRequired() != null
						&& !context.classSchema.getRequired().isEmpty()) {
					context.requiredProperties.addAll(
							(Set<String>) context.classSchema.getRequired().stream().collect(Collectors.toSet()));
					context.classSchema.setRequired(
							context.requiredProperties.stream().collect(Collectors.toList()));
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
		if (context.classSchema.getEnum() == null
				&& GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)
				&& GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)) {

			// If adding for the first time, need to include a "type: object" entry.
			if (context.classSchema.getAllOf() == null || context.classSchema.getAllOf().isEmpty()) {
				final var objSchema = new ObjectSchema();
				context.classSchema.addAllOfItem(objSchema);
			}

			// All processed classes, minus the base class, are the super classes.
			final var superClasses = new HashSet<OWLClass>(context.processedClasses);
			superClasses.remove(context.baseClass);

			// Make a copy of the super classes.  Loop through all super classes and remove any
			// super-super-classes that are being inherited by a nearer/more direct super class to the
			// base class.
			final var directSuperClasses = new HashSet<OWLClass>(superClasses);
			for (OWLClass superClassA : superClasses) {
				for (OWLClass superClassB : superClasses) {
					if (!superClassA.equals(superClassB)
							&& context.reasoner.getSuperClasses(superClassA, false).containsEntity(superClassB)) {
						directSuperClasses.remove(superClassB);
					}
				}
			}

			// Add all direct superclasses to allOf list.
			directSuperClasses.stream()
					.forEach(
							superClass -> {
								final var refSchema = new ObjectSchema();
								refSchema.set$ref("#/components/schemas/" + this.getPrefixedSchemaName(superClass));

								context.classSchema.addAllOfItem(refSchema);
							});
		}

		// After checking/processing, if the base class schema has properties but no AllOf list and no
		// type, set its type to "object".
		if (context.classSchema.getProperties() != null
				&& context.classSchema.getAllOf() == null
				&& context.classSchema.getType() == null) {
			MapperProperty.setSchemaType(context.classSchema, "object");
		}
	}

	private void pruneUnusedReferencedClasses() {
		for (final var refClass : new HashSet<OWLClass>(context.referencedClasses)) {
			// Indicator that referenced class has one or more equivalent classes.
			final boolean hasEquivalentClasses =
					EntitySearcher.getEquivalentClasses(refClass, context.baseClassOntology).count() > 0;

			// Indicator that referenced class has (subclass) properties.
			boolean hasSubClassProperties = false;

			// Indicator that referenced class is the range class for one or more object properties.
			boolean isDomainForDataProperty = false;

			// Indicator that referenced class is the range class for one or more object properties.
			boolean isRangeForObjectProperty = false;

			// Look for all subclass axioms where the expression type is not an OWL CLass (i.e. it is a
			// restriction).  This means there is an axiom indicating a "subclass of" property
			// restriction is declared for the class.
			for (final var ax : context.baseClassOntology.getSubClassAxiomsForSubClass(refClass)) {
				for (final var nce : ax.getNestedClassExpressions()) {

					if (!nce.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS)) {
						hasSubClassProperties = true;
					}
				}
			}

			// Look at all data properties.  If any of them contain a domain which is the current
			// referenced class, then set flag to keep the reference class to true.
			for (final var op : context.baseClassOntology.getDataPropertiesInSignature()) {
				for (final var dataPropDomainAx :
						context.baseClassOntology.getDataPropertyDomainAxioms(op)) {
					if (dataPropDomainAx.getClassesInSignature().contains(refClass)) {
						isDomainForDataProperty = true;
					}
				}
			}

			final Map<String, Schema> classProperties =
					context.classSchema == null ? null : context.classSchema.getProperties();

			// Look at all object properties for the base class.  If any of them contain a range which is
			// the current referenced class, then set flag to keep the reference class to true.
			for (final var op : context.baseClassOntology.getObjectPropertiesInSignature()) {
				if (classProperties != null && classProperties.containsKey(op.getIRI().getShortForm())) {
					for (final var objPropRangeAx :
							context.baseClassOntology.getObjectPropertyRangeAxioms(
									op.asObjectPropertyExpression())) {
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
				context.referencedClasses.remove(refClass);

				// Also remove from the AllOf list of schemas, if applicable.
				if (context.classSchema.getEnum() == null) {
					if (context.classSchema.getAllOf() != null) {
						for (final var allOfSchema : new ArrayList<Schema>(context.classSchema.getAllOf())) {
							if (allOfSchema.get$ref() != null
									&& allOfSchema
											.get$ref()
											.contains("#/components/schemas/" + this.getPrefixedSchemaName(refClass))) {
								context.classSchema.getAllOf().remove(allOfSchema);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Get all the classes referenced directly or indirectly (potentially through inheritance) by the
	 * base class.
	 *
	 * @return a {@link Set} of {@link OWLClass}
	 */
	public Set<OWLClass> getAllReferencedClasses() {
		return context.referencedClasses;
	}

	/**
	 * Get all the classes referenced directly or indirectly (potentially through inheritance) by the
	 * base class.
	 *
	 * @return a {@link Map} containing annotation mappings to mappings of property name and
	 *     annotation value.
	 */
	public Map<String, Map<String, String>> getMarkdownMappings() {
		return context.markdownGenerationMap;
	}

	/**
	 * Wrapper for the {@link #visit(OWLClass)} method which also takes the visited class's ontology
	 * as an argument.
	 *
	 * @param visitedClassOntology an {@link OWLOntology} which contains the visited class.
	 * @param ce an {@link OWLClass} to be visited by this {@link ObjectVisitor} class.
	 */
	public void visit(@Nonnull OWLOntology visitedClassOntology, @Nonnull OWLClass ce) {
		context.baseClassOntology = visitedClassOntology;
		ce.accept(this);
	}

	/**
	 * @param ce an {@link OWLClass} to be visited by this {@link ObjectVisitor} class.
	 */
	@Override
	public void visit(@Nonnull OWLClass ce) {
		// If the base class is null when this OWLClass is visited, then treat it as the base class and
		// set up this Visitor class with its basic details.
		if (context.baseClass == null) {
			this.initializeBaseClass(ce);

			// There is a possibility that the owl:thing for the ontology contains a universal property.
			// While generally rare, this might be some type of identifier, such as a GUID.
			if (context.owlThing != null) {
				context.owlThing.accept(this);
			}
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!ce.equals(context.owlThing) && !context.processedClasses.contains(ce)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			context.processedClasses.add(ce);

			// If using inheritance references, make sure to add super classes.
			if (GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)
					&& GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)) {
				// Add the base and super classes to the referenced class set.
				context.referencedClasses.add(ce);
			}

			// Loop through the ontologies to use the one relevant for the current OWLClass.
			// for (OWLOntology ontology : this.ontologies) {
			// Only traverse this OWLClass's super classes, if it is contained in the ontology.
			if (context.baseClassOntology.containsClassInSignature(ce.getIRI())) {
				// If it has subclass axioms, then loop through each to accept visits for all super
				// classes.
				context
						.baseClassOntology
						.subClassAxiomsForSubClass(ce)
						.forEach(
								ax -> {
									// Only traverse super classes for inheriting properties.  Restrictions handled
									// via generatePropertySchemasWithRestrictions() below.
									if (ax.getSuperClass().isOWLClass()) {
										ax.getSuperClass().accept(this);
									}
								});
			}
		}

		// Only include properties from the base class OR we are not following references (because all
		// properties need to be copied to the base class in this case).
		// Inherited details should be determined via references.
		if (context.baseClass.equals(ce)) {
			// Get all non-inherited object and data properties.
			context.basePropertiesMap.putAll(this.getObjectPropertySchemasForClass(ce));
			context.basePropertiesMap.putAll(this.getDataPropertySchemasForClass(ce));

			// Not using setProperties(), because it creates immutability which breaks unit tests.
			context.basePropertiesMap.forEach(context::addPropertyToSchema);

			// Generate restrictions for all properties of this class, regardless of following references
			// or not.
			this.generatePropertySchemasWithRestrictions(ce);
		} else {
			// If this is a superclass AND we are using inheritance/superclass references, still add the
			// properties to the properties map.
			if (!GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)) {
				context.basePropertiesMap.putAll(this.getObjectPropertySchemasForClass(ce));
				context.basePropertiesMap.putAll(this.getDataPropertySchemasForClass(ce));

				// Not using setProperties(), because it creates immutability which breaks unit tests.
				context.basePropertiesMap.forEach(context::addPropertyToSchema);

				this.generatePropertySchemasWithRestrictions(ce);
			}
		}
	}

	/**
	 * Update the base class's {@link Schema} properties with restrictions from the ontology.
	 *
	 * @param owlClass an {@link OWLClass} which may either be the base class or one of its super
	 *     classes.
	 */
	private void generatePropertySchemasWithRestrictions(OWLClass owlClass) {
		if (owlClass != null && owlClass.equals(context.baseClass)) {
			logger.info("--------------------------------------------------");
			logger.info("\tGenerating restrictions for:");
			logger.info("\t\t" + context.baseClass);
			logger.info("--------------------------------------------------");
		} else {
			logger.info("--------------------------------------------------");
			logger.info("\tGenerating restrictions for:");
			logger.info("\t\t" + context.baseClass);
			logger.info("\twhich were inherited from:");
			logger.info("\t\t" + owlClass);
			logger.info("--------------------------------------------------");
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!context.processedRestrictionClasses.contains(owlClass)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			context.processedRestrictionClasses.add(owlClass);

			// Search the ontology for this OWLClass.
			// If it has subclass axioms, then loop through each to generate schema restrictions.
			context
					.baseClassOntology
					.subClassAxiomsForSubClass(owlClass)
					.forEach(
							ax -> {
								// A flag to determine whether we should skip visiting the axiom's super class.
								boolean shouldSkipVisits = false;

								// Well-formed axioms should not be an OWLClass type.
								if (!(ax instanceof OWLClass)) {
									if (ax.getSuperClass() instanceof OWLRestriction) {
										final var property =
												ax.getSuperClass() instanceof OWLObjectRestriction
														? ((OWLObjectRestriction) ax.getSuperClass())
																.getProperty()
																.asOWLObjectProperty()
														: ((OWLDataRestriction) ax.getSuperClass())
																.getProperty()
																.asOWLDataProperty();
										context.currentlyProcessedPropertyName = property.getIRI().getShortForm();

										// Add any classes referenced by the restriction.
										context.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());
									} else if (ax.getSuperClass() instanceof OWLBooleanClassExpression) {
										if (ax.getSuperClass() instanceof OWLObjectComplementOf) {

											// Add the object complement reference class.
											context.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());

											logger.info(
													"\t"
															+ this.getBaseClassName()
															+ " has an object complement of axiom.  This is not for a property,"
															+ " so do not set property name.");
											logger.info("\t\taxiom:  " + ax);
										} else {
											logger.severe("\t" + this.getBaseClassName() + " has unknown restriction.");
											logger.severe("\t\taxiom:  " + ax);
											shouldSkipVisits = true;
										}
									} else if (ax.getSuperClass() instanceof OWLObjectOneOf) {
										logger.info(
												"\t"
														+ this.getBaseClassName()
														+ " is an ObjectOneOf set containing one or more Individuals.  Not"
														+ " setting property name, to treat it like an enum.");
										logger.info("\t\taxiom:  " + ax);
									} else {
										logger.info(
												"\t"
														+ this.getBaseClassName()
														+ " is a subclass of "
														+ this.getPrefixedSchemaName(ax.getSuperClass().asOWLClass())
														+ ".  No restrictions to process.");
										logger.info("\t\taxiom:  " + ax);
										shouldSkipVisits = true;
									}

									if (!shouldSkipVisits) {
										// Proceed with the visit.
										ax.getSuperClass().accept(this);

										// There are cases where property description/annotations have not been
										// processed yet.
										// Check each entity here to be safe.
										ax.getSuperClass()
												.objectPropertiesInSignature()
												.forEach(this::setDescriptionReadOnlyWriteOnlyFromAnnotations);
										ax.getSuperClass()
												.dataPropertiesInSignature()
												.forEach(this::setDescriptionReadOnlyWriteOnlyFromAnnotations);

										// Also check the subClass axioms for annotations specifying read/write only.
										this.setReadOnlyWriteOnlyFromAxiomAnnotations(ax);
									}

									// Clear out the property name.
									context.currentlyProcessedPropertyName = null;
								} else {
									logger.severe("\t" + this.getBaseClassName() + " has unknown restriction.");
									logger.severe("\t\taxiom:  " + ax);
								}
							});

			// For equivalent (to) classes (e.g. Defined classes) we need to accept the visit to
			// navigate it.
			context
					.baseClassOntology
					.equivalentClassesAxioms(owlClass)
					.forEach(
							(eqClsAx) -> {
								eqClsAx.accept(this);
							});
		}
	}

	/**
	 * For an OWLEntity (that is an object/data property), check its description and annotations. If
	 * it has either, update the property schema with description and/or write/read only annotation
	 * flags.
	 *
	 * @param entity an {@link OWLEntity} which should be an {@link OWLDataProperty} or {@link
	 *     OWLObjectProperty}
	 */
	private void setDescriptionReadOnlyWriteOnlyFromAnnotations(OWLEntity entity) {
		if (entity.isOWLDataProperty() || entity.isOWLObjectProperty()) {
			EntitySearcher.getAnnotations(entity, context.baseClassOntology)
					.forEach(
							annotation -> {
								var propertySchema =
										context.classSchema.getProperties() == null
												? null
												: (Schema)
														context.classSchema.getProperties().get(entity.getIRI().getShortForm());

								if (propertySchema != null) {
									if (propertySchema.getDescription() == null
											|| propertySchema.getDescription().isBlank()) {
										final var propertyDescription =
												OntologyDescriptionUtils.getDescription(
																entity,
																context.baseClassOntology,
																GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
														.orElse(null);
										MapperProperty.setSchemaDescription(propertySchema, propertyDescription);
									}

									final var annotationConfig = context.configData.getAnnotationConfig();
									if (annotationConfig != null) {
										final var annotationPropertyName =
												annotation.getProperty().getIRI().getShortForm();

										final var propertyAnnotations = annotationConfig.getPropertyAnnotations();

										if (propertyAnnotations != null) {
											// If property contains the annotation property (name is specified in
											// configuration
											// file) indicating it is read-only, then set value on the schema.
											final var readOnlyAnnotation = propertyAnnotations.getReadOnlyFlagName();
											if (readOnlyAnnotation != null
													&& !readOnlyAnnotation.isBlank()
													&& readOnlyAnnotation.equals(annotationPropertyName)) {
												MapperProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
											}

											// If property contains the annotation property (name is specified in
											// configuration
											// file) indicating it is write-only, then set value on the schema.
											final var writeOnlyAnnotation = propertyAnnotations.getWriteOnlyFlagName();
											if (writeOnlyAnnotation != null
													&& !writeOnlyAnnotation.isBlank()
													&& writeOnlyAnnotation.equals(annotationPropertyName)) {
												MapperProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
											}

											// If property contains the annotation property (name is specified in
											// configuration file) indicating what the example value is for a data
											// property,
											// then set value on the schema.
											final var exampleValueAnnotation = propertyAnnotations.getExampleValueName();
											if (entity.isOWLDataProperty()
													&& exampleValueAnnotation != null
													&& !exampleValueAnnotation.isBlank()
													&& exampleValueAnnotation.equals(annotationPropertyName)) {
												MapperDataProperty.setExampleValueForPropertySchema(
														propertySchema, annotation);
											}
										}
									}
								}
							});
		}
	}

	/**
	 * For an OWLAxiom, check its annotations. If has a read/write one then update the property schema
	 * with write/read only annotation flags.
	 *
	 * @param axiom an {@link OWLAxiom}
	 */
	private void setReadOnlyWriteOnlyFromAxiomAnnotations(OWLAxiom axiom) {
		axiom
				.annotations()
				.forEach(
						annotation -> {
							var propertySchema =
									context.classSchema.getProperties() == null
											? null
											: (Schema)
													context
															.classSchema
															.getProperties()
															.get(context.currentlyProcessedPropertyName);

							if (propertySchema != null) {
								final var annotationConfig = context.configData.getAnnotationConfig();
								if (annotationConfig != null) {
									final var annotationPropertyName =
											annotation.getProperty().getIRI().getShortForm();

									final var propertyAnnotations = annotationConfig.getPropertyAnnotations();

									if (propertyAnnotations != null) {

										// If property contains the annotation property (name is specified in
										// configuration
										// file) indicating it is read-only, then set value on the schema.
										final var readOnlyAnnotation = propertyAnnotations.getReadOnlyFlagName();
										if (readOnlyAnnotation != null
												&& !readOnlyAnnotation.isBlank()
												&& readOnlyAnnotation.equals(annotationPropertyName)) {
											MapperProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
										}

										// If property contains the annotation property (name is specified in
										// configuration
										// file) indicating it is write-only, then set value on the schema.
										final var writeOnlyAnnotation = propertyAnnotations.getWriteOnlyFlagName();
										if (writeOnlyAnnotation != null
												&& !writeOnlyAnnotation.isBlank()
												&& writeOnlyAnnotation.equals(annotationPropertyName)) {
											MapperProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
										}

										// If property contains the annotation property (name is specified in
										// configuration file) indicating what the example value is for a data property,
										// then set value on the schema.
										final var exampleValueAnnotation = propertyAnnotations.getExampleValueName();
										if (exampleValueAnnotation != null
												&& !exampleValueAnnotation.isBlank()
												&& exampleValueAnnotation.equals(annotationPropertyName)) {
											MapperDataProperty.setExampleValueForPropertySchema(
													propertySchema, annotation);
										}
									}
								}
							}
						});
	}

	/**
	 * Add annotation value to map for tracking markdown content.
	 *
	 * @param annotation an {@link OWLAnnotation} with the name of the markdown annotation and its
	 *     value.
	 * @param entity an {@link OWLEntity} with the class/property associated with the annotation.
	 */
	private void addMarkdownAnnotationsToMap(OWLAnnotation annotation, String annotationMappedTo) {
		final var annotationConfig = context.configData.getAnnotationConfig();
		if (annotationConfig != null) {
			final var annotationPropertyName = annotation.getProperty().getIRI().getShortForm();

			final var markdownGenAnnotations = annotationConfig.getMarkdownGenerationAnnotations();
			if (markdownGenAnnotations != null) {
				final var markdownGenAnnotationSet = markdownGenAnnotations;
				if (markdownGenAnnotationSet != null && !markdownGenAnnotationSet.isEmpty()) {
					for (final var entry : markdownGenAnnotationSet) {
						final var markdownAnnotationName = entry.getAnnotationName();

						if (annotationPropertyName.equals(markdownAnnotationName)) {
							final var markdownAnnotationLiteralValue = annotation.getValue().literalValue();
							final var markdownAnnotationValue =
									markdownAnnotationLiteralValue.isPresent()
											? markdownAnnotationLiteralValue.get().getLiteral()
											: "";

							var propMarkdownValueMap = context.markdownGenerationMap.get(markdownAnnotationName);
							if (propMarkdownValueMap == null) {
								propMarkdownValueMap = new TreeMap<>();
								context.markdownGenerationMap.put(markdownAnnotationName, propMarkdownValueMap);
							}

							propMarkdownValueMap.put(annotationMappedTo, markdownAnnotationValue);
						}
					}
				}
			}
		}
	}

	/**
	 * Add markdown content from annotations (which are defined in the configuration file) for all
	 * classes and object/data properties.
	 *
	 * @param axiom an {@link OWLAxiom}
	 */
	private void setMarkdownContentFromAxiomAnnotations() {
		for (final var refClass : context.referencedClasses) {
			final var refClassName = refClass.getIRI().getShortForm();

			// Get markdown annotations from classes.
			EntitySearcher.getAnnotationObjects(refClass, context.baseClassOntology)
					.forEach(
							(annotation) -> {
								this.addMarkdownAnnotationsToMap(annotation, refClassName);
							});

			// Get markdown annotations from data properties.
			context
					.baseClassOntology
					.axioms(AxiomType.DATA_PROPERTY_DOMAIN)
					.filter(
							dataPropDomainAx ->
									dataPropDomainAx.getDomain().getClassesInSignature().contains(refClass))
					.forEach(
							(dataPropDomainAx) -> {
								dataPropDomainAx
										.dataPropertiesInSignature()
										.forEach(
												(dataProp) -> {
													EntitySearcher.getAnnotationObjects(dataProp, context.baseClassOntology)
															.forEach(
																	(annotation) -> {
																		this.addMarkdownAnnotationsToMap(
																				annotation,
																				refClassName + "#" + dataProp.getIRI().getShortForm());
																	});
												});
							});

			// Get markdown annotations from object properties.
			context
					.baseClassOntology
					.axioms(AxiomType.OBJECT_PROPERTY_DOMAIN)
					.filter(
							objPropDomainAx ->
									objPropDomainAx.getDomain().getClassesInSignature().contains(refClass))
					.forEach(
							(objPropDomainAx) -> {
								objPropDomainAx
										.objectPropertiesInSignature()
										.forEach(
												(objProp) -> {
													EntitySearcher.getAnnotationObjects(objProp, context.baseClassOntology)
															.forEach(
																	(annotation) -> {
																		this.addMarkdownAnnotationsToMap(
																				annotation,
																				refClassName + "#" + objProp.getIRI().getShortForm());
																	});
												});
							});

			// Get annotations from subclass axioms/restrictions.
			context
					.baseClassOntology
					.subClassAxiomsForSubClass(refClass)
					.forEach(
							(axiom) -> {
								axiom
										.annotations()
										.forEach(
												(annotation) -> {
													axiom
															.components()
															.filter(component -> component instanceof OWLRestriction)
															.forEach(
																	(component) -> {
																		if (component instanceof OWLObjectRestriction) {
																			this.addMarkdownAnnotationsToMap(
																					annotation,
																					refClassName
																							+ "#"
																							+ ((OWLObjectRestriction) component)
																									.getProperty()
																									.asOWLObjectProperty()
																									.getIRI()
																									.getShortForm());
																		} else if (component instanceof OWLDataRestriction) {
																			this.addMarkdownAnnotationsToMap(
																					annotation,
																					refClassName
																							+ "#"
																							+ ((OWLDataRestriction) component)
																									.getProperty()
																									.asOWLDataProperty()
																									.getIRI()
																									.getShortForm());
																		} else {
																			logger.info(
																					"===  Failed while attempting to add markdown annotations"
																							+ " for:  "
																							+ component);
																		}
																	});
												});

								axiom
										.dataPropertiesInSignature()
										.forEach(
												(dataProp) -> {
													EntitySearcher.getAnnotationObjects(dataProp, context.baseClassOntology)
															.forEach(
																	(annotation) -> {
																		this.addMarkdownAnnotationsToMap(
																				annotation,
																				refClassName + "#" + dataProp.getIRI().getShortForm());
																	});
												});

								axiom
										.objectPropertiesInSignature()
										.forEach(
												(objProp) -> {
													EntitySearcher.getAnnotationObjects(objProp, context.baseClassOntology)
															.forEach(
																	(annotation) -> {
																		this.addMarkdownAnnotationsToMap(
																				annotation,
																				refClassName + "#" + objProp.getIRI().getShortForm());
																	});
												});
							});
		}
	}

	/**
	 * Convenience method for getting the base class's short form name (i.e. only its name, not its
	 * full IRI).
	 *
	 * @return a {@link String} which is the (short form) name of the base class.
	 */
	private String getBaseClassName() {
		return context.baseClass.getIRI().getShortForm();
	}

	/**
	 * Get a {@link Schema} for the object property (expression). The ranges are also passed because
	 * it may be inheriting from a super-property (and therefore not obvious from the object property
	 * expression itself).
	 *
	 * @param objPropExpr an {@link OWLObjectPropertyExpression}
	 * @param objPropRanges a {@link Set} of {@link OWLClass} representing the object property's
	 *     range(s), which may include inherited ranges not obvious from the {@link
	 *     OWLObjectPropertyExpression} itself.
	 * @return a {@link Schema} of the object property (expression).
	 */
	private Schema getObjectPropertySchema(
			OWLObjectPropertyExpression objPropExpr, @Nullable Set<OWLClass> objPropRanges) {
		final var op = objPropExpr.asOWLObjectProperty();
		final var propertyName = op.getIRI().getShortForm();
		context.propertyNames.add(propertyName);

		final var returnObjPropertySchema = new Schema();

		context.withProcessedProperty(
				propertyName,
				() -> {
					logger.info("\thas property:\t\"" + propertyName + "\"");

					final var propertyRanges = new HashSet<String>();
					final var complexObjectRanges = new HashSet<OWLClassExpression>();

					// Add object property OWLClass ranges to set of property ranges.
					if (objPropRanges != null) {
						objPropRanges.forEach(
								objPropRange -> {
									propertyRanges.add(this.getPrefixedSchemaName(objPropRange.asOWLClass()));

									// Add the range to the referenced class set.
									context.referencedClasses.add(objPropRange.asOWLClass());

									if (EntitySearcher.getEquivalentClasses(
															objPropRange.asOWLClass(), context.baseClassOntology)
													.count()
											> 0) {
										// Add the object property name and NOT the class name that it refers to.
										context.enumProperties.add(propertyName);
									}
								});
					}

					// Also loop through range axioms for object property expression and add ranges to map,
					// complex
					// map, or visit the range, if unionOf/intersectionOf/oneOf.
					context
							.baseClassOntology
							.objectPropertyRangeAxioms(objPropExpr)
							.forEach(
									(objPropRangeAxiom) -> {
										if (objPropRangeAxiom.getRange() instanceof OWLClass) {
											propertyRanges.add(
													this.getPrefixedSchemaName(objPropRangeAxiom.getRange().asOWLClass()));

											if (EntitySearcher.getEquivalentClasses(
																	objPropRangeAxiom.getRange().asOWLClass(),
																	context.baseClassOntology)
															.count()
													> 0) {
												// Add the object property name and NOT the class name that it refers to.
												context.enumProperties.add(propertyName);
											}

											// Add the range to the referenced class set.
											context.referencedClasses.add(objPropRangeAxiom.getRange().asOWLClass());
										} else if (objPropRangeAxiom.getRange() instanceof OWLObjectUnionOf
												|| objPropRangeAxiom.getRange() instanceof OWLObjectIntersectionOf
												|| objPropRangeAxiom.getRange() instanceof OWLObjectOneOf
												|| objPropRangeAxiom.getRange() instanceof OWLObjectComplementOf) {
											logger.info(
													"\t\t...has complex range -> proceeding to its restrictions"
															+ " immediately...");
											objPropRangeAxiom.getRange().accept(this);
										} else {
											complexObjectRanges.add(objPropRangeAxiom.getRange());
										}
									});

					// Check the ranges.  Output relevant info.  May not be necessary.
					if (propertyRanges.isEmpty()) {
						logger.warning("\t\tProperty \"" + op.getIRI() + "\" has range equals zero.");
					} else {
						logger.info("\t\tProperty range(s): " + propertyRanges);
					}
					logger.info("");

					// In cases, such as unionOf/intersectionOf/oneOf , the property schema may already be
					// set.  Get
					// it, if so.
					var objPropertySchema =
							context.classSchema.getProperties() == null
									? null
									: (Schema) context.classSchema.getProperties().get(propertyName);

					try {
						final var propertyDescription =
								OntologyDescriptionUtils.getDescription(
												op,
												context.baseClassOntology,
												GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
										.orElse(null);

						// Workaround for handling unionOf/intersectionOf/oneOf cases which may be set already
						// above.
						if (objPropertySchema == null) {
							// Get object property schema from mapper.
							objPropertySchema =
									MapperObjectProperty.createObjectPropertySchema(
											propertyName, propertyDescription, propertyRanges);
						} else {
							// These do not get set properly because the unionOf/intersectionOf/oneOf property
							// schema
							// was not created via MapperDataProperty.createDataPropertySchema().
							MapperObjectProperty.setSchemaName(objPropertySchema, propertyName);
							MapperObjectProperty.setSchemaDescription(objPropertySchema, propertyDescription);
						}

						// If property is functional, set the schema accordingly.
						if (EntitySearcher.isFunctional(
								op, Collections.singleton(context.baseClassOntology).stream())) {
							logger.info("\t\tProperty is functional.  Therefore, required with a max of 1 item.");
							context.functionalProperties.add(propertyName);
							context.requiredProperties.add(propertyName);
							MapperObjectProperty.setFunctionalForPropertySchema(objPropertySchema);
						}

						final var annotationConfig = context.configData.getAnnotationConfig();
						if (annotationConfig != null) {
							final var propertyAnnotations = annotationConfig.getPropertyAnnotations();

							if (propertyAnnotations != null) {
								// If property contains the annotation property (name is specified in configuration
								// file)
								// indicating it is read-only, then set value on the schema.
								final var readOnlyAnnotation = propertyAnnotations.getReadOnlyFlagName();
								if (readOnlyAnnotation != null && !readOnlyAnnotation.isBlank()) {
									if (EntitySearcher.getAnnotations(op, context.baseClassOntology)
													.filter(
															annotation ->
																	readOnlyAnnotation.equals(
																			annotation.getProperty().getIRI().getShortForm()))
													.count()
											> 0) {
										MapperObjectProperty.setReadOnlyValueForPropertySchema(objPropertySchema, true);
									}
								}

								// If property contains the annotation property (name is specified in configuration
								// file)
								// indicating it is write-only, then set value on the schema.
								final var writeOnlyAnnotation = propertyAnnotations.getWriteOnlyFlagName();
								if (writeOnlyAnnotation != null && !writeOnlyAnnotation.isBlank()) {
									if (EntitySearcher.getAnnotations(op, context.baseClassOntology)
													.filter(
															annotation ->
																	writeOnlyAnnotation.equals(
																			annotation.getProperty().getIRI().getShortForm()))
													.count()
											> 0) {
										MapperObjectProperty.setWriteOnlyValueForPropertySchema(
												objPropertySchema, true);
									}
								}
							}
						}

						// For any complex property ranges, traverse.  This will grab restrictions also.  There
						// is no
						// good way for this situation to grab only the types in this situation.
						if (!complexObjectRanges.isEmpty()) {
							complexObjectRanges.forEach(
									(objectRange) -> {
										objectRange.accept(this);
									});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					SchemaCloneUtils.clone(objPropertySchema, returnObjPropertySchema);
				});

		return returnObjPropertySchema;
	}

	/**
	 * Read the Ontology, obtain the ObjectProperties, obtain the range for each property and generate
	 * the SchemaProperty.
	 *
	 * @param owlClass the {@link OWLClass} to get all object property {@link Schema}s for.
	 * @return a {@link Map} of the object property name keys and their associated {@link Schema}s.
	 */
	private Map<String, Schema> getObjectPropertySchemasForClass(OWLClass owlClass) {
		// Object property map to return
		final var objPropertiesMap = new HashMap<String, Schema>();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Loop through all top-level object properties of this class's ontology.
		context
				.reasoner
				.subObjectProperties(
						context.reasoner.getTopObjectPropertyNode().getRepresentativeElement(),
						InferenceDepth.DIRECT)
				.filter(objPropExpr -> !objPropExpr.isBottomEntity() && objPropExpr.isOWLObjectProperty())
				.forEach(
						(objPropExpr) -> {
							var isOwlClassDomainOfObjProp = false;

							// Check property contains this owlClass as a domain.
							for (final var objPropDomainAx :
									context.baseClassOntology.getObjectPropertyDomainAxioms(objPropExpr)) {
								final var domain = objPropDomainAx.getDomain();
								if (domain.isOWLClass()) {
									if (owlClass.equals(domain)) {
										isOwlClassDomainOfObjProp = true;
									}
								} else if (domain instanceof OWLObjectUnionOf) {
									for (final var domainOperand : ((OWLObjectUnionOf) domain).getOperands()) {
										if (owlClass.equals(domainOperand)) {
											isOwlClassDomainOfObjProp = true;
										}
									}
								} else {
									logger.severe(
											"\t  The object property domain axiom \""
													+ objPropDomainAx
													+ "\" has an unknown domain type \""
													+ domain
													+ "\" !!");
								}
							}

							final var objPropRanges = new HashSet<OWLClass>();

							// Keep track of all property ranges.  Even if the super-property has no domain, the
							// ranges can be inherited by sub-properties which have this owlClass as a domain.
							for (final var objPropRangeAx :
									context.baseClassOntology.getObjectPropertyRangeAxioms(objPropExpr)) {
								final var range = objPropRangeAx.getRange();

								// Only handle ranges which are OWLClass objects
								if (range.isOWLClass()) {
									objPropRanges.add(range.asOWLClass());
								} else if (range instanceof OWLObjectUnionOf
										|| range instanceof OWLObjectIntersectionOf) {
									// This will be ignored temporarily, until the object property schema is created
								} else {
									logger.severe(
											"\t  The object property range axiom \""
													+ objPropRangeAx
													+ "\" has an unknown range type \""
													+ range
													+ "\" !!");
								}
							}

							// If this (sub-)property (under owl:topObjectProperty) has a domain of the current
							// owlClass, then get its schema.
							if (isOwlClassDomainOfObjProp) {
								final var propertyName = objPropExpr.asOWLObjectProperty().getIRI().getShortForm();

								// Save object property schema to class's schema.
								objPropertiesMap.put(
										propertyName, this.getObjectPropertySchema(objPropExpr, objPropRanges));

								context.withProcessedProperty(
										propertyName,
										() -> {
											for (final var objPropRangeAx :
													context.baseClassOntology.getObjectPropertyRangeAxioms(objPropExpr)) {
												final var range = objPropRangeAx.getRange();

												// For complex ranges which are unions or intersections, treat like a
												// restriction.
												if (range instanceof OWLObjectUnionOf
														|| range instanceof OWLObjectIntersectionOf) {
													range.accept(this);
												}
											}
										});
							}

							// ==========================================================================================
							// Junky workaround to use variable within the stream() below.
							final var isOwlClassDomainOfObjPropFinal = isOwlClassDomainOfObjProp;

							// Loop through all sub-properties of this property.
							context
									.reasoner
									.subObjectProperties(objPropExpr, InferenceDepth.ALL)
									.filter(
											subObjPropExpr ->
													!subObjPropExpr.isBottomEntity() && subObjPropExpr.isOWLObjectProperty())
									.forEach(
											(subObjPropExpr) -> {
												var isOwlClassDomainOfSubObjProp = false;

												// Check sub-property's domain(s) and inherit from its super-property.
												// Check property contains this owlClass as a domain.
												for (final var subObjPropDomainAx :
														context.baseClassOntology.getObjectPropertyDomainAxioms(
																subObjPropExpr)) {
													final var domain = subObjPropDomainAx.getDomain();
													if (domain.isOWLClass()) {
														if (owlClass.equals(domain)) {
															isOwlClassDomainOfSubObjProp = true;
														}
													} else if (domain instanceof OWLObjectUnionOf) {
														for (final var domainOperand :
																((OWLObjectUnionOf) domain).getOperands()) {
															if (owlClass.equals(domainOperand)) {
																isOwlClassDomainOfSubObjProp = true;
															}
														}
													} else {
														logger.severe(
																"\t  The object property domain axiom \""
																		+ subObjPropDomainAx
																		+ "\" has an unknown domain type \""
																		+ domain
																		+ "\" !!");
													}
												}

												final var subObjPropRanges = new HashSet<OWLClass>();
												subObjPropRanges.addAll(objPropRanges);

												// Check sub-property's range(s) and inherit from its super-property.
												for (final var subObjPropRangeAx :
														context.baseClassOntology.getObjectPropertyRangeAxioms(
																subObjPropExpr)) {
													final var range = subObjPropRangeAx.getRange();
													if (range.isOWLClass()) {
														subObjPropRanges.add(range.asOWLClass());
													} else if (range instanceof OWLObjectUnionOf) {
														for (final var rangeOperand :
																((OWLObjectUnionOf) range).getOperands()) {
															subObjPropRanges.add(rangeOperand.asOWLClass());
														}
													} else if (range instanceof OWLObjectUnionOf
															|| range instanceof OWLObjectIntersectionOf) {
														// This will be ignored temporarily, until the object property schema is
														// created
													} else {
														logger.severe(
																"\t  The object property range axiom \""
																		+ subObjPropRangeAx
																		+ "\" has an unknown range type \""
																		+ range
																		+ "\" !!");
													}
												}

												// If this (sub-)property (under owl:topObjectProperty) has a domain of the
												// current owlClass, then get its schema.
												if (isOwlClassDomainOfSubObjProp || isOwlClassDomainOfObjPropFinal) {
													final var propertyName =
															subObjPropExpr.asOWLObjectProperty().getIRI().getShortForm();

													// Save object property schema to class's schema.
													objPropertiesMap.put(
															propertyName,
															this.getObjectPropertySchema(subObjPropExpr, subObjPropRanges));

													context.withProcessedProperty(
															propertyName,
															() -> {
																for (final var subObjPropRangeAx :
																		context.baseClassOntology.getObjectPropertyRangeAxioms(
																				subObjPropExpr)) {
																	final var range = subObjPropRangeAx.getRange();

																	// For complex ranges which are unions or intersections, treat
																	// like a
																	// restriction.
																	if (range instanceof OWLObjectUnionOf
																			|| range instanceof OWLObjectIntersectionOf) {
																		range.accept(this);
																	}
																}
															});
												}
											});
							// ==========================================================================================
						});
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		return objPropertiesMap;
	}

	/**
	 * Read the Ontology, obtain the DataProperties, obtain the range for each property and generate
	 * the SchemaProperty.
	 *
	 * @param owlClass the {@link OWLClass} to get all data property {@link Schema}s for.
	 * @return a {@link Map} of the data property name keys and their associated {@link Schema}s.
	 */
	private Map<String, Schema> getDataPropertySchemasForClass(OWLClass owlClass) {
		final var dataPropDomainAxioms = new HashSet<OWLDataPropertyDomainAxiom>();
		dataPropDomainAxioms.addAll(
				context.baseClassOntology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));

		// Data property map to return
		final var dataPropertiesMap = new HashMap<String, Schema>();

		// For the class's properties, check each axiom where the axiom's domain is a class AND the
		// current class equals the domain.
		dataPropDomainAxioms.stream()
				.filter(
						dataPropDomainAx ->
								dataPropDomainAx.getDomain().getClassesInSignature().contains(owlClass))
				.forEach(
						(dataPropDomainAx) -> {
							// Get set of all data properties and subproperties.
							final var dataProperties = dataPropDomainAx.getDataPropertiesInSignature();
							for (final var topLevelDataProperty : dataProperties) {
								for (final var dataPropEx :
										context
												.reasoner
												.getSubDataProperties(topLevelDataProperty, false)
												.getFlattened()) {
									// owl:bottomDataProperty
									if (!dataPropEx.isOWLBottomDataProperty()) {
										dataProperties.add(dataPropEx.asOWLDataProperty());
									}
								}
							}

							// Loop through each data (sub)property and generate its schema
							for (final var dp : dataProperties) {
								final var propertyName = dp.getIRI().getShortForm();
								context.propertyNames.add(propertyName);
								context.withProcessedProperty(
										propertyName,
										() -> {
											logger.info("\thas property:  \"" + propertyName + "\"");

											final var propertyRanges = new HashSet<String>();
											final var complexDataRanges = new HashSet<OWLDataRange>();
											context
													.baseClassOntology
													.dataPropertyRangeAxioms(dp)
													.forEach(
															(dataPropRangeAxiom) -> {
																if (dataPropRangeAxiom.getRange() instanceof OWLDatatype) {
																	propertyRanges.add(
																			((OWLDatatype) dataPropRangeAxiom.getRange())
																					.getIRI()
																					.getShortForm());
																} else if (dataPropRangeAxiom.getRange() instanceof OWLDataUnionOf
																		|| dataPropRangeAxiom.getRange()
																				instanceof OWLDataIntersectionOf
																		|| dataPropRangeAxiom.getRange() instanceof OWLDataOneOf
																		|| dataPropRangeAxiom.getRange()
																				instanceof OWLDataComplementOf) {
																	logger.info(
																			"\t\t...has complex range -> proceeding to its restrictions"
																					+ " immediately...");
																	dataPropRangeAxiom.getRange().accept(this);
																} else {
																	complexDataRanges.add(dataPropRangeAxiom.getRange());
																}
															});

											// Check the ranges.  Output relevant info.  May not be necessary.
											if (propertyRanges.isEmpty()) {
												logger.warning(
														"\t\tProperty \"" + dp.getIRI() + "\" has range equals zero.");
											} else {
												logger.info("\t\tProperty range(s): " + propertyRanges);

												try {
													final var propertyDescription =
															OntologyDescriptionUtils.getDescription(
																			dp,
																			context.baseClassOntology,
																			GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
																	.orElse(null);

													// In cases, such as unionOf/intersectionOf/oneOf , the property schema
													// may
													// already be set.  Get it, if so.
													var dataPropertySchema =
															context.classSchema.getProperties() == null
																	? null
																	: (Schema) context.classSchema.getProperties().get(propertyName);

													// Workaround for handling unionOf/intersectionOf/oneOf cases which may be
													// set
													// already above.
													if (dataPropertySchema == null) {
														// Get data property schema from mapper.
														dataPropertySchema =
																MapperDataProperty.createDataPropertySchema(
																		propertyName, propertyDescription, propertyRanges);
													} else {
														// These do not get set properly because the
														// unionOf/intersectionOf/oneOf
														// property schema was not created via
														// MapperDataProperty.createDataPropertySchema().
														MapperDataProperty.setSchemaName(dataPropertySchema, propertyName);
														MapperDataProperty.setSchemaDescription(
																dataPropertySchema, propertyDescription);
													}

													// If property is functional, set the schema accordingly.
													if (EntitySearcher.isFunctional(
															dp, Collections.singleton(context.baseClassOntology).stream())) {
														logger.info(
																"\t\tProperty is functional.  Therefore, required with a max of 1"
																		+ " item.");
														context.functionalProperties.add(propertyName);
														context.requiredProperties.add(propertyName);
														MapperDataProperty.setFunctionalForPropertySchema(dataPropertySchema);
													}

													final var annotationConfig = context.configData.getAnnotationConfig();
													if (annotationConfig != null) {
														final var propertyAnnotations =
																annotationConfig.getPropertyAnnotations();

														if (propertyAnnotations != null) {
															// If property contains the annotation property (name is specified in
															// configuration file) indicating it is read-only, then set value on
															// the
															// schema.
															final var readOnlyAnnotation =
																	propertyAnnotations.getReadOnlyFlagName();
															if (readOnlyAnnotation != null && !readOnlyAnnotation.isBlank()) {
																if (EntitySearcher.getAnnotations(dp, context.baseClassOntology)
																				.filter(
																						annotation ->
																								readOnlyAnnotation.equals(
																										annotation
																												.getProperty()
																												.getIRI()
																												.getShortForm()))
																				.count()
																		> 0) {
																	MapperDataProperty.setReadOnlyValueForPropertySchema(
																			dataPropertySchema, true);
																}
															}

															// If property contains the annotation property (name is specified in
															// configuration file) indicating it is write-only, then set value on
															// the
															// schema.
															final var writeOnlyAnnotation =
																	propertyAnnotations.getWriteOnlyFlagName();
															if (writeOnlyAnnotation != null && !writeOnlyAnnotation.isBlank()) {
																if (EntitySearcher.getAnnotations(dp, context.baseClassOntology)
																				.filter(
																						annotation ->
																								writeOnlyAnnotation.equals(
																										annotation
																												.getProperty()
																												.getIRI()
																												.getShortForm()))
																				.count()
																		> 0) {
																	MapperDataProperty.setWriteOnlyValueForPropertySchema(
																			dataPropertySchema, true);
																}
															}

															// If property contains the annotation property (name is specified in
															// configuration file) indicating what the example value is for a data
															// property,
															// then set value on the schema.
															final var exampleValueAnnotation =
																	propertyAnnotations.getExampleValueName();
															if (exampleValueAnnotation != null
																	&& !exampleValueAnnotation.isBlank()) {
																for (final var annotation :
																		EntitySearcher.getAnnotations(dp, context.baseClassOntology)
																				.filter(
																						annotation ->
																								exampleValueAnnotation.equals(
																										annotation
																												.getProperty()
																												.getIRI()
																												.getShortForm()))
																				.collect(Collectors.toSet())) {
																	MapperDataProperty.setExampleValueForPropertySchema(
																			dataPropertySchema, annotation);
																}
															}
														}
													}

													// Save object property schema to class's schema.
													dataPropertiesMap.put(dataPropertySchema.getName(), dataPropertySchema);

													// For any complex property ranges, traverse.  This will grab restrictions
													// also.
													//  There is no good way for this situation to grab only the types in this
													// situation.
													if (!complexDataRanges.isEmpty()) {
														complexDataRanges.forEach(
																(dataRange) -> {
																	dataRange.accept(this);
																});
													}
												} catch (Exception e) {
													e.printStackTrace();
												}
											}

											logger.info("");
										});
							}
						});

		return dataPropertiesMap;
	}

	/**
	 * Gets a new (if one does not already exist) or existing property schema for use in updating
	 * during restriction visits.
	 *
	 * @param propertyName the name of the property to get a {@link Schema} for.
	 * @return a {@link Schema} for the property.
	 */
	private Schema getPropertySchemaForRestrictionVisit(String propertyName) {
		Schema currentPropertySchema =
				context.classSchema.getProperties() == null
						? null
						: (Schema) context.classSchema.getProperties().get(propertyName);

		// In certain cases, a property was not set up with domains/ranges but has a restriction.
		// This property will not exist in the map of property names + schemas yet, so add it and set it
		// up with basic info.
		if (currentPropertySchema == null) {
			currentPropertySchema = new ArraySchema();
			MapperProperty.setSchemaName(currentPropertySchema, propertyName);

			final var propertyDescription =
					GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS)
							? ObaConstants.DEFAULT_DESCRIPTION
							: null;
			MapperProperty.setSchemaDescription(currentPropertySchema, propertyDescription);

			// If this was a new property schema, need to make sure it's added.
			context.classSchema.addProperty(propertyName, currentPropertySchema);
		}

		return currentPropertySchema;
	}

	/** ================== Restrictions traversals ================== */
	@Override
	public void visit(@Nonnull OWLEquivalentClassesAxiom ax) {
		logger.info("\t-- analyzing OWLEquivalentClassesAxiom restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ax);
		logger.info("");

		// If equivalent class axiom AND contains owl:oneOf, then we're looking at an ENUM class.
		ax.classExpressions()
				.filter((e) -> e instanceof OWLObjectOneOf)
				.forEach(
						(oneOfObj) -> {
							var enumValues = ((OWLObjectOneOf) oneOfObj).getOperandsAsList();
							if (enumValues != null && !enumValues.isEmpty()) {
								// Add enum individuals to restriction range
								enumValues.forEach(
										(indv) -> {
											// The getShortForm() method appears to have a bug and removes numbers at the
											// beginning
											// MapperObjectProperty.addEnumValueToObjectSchema(this.classSchema,
											// ((OWLNamedIndividual) indv).getIRI().getShortForm());

											// This is a workaround for the bug.
											// Basically loop through all the prefixes and find/replace the current
											// prefix
											// individual's IRI with nothing.
											// If different, then we've found the individual and know the short form
											// name.
											final var format =
													context
															.baseClassOntology
															.getOWLOntologyManager()
															.getOntologyFormat(context.baseClassOntology);
											if (format.isPrefixOWLDocumentFormat()) {
												final var map =
														format.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
												final var fullIRI = indv.asOWLNamedIndividual().getIRI().toString();
												map.forEach(
														(prefix, iri) -> {
															if (!fullIRI.equals(fullIRI.replaceFirst(iri, ""))) {
																MapperObjectProperty.addEnumValueToObjectSchema(
																		context.classSchema, fullIRI.replaceFirst(iri, ""));
															}
														});
											}
										});
							}
						});

		// Loop through the class expressions in the defined / equivalent-to classes axiom and accept
		// visits from everything else.
		ax.classExpressions()
				.filter((e) -> !context.baseClass.equals(e) && !(e instanceof OWLObjectOneOf))
				.forEach(
						(e) -> {
							// Correctly configured defined (equivalent to) classes (excluding enums, handled
							// above) _should_ be an intersection of
							// one or more classes plus (optional) properties.
							if (e instanceof OWLObjectIntersectionOf) {
								((OWLObjectIntersectionOf) e)
										.operands()
										.forEach(
												(intersectionOperand) -> {
													if (intersectionOperand instanceof OWLClass) {
														// Handle classes normally, as though a superclass that we inherit from.
														intersectionOperand.accept(this);
													} else if (intersectionOperand instanceof OWLObjectRestriction) {
														// For object restrictions, we need to set the object property name
														// first.
														final var objectPropertyExpression =
																((OWLObjectRestriction) intersectionOperand).getProperty();
														final var objectProperty =
																objectPropertyExpression.asOWLObjectProperty();
														final var propertyName = objectProperty.getIRI().getShortForm();
														context.propertyNames.add(propertyName);
														context.withProcessedProperty(
																propertyName,
																() -> {
																	intersectionOperand.accept(this);
																});
													} else if (intersectionOperand instanceof OWLDataRestriction) {
														// For data restrictions, we need to set the data property name first.
														final var dataPropertyExpression =
																((OWLDataRestriction) intersectionOperand).getProperty();
														final var dataProperty = dataPropertyExpression.asOWLDataProperty();
														final var propertyName = dataProperty.getIRI().getShortForm();
														context.propertyNames.add(propertyName);
														context.withProcessedProperty(
																propertyName,
																() -> {
																	intersectionOperand.accept(this);
																});
													} else {
														// Not sure what would cause this, but lets spit out an error and figure
														// it out if we encounter it.
														logger.severe(
																"################ Operand instanceof ???:  " + intersectionOperand);
														logger.severe(
																"################ Taking no action for now.  Need to figure out"
																		+ " what use case this is.");
													}
												});
							} else {
								// Not sure this is a valid scenario??  This might happen for synonym classes?  Not
								// sure if other scenarios are valid (e.g. only an object/data property)??

								if (e instanceof OWLClass) {
									// Handle classes normally, as though a superclass that we inherit from.
									e.accept(this);
								} else if (e instanceof OWLObjectRestriction) {
									// For object restrictions, we need to set the object property name first.
									final var objectPropertyExpression = ((OWLObjectRestriction) e).getProperty();
									final var objectProperty = objectPropertyExpression.asOWLObjectProperty();
									final var propertyName = objectProperty.getIRI().getShortForm();
									context.propertyNames.add(propertyName);
									context.withProcessedProperty(
											propertyName,
											() -> {
												e.accept(this);
											});
								} else if (e instanceof OWLDataRestriction) {
									// For data restrictions, we need to set the data property name first.
									final var dataPropertyExpression = ((OWLDataRestriction) e).getProperty();
									final var dataProperty = dataPropertyExpression.asOWLDataProperty();
									final var propertyName = dataProperty.getIRI().getShortForm();
									context.propertyNames.add(propertyName);
									context.withProcessedProperty(
											propertyName,
											() -> {
												e.accept(this);
											});
								} else {
									// Not sure what would cause this, but lets spit out an error and figure it out if
									// we encounter it.
									logger.severe("################ Operand instanceof ???:  " + e);
									logger.severe(
											"################ Taking no action for now.  Need to figure out what use case"
													+ " this is.");
								}
							}
						});
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link
	 * OWLNaryBooleanClassExpression} (i.e. {@link OWLObjectUnionOf} or {@link
	 * OWLObjectIntersectionOf}).
	 *
	 * @param ce the OWLNaryBooleanClassExpression object
	 */
	private void visitOWLNaryBooleanClassExpression(@Nonnull OWLNaryBooleanClassExpression ce) {
		logger.info("\t-- analyzing OWLNaryBooleanClassExpression restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(MapperObjectProperty.getComplexObjectComposedSchema(ce));
		MapperObjectProperty.setSchemaType(currentPropertySchema, "array");
	}

	@Override
	public void visit(@Nonnull OWLObjectUnionOf ce) {
		this.visitOWLNaryBooleanClassExpression(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectIntersectionOf ce) {
		this.visitOWLNaryBooleanClassExpression(ce);
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link OWLQuantifiedObjectRestriction}
	 * (i.e. {@link OWLObjectAllValuesFrom}, {@link OWLObjectSomeValuesFrom}, or
	 * {@link OWLObjectCardinalityRestriction [subinterfaces: {@link OWLObjectExactCardinality}, {@link OWLObjectMaxCardinality}, or {@link OWLObjectMinCardinality}]).
	 *
	 * @param ce the {@link OWLQuantifiedObjectRestriction} object
	 */
	private void visitOWLQuantifiedObjectRestriction(@Nonnull OWLQuantifiedObjectRestriction or) {
		logger.info("\t-- analyzing OWLQuantifiedObjectRestriction restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + or);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

		final var ce = or.getFiller();
		if (ce instanceof OWLObjectOneOf) {
			ce.accept(this);
		} else if (ce instanceof OWLObjectUnionOf || ce instanceof OWLObjectIntersectionOf) {
			final var complexObjectRange =
					MapperObjectProperty.getComplexObjectComposedSchema((OWLNaryBooleanClassExpression) ce);

			if (or instanceof OWLObjectSomeValuesFrom) {
				MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(
						currentPropertySchema, complexObjectRange);
			} else if (or instanceof OWLObjectAllValuesFrom) {
				MapperObjectProperty.addAllOfToObjectPropertySchema(
						currentPropertySchema, complexObjectRange);
			}
		} else {
			final Integer restrictionValue =
					(or instanceof OWLObjectCardinalityRestriction)
							? ((OWLObjectCardinalityRestriction) or).getCardinality()
							: null;
			final var objRestrictionRange = this.getPrefixedSchemaName(ce.asOWLClass());

			// Update current property schema with the appropriate restriction range/value.
			if (or instanceof OWLObjectSomeValuesFrom) {
				MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(
						currentPropertySchema, objRestrictionRange);
			} else if (or instanceof OWLObjectAllValuesFrom) {
				MapperObjectProperty.addAllOfToObjectPropertySchema(
						currentPropertySchema, objRestrictionRange);
			} else if (or instanceof OWLObjectMinCardinality) {
				MapperObjectProperty.addMinCardinalityToPropertySchema(
						currentPropertySchema, restrictionValue, objRestrictionRange);
			} else if (or instanceof OWLObjectMaxCardinality) {
				MapperObjectProperty.addMaxCardinalityToPropertySchema(
						currentPropertySchema, restrictionValue, objRestrictionRange);
			} else if (or instanceof OWLObjectExactCardinality) {
				MapperObjectProperty.addExactCardinalityToPropertySchema(
						currentPropertySchema, restrictionValue, objRestrictionRange);
			}
		}
	}

	/**
	 * This method gets called when a class expression is an existential (someValuesFrom) restriction
	 * and it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLObjectSomeValuesFrom ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	/**
	 * This method gets called when a class expression is a universal (allValuesFrom) restriction and
	 * it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLObjectAllValuesFrom ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectMinCardinality ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectMaxCardinality ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectExactCardinality ce) {
		this.visitOWLQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLObjectComplementOf ce) {
		logger.info("\t-- analyzing OWLObjectComplementOf restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// ComplementOf can occur either for OWLClass or for one of its object properties.  If the
		// property name is null, assume it is a class's complement (and not a property's complement).
		if (context.currentlyProcessedPropertyName == null) {
			MapperObjectProperty.setComplementOfForObjectSchema(
					context.classSchema, this.getPrefixedSchemaName(ce.getOperand().asOWLClass()));
		} else {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema =
					this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

			MapperObjectProperty.setComplementOfForObjectSchema(
					currentPropertySchema, this.getPrefixedSchemaName(ce.getOperand().asOWLClass()));
		}
	}

	@Override
	public void visit(@Nonnull OWLObjectHasValue ce) {
		logger.info("\t-- analyzing OWLObjectHasValue restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

		if (ce.getFiller() != null && ce.getFiller() instanceof OWLNamedIndividual) {
			MapperObjectProperty.addHasValueOfPropertySchema(currentPropertySchema, ce.getFiller());
		} else {
			logger.severe(
					"Restriction for OWLObjectHasValue has unknown value \""
							+ ce.getFiller()
							+ "\", which is not an OWLNamedIndividual.  Skipping restriction.");
		}
	}

	@Override
	public void visit(@Nonnull OWLObjectOneOf ce) {
		logger.info("\t-- analyzing OWLObjectOneOf restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// ObjectOneOf can occur either for OWLClass or for one of its object properties.  If the
		// property name is null, assume this class is actually an enum.
		if (context.currentlyProcessedPropertyName == null) {
			var enumValues = ce.getOperandsAsList();
			if (enumValues != null && !enumValues.isEmpty()) {
				// Add enum individuals to restriction range
				enumValues.forEach(
						(indv) -> {
							MapperObjectProperty.addEnumValueToObjectSchema(
									context.classSchema, ((OWLNamedIndividual) indv).getIRI().getShortForm());
						});
			}
		} else {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema =
					this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

			for (OWLIndividual individual : ce.getIndividuals()) {
				MapperObjectProperty.addOneOfToObjectPropertySchema(
						currentPropertySchema, individual.asOWLNamedIndividual().getIRI().getShortForm());
			}
		}
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link
	 * OWLNaryDataRange} (i.e. {@link OWLDataUnionOf} or {@link OWLDataIntersectionOf}).
	 *
	 * @param ce the OWLNaryDataRange object
	 */
	private void visitOWLNaryDataRange(@Nonnull OWLNaryDataRange ce) {
		logger.info("\t-- analyzing OWLNaryDataRange restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(MapperDataProperty.getComplexDataComposedSchema(ce));
		MapperDataProperty.setSchemaType(currentPropertySchema, "array");
	}

	@Override
	public void visit(@Nonnull OWLDataUnionOf ce) {
		this.visitOWLNaryDataRange(ce);
	}

	@Override
	public void visit(@Nonnull OWLDataIntersectionOf ce) {
		this.visitOWLNaryDataRange(ce);
	}

	/**
	 * Convenience method for adding restriction values and ranges from a visit to {@link
	 * OWLQuantifiedDataRestriction} (i.e. {@link OWLDataAllValuesFrom}, {@link
	 * OWLDataSomeValuesFrom}, or {@link OWLDataCardinalityRestriction} [subinterfaces: {@link
	 * OWLDataMinCardinality}, {@link OWLDataMaxCardinality}, or {@link OWLDataExactCardinality}]).
	 *
	 * @param ce the {@link OWLQuantifiedDataRestriction} object
	 */
	private void visitOWLQuantifiedDataRestriction(@Nonnull OWLQuantifiedDataRestriction dr) {
		logger.info("\t-- analyzing OWLQuantifiedDataRestriction restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + dr);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

		Integer restrictionValue =
				(dr instanceof OWLDataCardinalityRestriction)
						? ((OWLDataCardinalityRestriction) dr).getCardinality()
						: null;

		final var ce = dr.getFiller();
		if (ce instanceof OWLDataOneOf) {
			ce.accept(this);
		} else if (ce instanceof OWLDataUnionOf || ce instanceof OWLDataIntersectionOf) {
			final var complexDataRange =
					MapperDataProperty.getComplexDataComposedSchema((OWLNaryDataRange) ce);

			if (dr instanceof OWLDataSomeValuesFrom) {
				MapperDataProperty.addSomeValuesFromToDataPropertySchema(
						currentPropertySchema, complexDataRange);
			} else if (dr instanceof OWLDataAllValuesFrom) {
				MapperDataProperty.addAllOfDataPropertySchema(currentPropertySchema, complexDataRange);
			}
		} else {
			if (ce instanceof OWLDatatypeRestriction) {
				final var restrictionDatatype = ((OWLDatatypeRestriction) ce).getDatatype();
				for (final var facet : ((OWLDatatypeRestriction) ce).getFacetRestrictions()) {
					final var dataRestrictionRange = restrictionDatatype.getIRI().getShortForm();

					if (dataRestrictionRange != null) {
						// Update current property schema with the appropriate restriction datatype/value.
						if (dr instanceof OWLDataSomeValuesFrom) {
							MapperDataProperty.addSomeValuesFromToDataPropertySchema(
									currentPropertySchema, dataRestrictionRange);
						} else if (dr instanceof OWLDataAllValuesFrom) {
							MapperDataProperty.addAllOfDataPropertySchema(
									currentPropertySchema, dataRestrictionRange);
						} else if (dr instanceof OWLDataMinCardinality) {
							MapperDataProperty.addMinCardinalityToPropertySchema(
									currentPropertySchema, restrictionValue, dataRestrictionRange);
						} else if (dr instanceof OWLDataMaxCardinality) {
							MapperDataProperty.addMaxCardinalityToPropertySchema(
									currentPropertySchema, restrictionValue, dataRestrictionRange);
						} else if (dr instanceof OWLDataExactCardinality) {
							MapperDataProperty.addExactCardinalityToPropertySchema(
									currentPropertySchema, restrictionValue, dataRestrictionRange);
						}

						MapperDataProperty.addDatatypeRestrictionToPropertySchema(currentPropertySchema, facet);
					} else {
						logger.severe(
								"\t   Invalid datatype restriction range (i.e. null).  Verify it is valid in the"
										+ " ontology.");
						logger.severe("");
					}
				}

			} else {
				final var dataRestrictionRange = ce.asOWLDatatype().getIRI().getShortForm();

				// Update current property schema with the appropriate restriction datatype/value.
				if (dr instanceof OWLDataSomeValuesFrom) {
					MapperDataProperty.addSomeValuesFromToDataPropertySchema(
							currentPropertySchema, dataRestrictionRange);
				} else if (dr instanceof OWLDataAllValuesFrom) {
					MapperDataProperty.addAllOfDataPropertySchema(
							currentPropertySchema, dataRestrictionRange);
				} else if (dr instanceof OWLDataMinCardinality) {
					MapperDataProperty.addMinCardinalityToPropertySchema(
							currentPropertySchema, restrictionValue, dataRestrictionRange);
				} else if (dr instanceof OWLDataMaxCardinality) {
					MapperDataProperty.addMaxCardinalityToPropertySchema(
							currentPropertySchema, restrictionValue, dataRestrictionRange);
				} else if (dr instanceof OWLDataExactCardinality) {
					MapperDataProperty.addExactCardinalityToPropertySchema(
							currentPropertySchema, restrictionValue, dataRestrictionRange);
				}
			}
		}
	}

	/**
	 * This method gets called when a class expression is a universal (allValuesFrom) restriction and
	 * it asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLDataAllValuesFrom ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}

	/**
	 * This method gets called when a class expression is a some (someValuesFrom) restriction and it
	 * asks us to visit it
	 */
	@Override
	public void visit(@Nonnull OWLDataSomeValuesFrom ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLDataMinCardinality ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLDataMaxCardinality ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLDataExactCardinality ce) {
		this.visitOWLQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(@Nonnull OWLDataOneOf ce) {
		logger.info("\t-- analyzing OWLDataOneOf restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		ce.values()
				.forEach(
						(oneOfValue) -> {
							// If no existing property schema, then create empty schema for it.
							final var currentPropertySchema =
									this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

							MapperDataProperty.addOneOfDataPropertySchema(currentPropertySchema, oneOfValue);
						});
	}

	@Override
	public void visit(@Nonnull OWLDataComplementOf ce) {
		logger.info("\t-- analyzing OWLDataComplementOf restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		ce.datatypesInSignature()
				.forEach(
						(complementOfDatatype) -> {
							// If no existing property schema, then create empty schema for it.
							final var currentPropertySchema =
									this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

							MapperDataProperty.setComplementOfForDataSchema(
									currentPropertySchema, complementOfDatatype);
						});
	}

	@Override
	public void visit(@Nonnull OWLDataHasValue ce) {
		logger.info("\t-- analyzing OWLDataHasValue restrictions --");
		logger.info("\t   class:  " + context.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(context.currentlyProcessedPropertyName);

		if (ce.getFiller() != null
				&& ce.getFiller() instanceof OWLLiteral
				&& ce.getFiller().getDatatype() != null) {
			MapperDataProperty.addHasValueOfPropertySchema(currentPropertySchema, ce.getFiller());
		} else {
			logger.severe(
					"Restriction for OWLDataHasValue has unknown value \""
							+ ce.getFiller()
							+ "\", which is not a valid OWLLiteral.  Skipping restriction.");
		}
	}
}
