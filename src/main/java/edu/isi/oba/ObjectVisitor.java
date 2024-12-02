package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.semanticweb.owlapi.model.AxiomType;
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
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.search.EntitySearcher;

/** Visits existential restrictions and collects the properties which are restricted. */
public class ObjectVisitor implements OWLObjectVisitor {
	private final Set<OWLOntology> ontologies;
	private final YamlConfig configData;

	// Base class for this Object Visitor.  This _should_ be the class that .accept()s a visit from
	// this Object Visitor.
	private OWLClass baseClass;
	private OWLOntology ontologyOfBaseClass;
	private Schema classSchema;

	private OWLReasoner reasoner;
	private OWLReasonerFactory reasonerFactory;
	private OWLClass owlThing; // TODO: is this needed anymore??

	private final Map<String, Schema> basePropertiesMap = new HashMap<>();

	private final Set<String> propertyNames = new HashSet<>();
	private final Set<String> enumProperties = new HashSet<>();
	private final Set<String> requiredProperties = new HashSet<>();
	private final Set<String> functionalProperties = new HashSet<>();
	private final Set<OWLClass> referencedClasses = new HashSet<>();
	private final Set<OWLClass> processedClasses = new HashSet<>();
	private final Set<OWLClass> processedRestrictionClasses = new HashSet<>();

	// Used to keep track of a property being visited.  Necessary for complex visits which can involve
	// recursion, because the property name is not passable.
	private String currentlyProcessedPropertyName = null;

	/**
	 * Constructor for object visitor.
	 *
	 * @param ontologies the {@link Set} of {@link OWLOntology} loaded by the configuration file.
	 * @param configData a {@link YamlConfig} containing all details loaded from the configuration
	 *     file.
	 */
	ObjectVisitor(Set<OWLOntology> ontologies, YamlConfig configData) {
		this.ontologies = ontologies;
		this.configData = configData;
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
		if (this.baseClass == null) {
			this.baseClass = baseClass;
			final var visitedClassIRI = this.baseClass.getIRI();

			this.reasonerFactory = new StructuralReasonerFactory();

			// We can pragmatically determine the class's ontology based on the set of ontologies and the
			// class itself.  Also set the owl:Thing for that ontology.
			this.ontologies.stream()
					.takeWhile(ontology -> ontology.containsClassInSignature(visitedClassIRI))
					.forEach(
							(ontology) -> {
								this.ontologyOfBaseClass = ontology;
								this.reasoner = this.reasonerFactory.createReasoner(ontology);
								this.owlThing = this.reasoner.getTopClassNode().getRepresentativeElement();
							});

			this.classSchema = this.getBaseClassBasicSchema();
		}
	}

	/**
	 * Create and return a basic {@link Schema} to be used when adding other details, such as
	 * properties.
	 *
	 * @return a basic {@link Schema} for this visitor's base class.
	 */
	private Schema getBaseClassBasicSchema() {
		var basicClassSchema = new Schema();
		MapperProperty.setSchemaName(basicClassSchema, this.getBaseClassName());
		MapperProperty.setSchemaDescription(
				basicClassSchema,
				ObaUtils.getDescription(
						this.baseClass,
						this.ontologies,
						this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS)));
		MapperProperty.setSchemaType(basicClassSchema, "object");

		if (this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_PROPERTIES)) {
			// Not using setProperties(), because it creates immutability which breaks unit tests.
			this.getDefaultProperties()
					.forEach(
							(schemaName, schema) -> {
								basicClassSchema.addProperty(schemaName, schema);
							});
		}

		return basicClassSchema;
	}

	/**
	 * Get the OpenAPI schema for the base class specified in the constructor. Although somewhat
	 * convoluted, this schema will not be generated fully until the {@link #visit(OWLClass)} method
	 * has been called by the base OWLClass to accept this visitor class.
	 *
	 * @see {@link #visit(OWLClass)}
	 * @return a {@link Schema} for the entire class
	 */
	public Schema getClassSchema() {
		// There are cases where a property has ComplementOf set (i.e. "not: ...") but no items.
		// Because the schema is an array, it will have problems.
		// Need to either change it to "type: object" OR add empty items list (i.e. "items: {}").
		// Using the former approach because 1) it renders in Swagger UI correctly and 2) unclear if
		// empty items plus complement is a valid use case.
		final Map<String, Schema> classProperties =
				this.classSchema == null ? null : this.classSchema.getProperties();
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

		// Enums are already set up.  They should also never have required properties (even if
		// inherited somehow).
		if (this.classSchema.getEnum() != null) {
			this.requiredProperties.clear();
		}

		// Generate the required properties for the class, if applicable.
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.REQUIRED_PROPERTIES_FROM_CARDINALITY)) {
			this.generateRequiredPropertiesForClassSchemas();
		}

		// Convert non-array property items, if applicable.
		if (!this.configData.getConfigFlagValue(CONFIG_FLAG.ALWAYS_GENERATE_ARRAYS)) {
			MapperProperty.convertArrayToNonArrayPropertySchemas(
					this.classSchema,
					this.enumProperties,
					this.functionalProperties,
					this.configData.getConfigFlagValue(CONFIG_FLAG.FIX_SINGULAR_PLURAL_PROPERTY_NAMES));

			// If there are required properties, they may have changes (i.e. pluralized or singularized).
			// Make sure to clear and re-populate the Set/List.  (Primiarily done to keep everything in
			// alphabetical order)
			if (!this.requiredProperties.isEmpty()) {
				this.requiredProperties.clear();

				if (this.classSchema.getRequired() != null && !this.classSchema.getRequired().isEmpty()) {
					this.requiredProperties.addAll(
							(Set<String>) this.classSchema.getRequired().stream().collect(Collectors.toSet()));
					this.classSchema.setRequired(
							this.requiredProperties.stream().collect(Collectors.toList()));
				}
			}
		}

		// If following references AND use inheritance references (for the class), we do not want to
		// inherit/reference the same class multiple times accidentally.
		// (e.g. if we have Person > Student > ExchangeStudent, Student already inherits everything from
		// Person.
		// 		For ExchangeStudent, we do not want to inherit from Person AND Student.
		//		We only need to inherit from Student [which automatically inherits everything from Person
		// also].)
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)
				&& this.configData.getConfigFlagValue(CONFIG_FLAG.USE_INHERITANCE_REFERENCES)) {

			// If the class has a type (likely "object" - are there any other possibilities??), it needs
			// to be removed and added to the "allOf" entries.
			MapperProperty.setSchemaType(this.classSchema, null);

			// If adding for the first time, need to include a "type: object" entry.
			if (this.classSchema.getAllOf() == null || this.classSchema.getAllOf().isEmpty()) {
				final var objSchema = new ObjectSchema();
				this.classSchema.addAllOfItem(objSchema);
			}

			// All processed classes, minus the base class, are the super classes.
			final var superClasses = new HashSet<OWLClass>(this.processedClasses);
			superClasses.remove(this.baseClass);

			// Make a copy of the super classes.
			// Loop through all super classes and remove any super-super-classes that are being inherited
			// by a nearer/more direct super class to the base class.
			final var directSuperClasses = new HashSet<OWLClass>(superClasses);
			for (OWLClass superClassA : superClasses) {
				for (OWLClass superClassB : superClasses) {
					if (!superClassA.equals(superClassB)
							&& this.reasoner.getSuperClasses(superClassA, false).containsEntity(superClassB)) {
						directSuperClasses.remove(superClassB);
					}
				}
			}

			// Add all direct superclasses to allOf list.
			directSuperClasses.stream()
					.forEach(
							superClass -> {
								final var refSchema = new ObjectSchema();
								refSchema.set$ref(superClass.getIRI().getShortForm());

								this.classSchema.addAllOfItem(refSchema);
							});

			// If there is only one item in the allOf list, then it is "type: object".  That means nothing
			// is inherited.  Set it back to null.
			if (this.classSchema.getAllOf() != null && this.classSchema.getAllOf().size() == 1) {
				this.classSchema.setAllOf(null);
			}
		}

		return this.classSchema;
	}

	/**
	 * Get all the classes referenced directly or indirectly (potentially through inheritance) by the
	 * base class.
	 *
	 * @return a {@link Set} of {@link OWLClass}
	 */
	public Set<OWLClass> getAllReferencedClasses() {
		return this.referencedClasses;
	}

	/**
	 * @param ce an {@link OWLClass} to be visited by this visitor class.
	 */
	@Override
	public void visit(@Nonnull OWLClass ce) {
		// If the base class is null when this OWLClass is visited, then treat it as the base class and
		// set up this Visitor class with its basic details.
		if (this.baseClass == null) {
			this.initializeBaseClass(ce);

			// There is a possibility that the owl:thing for the ontology contains a universal property.
			// While generally rare, this might be some type of identifier, such as a GUID.
			this.owlThing.accept(this);
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!this.processedClasses.contains(ce)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.processedClasses.add(ce);

			// If using inheritance references, make sure to add super classes.
			if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)
					&& this.configData.getConfigFlagValue(CONFIG_FLAG.USE_INHERITANCE_REFERENCES)) {
				// Add the base and super classes to the referenced class set.
				this.referencedClasses.add(ce);
			}

			// Loop through the ontologies to use the one relevant for the current OWLClass.
			for (OWLOntology ontology : this.ontologies) {
				// Only traverse this OWLClass's super classes, if it is contained in the ontology.
				if (ontology.containsClassInSignature(ce.getIRI())) {
					// If it has subclass axioms, then loop through each to accept visits for all super
					// classes.
					ontology
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
		}

		// Only include properties from the base class OR we are not following references (because all
		// properties need to be copied to the base class in this case).
		// Inherited details should be determined via references.
		if (this.baseClass.equals(ce)) {
			// Get all non-inherited object and data properties.
			this.basePropertiesMap.putAll(this.getObjectPropertySchemasForClass(ce));
			this.basePropertiesMap.putAll(this.getDataPropertySchemasForClass(ce));

			// Not using setProperties(), because it creates immutability which breaks unit tests.
			this.basePropertiesMap.forEach(
					(schemaName, schema) -> {
						this.classSchema.addProperty(schemaName, schema);
					});

			// Generate restrictions for all properties of this class, regardless of following references
			// or not.
			this.generatePropertySchemasWithRestrictions(ce);
		} else {
			// If this is a superclass AND we are using inheritance/superclass references, still add the
			// properties to the properties map.
			if (!this.configData.getConfigFlagValue(CONFIG_FLAG.USE_INHERITANCE_REFERENCES)) {
				this.basePropertiesMap.putAll(this.getObjectPropertySchemasForClass(ce));
				this.basePropertiesMap.putAll(this.getDataPropertySchemasForClass(ce));

				// Not using setProperties(), because it creates immutability which breaks unit tests.
				this.basePropertiesMap.forEach(
						(schemaName, schema) -> {
							this.classSchema.addProperty(schemaName, schema);
						});

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
		if (owlClass != null && owlClass.equals(this.baseClass)) {
			logger.info("--------------------------------------------------");
			logger.info("\tGenerating restrictions for:");
			logger.info("\t\t" + this.baseClass);
			logger.info("--------------------------------------------------");
		} else {
			logger.info("--------------------------------------------------");
			logger.info("\tGenerating restrictions for:");
			logger.info("\t\t" + this.baseClass);
			logger.info("\twhich were inherited from:");
			logger.info("\t\t" + owlClass);
			logger.info("--------------------------------------------------");
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!this.processedRestrictionClasses.contains(owlClass)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.processedRestrictionClasses.add(owlClass);

			// Loop through the ontologies to use the one relevant for the current OWLClass.
			for (OWLOntology ontology : this.ontologies) {
				// Search the ontology for this OWLClass.
				// If it has subclass axioms, then loop through each to generate schema restrictions.
				ontology
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
											this.currentlyProcessedPropertyName = property.getIRI().getShortForm();

											// Add any classes referenced by the restriction.
											this.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());
										} else if (ax.getSuperClass() instanceof OWLBooleanClassExpression) {
											if (ax.getSuperClass() instanceof OWLObjectComplementOf) {
												// Add the object complement reference class.
												this.referencedClasses.addAll(ax.getSuperClass().getClassesInSignature());

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
															+ ax.getSuperClass().asOWLClass().getIRI().getShortForm()
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
													.forEach(
															(entity) -> {
																this.setDescriptionReadOnlyWriteOnlyFromAnnotations(entity);
															});
											ax.getSuperClass()
													.dataPropertiesInSignature()
													.forEach(
															(entity) -> {
																this.setDescriptionReadOnlyWriteOnlyFromAnnotations(entity);
															});

											// Also check the subClass axioms for annotations specifying read/write only.
											this.setReadOnlyWriteOnlyFromAxiomAnnotations(ax);
										}

										// Clear out the property name.
										this.currentlyProcessedPropertyName = null;
									} else {
										logger.severe("\t" + this.getBaseClassName() + " has unknown restriction.");
										logger.severe("\t\taxiom:  " + ax);
									}
								});

				// For equivalent (to) classes (e.g. Defined classes) we need to accept the visit to
				// navigate it.
				ontology
						.equivalentClassesAxioms(owlClass)
						.forEach(
								(eqClsAx) -> {
									eqClsAx.accept(this);
								});
			}
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
			EntitySearcher.getAnnotations(entity, this.ontologyOfBaseClass)
					.forEach(
							annotation -> {
								var propertySchema =
										this.classSchema.getProperties() == null
												? null
												: (Schema)
														this.classSchema.getProperties().get(entity.getIRI().getShortForm());

								if (propertySchema != null) {
									if (propertySchema.getDescription() == null
											|| propertySchema.getDescription().isBlank()) {
										final var propertyDescription =
												ObaUtils.getDescription(
														entity,
														this.ontologies,
														this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));
										MapperProperty.setSchemaDescription(propertySchema, propertyDescription);
									}

									final var propertyAnnotations = this.configData.getProperty_annotations();
									final var isPropertyAnnotationsPresent = propertyAnnotations.isPresent();

									// If property contains the annotation property (name is specified in
									// configuration
									// file) indicating it is read-only, then set value on the schema.
									final var readOnlyAnnotation =
											isPropertyAnnotationsPresent
													? propertyAnnotations.get().getRead_only_flag_name()
													: null;
									if (readOnlyAnnotation != null
											&& !readOnlyAnnotation.isBlank()
											&& readOnlyAnnotation.equals(
													annotation.getProperty().getIRI().getShortForm())) {
										MapperProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
									}

									// If property contains the annotation property (name is specified in
									// configuration
									// file) indicating it is write-only, then set value on the schema.
									final var writeOnlyAnnotation =
											isPropertyAnnotationsPresent
													? propertyAnnotations.get().getWrite_only_flag_name()
													: null;
									if (writeOnlyAnnotation != null
											&& !writeOnlyAnnotation.isBlank()
											&& writeOnlyAnnotation.equals(
													annotation.getProperty().getIRI().getShortForm())) {
										MapperProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
									}

									// If property contains the annotation property (name is specified in
									// configuration file) indicating what the example value is for a data property,
									// then set value on the schema.
									final var exampleValueAnnotation =
											isPropertyAnnotationsPresent
													? propertyAnnotations.get().getExample_value_name()
													: null;
									if (entity.isOWLDataProperty()
											&& exampleValueAnnotation != null
											&& !exampleValueAnnotation.isBlank()
											&& exampleValueAnnotation.equals(
													annotation.getProperty().getIRI().getShortForm())) {
										MapperDataProperty.setExampleValueForPropertySchema(propertySchema, annotation);
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
									this.classSchema.getProperties() == null
											? null
											: (Schema)
													this.classSchema.getProperties().get(this.currentlyProcessedPropertyName);

							if (propertySchema != null) {
								final var propertyAnnotations = this.configData.getProperty_annotations();
								final var isPropertyAnnotationsPresent = propertyAnnotations.isPresent();

								// If property contains the annotation property (name is specified in configuration
								// file) indicating it is read-only, then set value on the schema.
								final var readOnlyAnnotation =
										isPropertyAnnotationsPresent
												? propertyAnnotations.get().getRead_only_flag_name()
												: null;
								if (readOnlyAnnotation != null
										&& !readOnlyAnnotation.isBlank()
										&& readOnlyAnnotation.equals(
												annotation.getProperty().getIRI().getShortForm())) {
									MapperProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
								}

								// If property contains the annotation property (name is specified in configuration
								// file) indicating it is write-only, then set value on the schema.
								final var writeOnlyAnnotation =
										isPropertyAnnotationsPresent
												? propertyAnnotations.get().getWrite_only_flag_name()
												: null;
								if (writeOnlyAnnotation != null
										&& !writeOnlyAnnotation.isBlank()
										&& writeOnlyAnnotation.equals(
												annotation.getProperty().getIRI().getShortForm())) {
									MapperProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
								}

								// If property contains the annotation property (name is specified in
								// configuration file) indicating what the example value is for a data property,
								// then set value on the schema.
								final var exampleValueAnnotation =
										isPropertyAnnotationsPresent
												? propertyAnnotations.get().getExample_value_name()
												: null;
								if (exampleValueAnnotation != null
										&& !exampleValueAnnotation.isBlank()
										&& exampleValueAnnotation.equals(
												annotation.getProperty().getIRI().getShortForm())) {
									MapperDataProperty.setExampleValueForPropertySchema(propertySchema, annotation);
								}
							}
						});
	}

	/**
	 * Check each of the base class's properties and add it to the list of required properties, if it
	 * meets the criteria.
	 */
	private void generateRequiredPropertiesForClassSchemas() {
		final Map<String, Schema> propertySchemas =
				this.classSchema.getProperties() == null
						? new HashMap<>()
						: this.classSchema.getProperties();

		propertySchemas.forEach(
				(propertyName, propertySchema) -> {
					// If min value is 1, it is required and not nullable.
					// Otherwise, it is nullable.  Functional properties can be nullable while also being
					// required.
					if (propertySchema.getMinItems() != null && propertySchema.getMinItems() > 0) {
						MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
						this.requiredProperties.add(propertyName);
					} else {
						MapperProperty.setNullableValueForPropertySchema(propertySchema, true);

						if (this.functionalProperties.contains(propertyName)) {
							this.requiredProperties.add(propertyName);
						}
					}
				});

		this.classSchema.setRequired(this.requiredProperties.stream().collect(Collectors.toList()));
	}

	/**
	 * Convenience method for getting the base class's short form name (i.e. only its name, not its
	 * full IRI).
	 *
	 * @return a {@link String} which is the (short form) name of the base class.
	 */
	private String getBaseClassName() {
		return this.baseClass.getIRI().getShortForm();
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
		this.propertyNames.add(propertyName);
		this.currentlyProcessedPropertyName = propertyName;

		logger.info("\thas property:\t\"" + propertyName + "\"");

		final var propertyRanges = new HashSet<String>();
		final var complexObjectRanges = new HashSet<OWLClassExpression>();

		// Add object property OWLClass ranges to set of property ranges.
		if (objPropRanges != null) {
			objPropRanges.forEach(
					objPropRange -> {
						if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
							propertyRanges.add(objPropRange.asOWLClass().getIRI().getShortForm());

							// Add the range to the referenced class set.
							this.referencedClasses.add(objPropRange.asOWLClass());

							if (EntitySearcher.getEquivalentClasses(
													objPropRange.asOWLClass(), this.ontologyOfBaseClass)
											.count()
									> 0) {
								// Add the object property name and NOT the class name that it refers to.
								this.enumProperties.add(propertyName);
							}
						} else {
							propertyRanges.add(null);
						}
					});
		}

		// Also loop through range axioms for object property expression and add ranges to map, complex
		// map, or visit the range, if unionOf/intersectionOf/oneOf.
		this.ontologyOfBaseClass
				.objectPropertyRangeAxioms(objPropExpr)
				.forEach(
						(objPropRangeAxiom) -> {
							if (objPropRangeAxiom.getRange() instanceof OWLClass) {
								if (this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)) {
									propertyRanges.add(
											objPropRangeAxiom.getRange().asOWLClass().getIRI().getShortForm());

									if (EntitySearcher.getEquivalentClasses(
															objPropRangeAxiom.getRange().asOWLClass(), this.ontologyOfBaseClass)
													.count()
											> 0) {
										// Add the object property name and NOT the class name that it refers to.
										this.enumProperties.add(propertyName);
									}

									// Add the range to the referenced class set.
									this.referencedClasses.add(objPropRangeAxiom.getRange().asOWLClass());
								} else {
									propertyRanges.add(null);
								}
							} else if (objPropRangeAxiom.getRange() instanceof OWLObjectUnionOf
									|| objPropRangeAxiom.getRange() instanceof OWLObjectIntersectionOf
									|| objPropRangeAxiom.getRange() instanceof OWLObjectOneOf
									|| objPropRangeAxiom.getRange() instanceof OWLObjectComplementOf) {
								logger.info(
										"\t\t...has complex range -> proceeding to its restrictions immediately...");
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

		// In cases, such as unionOf/intersectionOf/oneOf , the property schema may already be set.  Get
		// it, if so.
		var objPropertySchema =
				this.classSchema.getProperties() == null
						? null
						: (Schema) this.classSchema.getProperties().get(propertyName);

		try {
			final var propertyDescription =
					ObaUtils.getDescription(
							op,
							this.ontologies,
							this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));

			// Workaround for handling unionOf/intersectionOf/oneOf cases which may be set already above.
			if (objPropertySchema == null) {
				// Get object property schema from mapper.
				objPropertySchema =
						MapperObjectProperty.createObjectPropertySchema(
								propertyName, propertyDescription, propertyRanges);
			} else {
				// These do not get set properly because the unionOf/intersectionOf/oneOf property schema
				// was not created via MapperDataProperty.createDataPropertySchema().
				MapperObjectProperty.setSchemaName(objPropertySchema, propertyName);
				MapperObjectProperty.setSchemaDescription(objPropertySchema, propertyDescription);
			}

			// If property is functional, set the schema accordingly.
			if (EntitySearcher.isFunctional(
					op, Collections.singleton(this.ontologyOfBaseClass).stream())) {
				logger.info("\t\tProperty is functional.  Therefore, required with a max of 1 item.");
				this.functionalProperties.add(propertyName);
				this.requiredProperties.add(propertyName);
				MapperObjectProperty.setFunctionalForPropertySchema(objPropertySchema);
			}

			final var propertyAnnotations = this.configData.getProperty_annotations();
			final var isPropertyAnnotationsPresent = propertyAnnotations.isPresent();

			// If property contains the annotation property (name is specified in configuration file)
			// indicating it is read-only, then set value on the schema.
			final var readOnlyAnnotation =
					isPropertyAnnotationsPresent ? propertyAnnotations.get().getRead_only_flag_name() : null;
			if (readOnlyAnnotation != null && !readOnlyAnnotation.isBlank()) {
				if (EntitySearcher.getAnnotations(op, this.ontologyOfBaseClass)
								.filter(
										annotation ->
												readOnlyAnnotation.equals(annotation.getProperty().getIRI().getShortForm()))
								.count()
						> 0) {
					MapperObjectProperty.setReadOnlyValueForPropertySchema(objPropertySchema, true);
				}
			}

			// If property contains the annotation property (name is specified in configuration file)
			// indicating it is write-only, then set value on the schema.
			final var writeOnlyAnnotation =
					isPropertyAnnotationsPresent ? propertyAnnotations.get().getWrite_only_flag_name() : null;
			if (writeOnlyAnnotation != null && !writeOnlyAnnotation.isBlank()) {
				if (EntitySearcher.getAnnotations(op, this.ontologyOfBaseClass)
								.filter(
										annotation ->
												writeOnlyAnnotation.equals(
														annotation.getProperty().getIRI().getShortForm()))
								.count()
						> 0) {
					MapperObjectProperty.setWriteOnlyValueForPropertySchema(objPropertySchema, true);
				}
			}

			// For any complex property ranges, traverse.  This will grab restrictions also.  There is no
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

		this.currentlyProcessedPropertyName = null;

		return objPropertySchema;
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
		this.reasoner
				.subObjectProperties(
						this.reasoner.getTopObjectPropertyNode().getRepresentativeElement(),
						InferenceDepth.DIRECT)
				.filter(objPropExpr -> !objPropExpr.isBottomEntity() && objPropExpr.isOWLObjectProperty())
				.forEach(
						(objPropExpr) -> {
							var isOwlClassDomainOfObjProp = false;

							// Check property contains this owlClass as a domain.
							for (final var objPropDomainAx :
									this.ontologyOfBaseClass.getObjectPropertyDomainAxioms(objPropExpr)) {
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
									this.ontologyOfBaseClass.getObjectPropertyRangeAxioms(objPropExpr)) {
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

								// Keep track of the property name for the accept() call in the FOR loop below.
								this.currentlyProcessedPropertyName = propertyName;

								for (final var objPropRangeAx :
										this.ontologyOfBaseClass.getObjectPropertyRangeAxioms(objPropExpr)) {
									final var range = objPropRangeAx.getRange();

									this.currentlyProcessedPropertyName = propertyName;

									// For complex ranges which are unions or intersections, treat like a restriction.
									if (range instanceof OWLObjectUnionOf
											|| range instanceof OWLObjectIntersectionOf) {
										range.accept(this);
									}
								}

								this.currentlyProcessedPropertyName = null;
							}

							// ==========================================================================================
							// Junky workaround to use variable within the stream() below.
							final var isOwlClassDomainOfObjPropFinal = isOwlClassDomainOfObjProp;

							// Loop through all sub-properties of this property.
							this.reasoner
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
														this.ontologyOfBaseClass.getObjectPropertyDomainAxioms(
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
														this.ontologyOfBaseClass.getObjectPropertyRangeAxioms(subObjPropExpr)) {
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

													// Keep track of the property name for the accept() call in the FOR loop
													// below.
													this.currentlyProcessedPropertyName = propertyName;

													for (final var subObjPropRangeAx :
															this.ontologyOfBaseClass.getObjectPropertyRangeAxioms(
																	subObjPropExpr)) {
														final var range = subObjPropRangeAx.getRange();

														// For complex ranges which are unions or intersections, treat like a
														// restriction.
														if (range instanceof OWLObjectUnionOf
																|| range instanceof OWLObjectIntersectionOf) {
															range.accept(this);
														}
													}

													this.currentlyProcessedPropertyName = null;
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
		for (OWLOntology ontology : this.ontologies) {
			dataPropDomainAxioms.addAll(ontology.getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));
		}

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
										this.reasoner
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
								this.propertyNames.add(propertyName);
								this.currentlyProcessedPropertyName = propertyName;

								logger.info("\thas property:  \"" + propertyName + "\"");

								final var propertyRanges = new HashSet<String>();
								final var complexDataRanges = new HashSet<OWLDataRange>();
								this.ontologyOfBaseClass
										.dataPropertyRangeAxioms(dp)
										.forEach(
												(dataPropRangeAxiom) -> {
													if (dataPropRangeAxiom.getRange() instanceof OWLDatatype) {
														propertyRanges.add(
																((OWLDatatype) dataPropRangeAxiom.getRange())
																		.getIRI()
																		.getShortForm());
													} else if (dataPropRangeAxiom.getRange() instanceof OWLDataUnionOf
															|| dataPropRangeAxiom.getRange() instanceof OWLDataIntersectionOf
															|| dataPropRangeAxiom.getRange() instanceof OWLDataOneOf
															|| dataPropRangeAxiom.getRange() instanceof OWLDataComplementOf) {
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
									logger.warning("\t\tProperty \"" + dp.getIRI() + "\" has range equals zero.");
								} else {
									logger.info("\t\tProperty range(s): " + propertyRanges);

									try {
										final var propertyDescription =
												ObaUtils.getDescription(
														dp,
														this.ontologies,
														this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS));

										// In cases, such as unionOf/intersectionOf/oneOf , the property schema may
										// already be set.  Get it, if so.
										var dataPropertySchema =
												this.classSchema.getProperties() == null
														? null
														: (Schema) this.classSchema.getProperties().get(propertyName);

										// Workaround for handling unionOf/intersectionOf/oneOf cases which may be set
										// already above.
										if (dataPropertySchema == null) {
											// Get data property schema from mapper.
											dataPropertySchema =
													MapperDataProperty.createDataPropertySchema(
															propertyName, propertyDescription, propertyRanges);
										} else {
											// These do not get set properly because the unionOf/intersectionOf/oneOf
											// property schema was not created via
											// MapperDataProperty.createDataPropertySchema().
											MapperDataProperty.setSchemaName(dataPropertySchema, propertyName);
											MapperDataProperty.setSchemaDescription(
													dataPropertySchema, propertyDescription);
										}

										// If property is functional, set the schema accordingly.
										if (EntitySearcher.isFunctional(
												dp, Collections.singleton(this.ontologyOfBaseClass).stream())) {
											logger.info(
													"\t\tProperty is functional.  Therefore, required with a max of 1 item.");
											this.functionalProperties.add(propertyName);
											this.requiredProperties.add(propertyName);
											MapperDataProperty.setFunctionalForPropertySchema(dataPropertySchema);
										}

										final var propertyAnnotations = this.configData.getProperty_annotations();
										final var isPropertyAnnotationsPresent = propertyAnnotations.isPresent();

										// If property contains the annotation property (name is specified in
										// configuration file) indicating it is read-only, then set value on the schema.
										final var readOnlyAnnotation =
												isPropertyAnnotationsPresent
														? propertyAnnotations.get().getRead_only_flag_name()
														: null;
										if (readOnlyAnnotation != null && !readOnlyAnnotation.isBlank()) {
											if (EntitySearcher.getAnnotations(dp, this.ontologyOfBaseClass)
															.filter(
																	annotation ->
																			readOnlyAnnotation.equals(
																					annotation.getProperty().getIRI().getShortForm()))
															.count()
													> 0) {
												MapperDataProperty.setReadOnlyValueForPropertySchema(
														dataPropertySchema, true);
											}
										}

										// If property contains the annotation property (name is specified in
										// configuration file) indicating it is write-only, then set value on the
										// schema.
										final var writeOnlyAnnotation =
												isPropertyAnnotationsPresent
														? propertyAnnotations.get().getWrite_only_flag_name()
														: null;
										if (writeOnlyAnnotation != null && !writeOnlyAnnotation.isBlank()) {
											if (EntitySearcher.getAnnotations(dp, this.ontologyOfBaseClass)
															.filter(
																	annotation ->
																			writeOnlyAnnotation.equals(
																					annotation.getProperty().getIRI().getShortForm()))
															.count()
													> 0) {
												MapperDataProperty.setWriteOnlyValueForPropertySchema(
														dataPropertySchema, true);
											}
										}

										// If property contains the annotation property (name is specified in
										// configuration file) indicating what the example value is for a data property,
										// then set value on the schema.
										final var exampleValueAnnotation =
												isPropertyAnnotationsPresent
														? propertyAnnotations.get().getExample_value_name()
														: null;
										if (exampleValueAnnotation != null && !exampleValueAnnotation.isBlank()) {
											for (final var annotation :
													EntitySearcher.getAnnotations(dp, this.ontologyOfBaseClass)
															.filter(
																	annotation ->
																			exampleValueAnnotation.equals(
																					annotation.getProperty().getIRI().getShortForm()))
															.collect(Collectors.toSet())) {
												MapperDataProperty.setExampleValueForPropertySchema(
														dataPropertySchema, annotation);
											}
										}

										// Save object property schema to class's schema.
										dataPropertiesMap.put(dataPropertySchema.getName(), dataPropertySchema);

										// For any complex property ranges, traverse.  This will grab restrictions also.
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

								this.currentlyProcessedPropertyName = null;
							}
						});

		return dataPropertiesMap;
	}

	/**
	 * Get default schema properties.
	 *
	 * <p>These can be disabled by setting `default_properties` to `false` in the `config.yaml` file.
	 *
	 * @return A Map where key is property name and value is the property's Swagger/OpenAPI Schema
	 */
	private Map<String, Schema> getDefaultProperties() {
		// Add some typical default properties (e.g. id, lable, type, and description)
		final var idPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"id",
						"identifier",
						new HashSet<String>() {
							{
								add("integer");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(idPropertySchema, false);
		final var labelPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"label",
						"short description of the resource",
						new HashSet<String>() {
							{
								add("string");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(labelPropertySchema, true);
		final var typePropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"type",
						"type(s) of the resource",
						new HashSet<String>() {
							{
								add("string");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(typePropertySchema, true);
		final var descriptionPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"description",
						"small description",
						new HashSet<String>() {
							{
								add("string");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(descriptionPropertySchema, true);

		// Also add some default property examples of different types (e.g. a date/time, a boolean, and
		// a float)
		final var eventDateTimePropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"eventDateTime",
						"a date/time of the resource",
						new HashSet<String>() {
							{
								add("dateTime");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(eventDateTimePropertySchema, true);
		final var isBoolPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"isBool",
						"a boolean indicator of the resource",
						new HashSet<String>() {
							{
								add("boolean");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(isBoolPropertySchema, true);
		final var quantityPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"quantity",
						"a number quantity of the resource",
						new HashSet<String>() {
							{
								add("float");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(quantityPropertySchema, true);

		return Map.ofEntries(
				Map.entry(idPropertySchema.getName(), idPropertySchema),
				Map.entry(labelPropertySchema.getName(), labelPropertySchema),
				Map.entry(typePropertySchema.getName(), typePropertySchema),
				Map.entry(descriptionPropertySchema.getName(), descriptionPropertySchema),
				Map.entry(eventDateTimePropertySchema.getName(), eventDateTimePropertySchema),
				Map.entry(isBoolPropertySchema.getName(), isBoolPropertySchema),
				Map.entry(quantityPropertySchema.getName(), quantityPropertySchema));
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
				this.classSchema.getProperties() == null
						? null
						: (Schema) this.classSchema.getProperties().get(propertyName);

		// In certain cases, a property was not set up with domains/ranges but has a restriction.
		// This property will not exist in the map of property names + schemas yet, so add it and set it
		// up with basic info.
		if (currentPropertySchema == null) {
			currentPropertySchema = new ArraySchema();
			MapperProperty.setSchemaName(currentPropertySchema, propertyName);

			final var propertyDescription =
					this.configData.getConfigFlagValue(CONFIG_FLAG.DEFAULT_DESCRIPTIONS)
							? ObaUtils.DEFAULT_DESCRIPTION
							: null;
			MapperProperty.setSchemaDescription(currentPropertySchema, propertyDescription);

			// If this was a new property schema, need to make sure it's added.
			this.classSchema.addProperty(propertyName, currentPropertySchema);
		}

		return currentPropertySchema;
	}

	/** ================== Restrictions traversals ================== */
	@Override
	public void visit(@Nonnull OWLEquivalentClassesAxiom ax) {
		logger.info("\t-- analyzing OWLEquivalentClassesAxiom restrictions --");
		logger.info("\t   class:  " + this.baseClass);
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
											// Basically loop through all the prefixes and find/replace the current prefix
											// individual's IRI with nothing.
											// If different, then we've found the individual and know the short form name.
											final var format =
													this.ontologyOfBaseClass
															.getOWLOntologyManager()
															.getOntologyFormat(this.ontologyOfBaseClass);
											if (format.isPrefixOWLDocumentFormat()) {
												final var map =
														format.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
												final var fullIRI = indv.asOWLNamedIndividual().getIRI().toString();
												map.forEach(
														(prefix, iri) -> {
															if (!fullIRI.equals(fullIRI.replaceFirst(iri, ""))) {
																MapperObjectProperty.addEnumValueToObjectSchema(
																		this.classSchema, fullIRI.replaceFirst(iri, ""));
															}
														});
											}
										});
							}
						});

		// Loop through the class expressions in the defined / equivalent-to classes axiom and accept
		// visits from everything else.
		ax.classExpressions()
				.filter((e) -> !this.baseClass.equals(e) && !(e instanceof OWLObjectOneOf))
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
														this.propertyNames.add(propertyName);
														this.currentlyProcessedPropertyName = propertyName;
														intersectionOperand.accept(this);
														this.currentlyProcessedPropertyName = null;
													} else if (intersectionOperand instanceof OWLDataRestriction) {
														// For data restrictions, we need to set the data property name first.
														final var dataPropertyExpression =
																((OWLDataRestriction) intersectionOperand).getProperty();
														final var dataProperty = dataPropertyExpression.asOWLDataProperty();
														final var propertyName = dataProperty.getIRI().getShortForm();
														this.propertyNames.add(propertyName);
														this.currentlyProcessedPropertyName = propertyName;
														intersectionOperand.accept(this);
														this.currentlyProcessedPropertyName = null;
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
									this.propertyNames.add(propertyName);
									this.currentlyProcessedPropertyName = propertyName;
									e.accept(this);
									this.currentlyProcessedPropertyName = null;
								} else if (e instanceof OWLDataRestriction) {
									// For data restrictions, we need to set the data property name first.
									final var dataPropertyExpression = ((OWLDataRestriction) e).getProperty();
									final var dataProperty = dataPropertyExpression.asOWLDataProperty();
									final var propertyName = dataProperty.getIRI().getShortForm();
									this.propertyNames.add(propertyName);
									this.currentlyProcessedPropertyName = propertyName;
									e.accept(this);
									this.currentlyProcessedPropertyName = null;
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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

		currentPropertySchema.setItems(
				MapperObjectProperty.getComplexObjectComposedSchema(
						ce, this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)));
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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + or);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

		final var ce = or.getFiller();
		if (ce instanceof OWLObjectOneOf) {
			ce.accept(this);
		} else if (ce instanceof OWLObjectUnionOf || ce instanceof OWLObjectIntersectionOf) {
			final var complexObjectRange =
					MapperObjectProperty.getComplexObjectComposedSchema(
							(OWLNaryBooleanClassExpression) ce,
							this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES));

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
			final var objRestrictionRange =
					this.configData.getConfigFlagValue(CONFIG_FLAG.FOLLOW_REFERENCES)
							? ce.asOWLClass().getIRI().getShortForm()
							: null;

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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// ComplementOf can occur either for OWLClass or for one of its object properties.  If the
		// property name is null, assume it is a class's complement (and not a property's complement).
		if (this.currentlyProcessedPropertyName == null) {
			MapperObjectProperty.setComplementOfForObjectSchema(
					this.classSchema, ce.getOperand().asOWLClass().getIRI().getShortForm());
		} else {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema =
					this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

			MapperObjectProperty.setComplementOfForObjectSchema(
					currentPropertySchema, ce.getOperand().asOWLClass().getIRI().getShortForm());
		}
	}

	@Override
	public void visit(@Nonnull OWLObjectHasValue ce) {
		logger.info("\t-- analyzing OWLObjectHasValue restrictions --");
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// ObjectOneOf can occur either for OWLClass or for one of its object properties.  If the
		// property name is null, assume this class is actually an enum.
		if (this.currentlyProcessedPropertyName == null) {
			var enumValues = ce.getOperandsAsList();
			if (enumValues != null && !enumValues.isEmpty()) {
				// Add enum individuals to restriction range
				enumValues.forEach(
						(indv) -> {
							MapperObjectProperty.addEnumValueToObjectSchema(
									this.classSchema, ((OWLNamedIndividual) indv).getIRI().getShortForm());
						});
			}
		} else {
			// If no existing property schema, then create empty schema for it.
			final var currentPropertySchema =
					this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + dr);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

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
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		ce.values()
				.forEach(
						(oneOfValue) -> {
							// If no existing property schema, then create empty schema for it.
							final var currentPropertySchema =
									this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

							MapperDataProperty.addOneOfDataPropertySchema(currentPropertySchema, oneOfValue);
						});
	}

	@Override
	public void visit(@Nonnull OWLDataComplementOf ce) {
		logger.info("\t-- analyzing OWLDataComplementOf restrictions --");
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		ce.datatypesInSignature()
				.forEach(
						(complementOfDatatype) -> {
							// If no existing property schema, then create empty schema for it.
							final var currentPropertySchema =
									this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

							MapperDataProperty.setComplementOfForDataSchema(
									currentPropertySchema, complementOfDatatype);
						});
	}

	@Override
	public void visit(@Nonnull OWLDataHasValue ce) {
		logger.info("\t-- analyzing OWLDataHasValue restrictions --");
		logger.info("\t   class:  " + this.baseClass);
		logger.info("\t   axiom:  " + ce);
		logger.info("");

		// If no existing property schema, then create empty schema for it.
		final var currentPropertySchema =
				this.getPropertySchemaForRestrictionVisit(this.currentlyProcessedPropertyName);

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
