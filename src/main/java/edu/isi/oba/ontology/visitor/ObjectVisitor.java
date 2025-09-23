package edu.isi.oba.ontology.visitor;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.MapperDataProperty;
import edu.isi.oba.MapperObjectProperty;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.exceptions.OntologyVisitorException;
import edu.isi.oba.ontology.schema.SchemaBuilder;
import edu.isi.oba.ontology.schema.SchemaOrchestrator;
import edu.isi.oba.utils.ontology.OntologyDescriptionUtils;
import edu.isi.oba.utils.schema.SchemaCloneUtils;
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
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
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
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.reasoner.InferenceDepth;
import org.semanticweb.owlapi.search.EntitySearcher;

/** Visits existential restrictions and collects the properties which are restricted. */
public class ObjectVisitor implements OWLObjectVisitor {

	// private final OWLClass owlClass;
	private final VisitorContext context;
	private final RestrictionProcessor restrictionProcessor;

	/**
	 * Constructor for ObjectVisitor.
	 *
	 * <p>Initializes the visitor this.context using the provided configuration.
	 *
	 * @param configData a {@link YamlConfig} containing all details loaded from the configuration
	 *     file.
	 */
	public ObjectVisitor(OWLClass owlClass, OWLOntology ontology, YamlConfig configData)
			throws OntologyVisitorException {
		this.context = new VisitorContext(owlClass, ontology, configData, logger);
		this.restrictionProcessor = new RestrictionProcessor(this.context, logger, this);

		this.visit(this.context.getBaseClass());
	}

	// Package-private constructor for unit tests
	ObjectVisitor(OWLClass owlClass, OWLOntology ontology, YamlConfig configData, boolean skipVisit)
			throws Exception {
		this.context = new VisitorContext(owlClass, ontology, configData, logger);
		this.restrictionProcessor = new RestrictionProcessor(this.context, logger, this);
		if (!skipVisit) {
			this.visit(this.context.getBaseClass());
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
		return SchemaBuilder.getPrefixedSchemaName(owlClass, this.context.getBaseClassOntology());
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
		return new SchemaOrchestrator(this.context, logger).generateSchema();
	}

	/**
	 * Get all the classes referenced directly or indirectly (potentially through inheritance) by the
	 * base class.
	 *
	 * @return a {@link Set} of {@link OWLClass}
	 */
	public Set<OWLClass> getAllReferencedClasses() {
		return this.context.getReferencedClasses();
	}

	/**
	 * Get all the classes referenced directly or indirectly (potentially through inheritance) by the
	 * base class.
	 *
	 * @return a {@link Map} containing annotation mappings to mappings of property name and
	 *     annotation value.
	 */
	public Map<String, Map<String, String>> getMarkdownMappings() {
		return this.context.getMarkdownGenerationMap();
	}

	/**
	 * Wrapper for the {@link #visit(OWLClass)} method which also takes the visited class's ontology
	 * as an argument.
	 *
	 * @param visitedClassOntology an {@link OWLOntology} which contains the visited class.
	 * @param ce an {@link OWLClass} to be visited by this {@link ObjectVisitor} class.
	 */
	public void visit(@Nonnull OWLOntology visitedClassOntology, @Nonnull OWLClass ce) {
		this.context.setBaseClassOntology(visitedClassOntology);
		ce.accept(this);
	}

	/**
	 * @param ce an {@link OWLClass} to be visited by this {@link ObjectVisitor} class.
	 */
	@Override
	public void visit(@Nonnull OWLClass ce) {
		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!ce.equals(this.context.getOwlThing()) && !this.context.isClassProcessed(ce)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.context.markClassAsProcessed(ce);

			// If using inheritance references, make sure to add super classes.
			if (GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)
					&& GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)) {
				// Add the base and super classes to the referenced class set.
				this.context.addReferencedClass(ce);
			}

			// Loop through the ontologies to use the one relevant for the current OWLClass.
			// for (OWLOntology ontology : this.ontologies) {
			// Only traverse this OWLClass's super classes, if it is contained in the ontology.
			if (this.context.getBaseClassOntology().containsClassInSignature(ce.getIRI())) {
				// If it has subclass axioms, then loop through each to accept visits for all super
				// classes.
				this.context
						.getBaseClassOntology()
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
		if (this.context.getBaseClass().equals(ce)) {
			// Get all non-inherited object and data properties.
			this.context.addAllBasePropertiesMap(this.getObjectPropertySchemasForClass(ce));
			this.context.addAllBasePropertiesMap(this.getDataPropertySchemasForClass(ce));

			// Not using setProperties(), because it creates immutability which breaks unit tests.
			this.context.getBasePropertiesMap().forEach(this.context::addPropertyToSchema);

			// Generate restrictions for all properties of this class, regardless of following references
			// or not.
			this.generatePropertySchemasWithRestrictions(ce);
		} else {
			// If this is a superclass AND we are using inheritance/superclass references, still add the
			// properties to the properties map.
			if (!GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES)) {
				this.context.addAllBasePropertiesMap(this.getObjectPropertySchemasForClass(ce));
				this.context.addAllBasePropertiesMap(this.getDataPropertySchemasForClass(ce));

				// Not using setProperties(), because it creates immutability which breaks unit tests.
				this.context.getBasePropertiesMap().forEach(this.context::addPropertyToSchema);

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
		if (owlClass != null && owlClass.equals(this.context.getBaseClass())) {
			logger.info("--------------------------------------------------");
			logger.info("\tGenerating restrictions for:");
			logger.info("\t\t" + this.context.getBaseClass());
			logger.info("--------------------------------------------------");
		} else {
			logger.info("--------------------------------------------------");
			logger.info("\tGenerating restrictions for:");
			logger.info("\t\t" + this.context.getBaseClass());
			logger.info("\twhich were inherited from:");
			logger.info("\t\t" + owlClass);
			logger.info("--------------------------------------------------");
		}

		// Avoid cycles and accept visits from super classes for the purpose of getting all properties.
		if (!this.context.isRestrictionClassProcessed(owlClass)) {
			// If we are processing inherited restrictions then we recursively visit named supers.
			this.context.addProcessedRestrictionClass(owlClass);

			// Search the ontology for this OWLClass.
			// If it has subclass axioms, then loop through each to generate schema restrictions.
			this.context
					.getBaseClassOntology()
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
										this.context.setCurrentlyProcessedPropertyName(
												property.getIRI().getShortForm());

										// Add any classes referenced by the restriction.
										this.context.addAllReferencedClasses(
												ax.getSuperClass().getClassesInSignature());
									} else if (ax.getSuperClass() instanceof OWLBooleanClassExpression) {
										if (ax.getSuperClass() instanceof OWLObjectComplementOf) {

											// Add the object complement reference class.
											this.context.addAllReferencedClasses(
													ax.getSuperClass().getClassesInSignature());

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
									this.context.clearCurrentlyProcessedPropertyName();
								} else {
									logger.severe("\t" + this.getBaseClassName() + " has unknown restriction.");
									logger.severe("\t\taxiom:  " + ax);
								}
							});

			// For equivalent (to) classes (e.g. Defined classes) we need to accept the visit to
			// navigate it.
			this.context
					.getBaseClassOntology()
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
			EntitySearcher.getAnnotations(entity, this.context.getBaseClassOntology())
					.forEach(
							annotation -> {
								var propertySchema =
										this.context.getClassSchema().getProperties() == null
												? null
												: (Schema)
														this.context
																.getClassSchema()
																.getProperties()
																.get(entity.getIRI().getShortForm());

								if (propertySchema != null) {
									if (propertySchema.getDescription() == null
											|| propertySchema.getDescription().isBlank()) {
										final var propertyDescription =
												OntologyDescriptionUtils.getDescription(
																entity,
																this.context.getBaseClassOntology(),
																GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
														.orElse(null);
										MapperProperty.setSchemaDescription(propertySchema, propertyDescription);
									}

									final var annotationConfig = this.context.getConfigData().getAnnotationConfig();
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
									this.context.getClassSchema().getProperties() == null
											? null
											: (Schema)
													this.context
															.getClassSchema()
															.getProperties()
															.get(this.context.getCurrentlyProcessedPropertyName());

							if (propertySchema != null) {
								final var annotationConfig = this.context.getConfigData().getAnnotationConfig();
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
	 * Convenience method for getting the base class's short form name (i.e. only its name, not its
	 * full IRI).
	 *
	 * @return a {@link String} which is the (short form) name of the base class.
	 */
	private String getBaseClassName() {
		return this.context.getBaseClass().getIRI().getShortForm();
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
		this.context.addPropertyName(propertyName);

		final var returnObjPropertySchema = new Schema();

		this.context.withProcessedProperty(
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
									this.context.addReferencedClass(objPropRange.asOWLClass());

									if (EntitySearcher.getEquivalentClasses(
															objPropRange.asOWLClass(), this.context.getBaseClassOntology())
													.count()
											> 0) {
										// Add the object property name and NOT the class name that it refers to.
										this.context.addEnumProperty(propertyName);
									}
								});
					}

					// Also loop through range axioms for object property expression and add ranges to map,
					// complex
					// map, or visit the range, if unionOf/intersectionOf/oneOf.
					this.context
							.getBaseClassOntology()
							.objectPropertyRangeAxioms(objPropExpr)
							.forEach(
									(objPropRangeAxiom) -> {
										if (objPropRangeAxiom.getRange() instanceof OWLClass) {
											propertyRanges.add(
													this.getPrefixedSchemaName(objPropRangeAxiom.getRange().asOWLClass()));

											if (EntitySearcher.getEquivalentClasses(
																	objPropRangeAxiom.getRange().asOWLClass(),
																	this.context.getBaseClassOntology())
															.count()
													> 0) {
												// Add the object property name and NOT the class name that it refers to.
												this.context.addEnumProperty(propertyName);
											}

											// Add the range to the referenced class set.
											this.context.addReferencedClass(objPropRangeAxiom.getRange().asOWLClass());
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
							this.context.getClassSchema().getProperties() == null
									? null
									: (Schema) this.context.getClassSchema().getProperties().get(propertyName);

					try {
						final var propertyDescription =
								OntologyDescriptionUtils.getDescription(
												op,
												this.context.getBaseClassOntology(),
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
								op, Collections.singleton(this.context.getBaseClassOntology()).stream())) {
							logger.info("\t\tProperty is functional.  Therefore, required with a max of 1 item.");
							this.context.addFunctionalProperty(propertyName);
							this.context.addRequiredProperty(propertyName);
							MapperObjectProperty.setFunctionalForPropertySchema(objPropertySchema);
						}

						final var annotationConfig = this.context.getConfigData().getAnnotationConfig();
						if (annotationConfig != null) {
							final var propertyAnnotations = annotationConfig.getPropertyAnnotations();

							if (propertyAnnotations != null) {
								// If property contains the annotation property (name is specified in configuration
								// file)
								// indicating it is read-only, then set value on the schema.
								final var readOnlyAnnotation = propertyAnnotations.getReadOnlyFlagName();
								if (readOnlyAnnotation != null && !readOnlyAnnotation.isBlank()) {
									if (EntitySearcher.getAnnotations(op, this.context.getBaseClassOntology())
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
									if (EntitySearcher.getAnnotations(op, this.context.getBaseClassOntology())
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
		this.context
				.getReasoner()
				.subObjectProperties(
						this.context.getReasoner().getTopObjectPropertyNode().getRepresentativeElement(),
						InferenceDepth.DIRECT)
				.filter(objPropExpr -> !objPropExpr.isBottomEntity() && objPropExpr.isOWLObjectProperty())
				.forEach(
						(objPropExpr) -> {
							var isOwlClassDomainOfObjProp = false;

							// Check property contains this owlClass as a domain.
							for (final var objPropDomainAx :
									this.context.getBaseClassOntology().getObjectPropertyDomainAxioms(objPropExpr)) {
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
									this.context.getBaseClassOntology().getObjectPropertyRangeAxioms(objPropExpr)) {
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

								this.context.withProcessedProperty(
										propertyName,
										() -> {
											for (final var objPropRangeAx :
													this.context
															.getBaseClassOntology()
															.getObjectPropertyRangeAxioms(objPropExpr)) {
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
							this.context
									.getReasoner()
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
														this.context
																.getBaseClassOntology()
																.getObjectPropertyDomainAxioms(subObjPropExpr)) {
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
														this.context
																.getBaseClassOntology()
																.getObjectPropertyRangeAxioms(subObjPropExpr)) {
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

													this.context.withProcessedProperty(
															propertyName,
															() -> {
																for (final var subObjPropRangeAx :
																		this.context
																				.getBaseClassOntology()
																				.getObjectPropertyRangeAxioms(subObjPropExpr)) {
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
				this.context.getBaseClassOntology().getAxioms(AxiomType.DATA_PROPERTY_DOMAIN));

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
										this.context
												.getReasoner()
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
								this.context.addPropertyName(propertyName);
								this.context.withProcessedProperty(
										propertyName,
										() -> {
											logger.info("\thas property:  \"" + propertyName + "\"");

											final var propertyRanges = new HashSet<String>();
											final var complexDataRanges = new HashSet<OWLDataRange>();
											this.context
													.getBaseClassOntology()
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
																			this.context.getBaseClassOntology(),
																			GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
																	.orElse(null);

													// In cases, such as unionOf/intersectionOf/oneOf , the property schema
													// may
													// already be set.  Get it, if so.
													var dataPropertySchema =
															this.context.getClassSchema().getProperties() == null
																	? null
																	: (Schema)
																			this.context
																					.getClassSchema()
																					.getProperties()
																					.get(propertyName);

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
															dp,
															Collections.singleton(this.context.getBaseClassOntology())
																	.stream())) {
														logger.info(
																"\t\tProperty is functional.  Therefore, required with a max of 1"
																		+ " item.");
														this.context.addFunctionalProperty(propertyName);
														this.context.addRequiredProperty(propertyName);
														MapperDataProperty.setFunctionalForPropertySchema(dataPropertySchema);
													}

													final var annotationConfig =
															this.context.getConfigData().getAnnotationConfig();
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
																if (EntitySearcher.getAnnotations(
																						dp, this.context.getBaseClassOntology())
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
																if (EntitySearcher.getAnnotations(
																						dp, this.context.getBaseClassOntology())
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
																		EntitySearcher.getAnnotations(
																						dp, this.context.getBaseClassOntology())
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

	/** ================== Restrictions traversals ================== */
	@Override
	public void visit(@Nonnull OWLEquivalentClassesAxiom ax) {
		logger.info("\t-- analyzing OWLEquivalentClassesAxiom restrictions --");
		logger.info("\t   class:  " + this.context.getBaseClass());
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
													this.context
															.getBaseClassOntology()
															.getOWLOntologyManager()
															.getOntologyFormat(this.context.getBaseClassOntology());
											if (format.isPrefixOWLDocumentFormat()) {
												final var map =
														format.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
												final var fullIRI = indv.asOWLNamedIndividual().getIRI().toString();
												map.forEach(
														(prefix, iri) -> {
															if (!fullIRI.equals(fullIRI.replaceFirst(iri, ""))) {
																MapperObjectProperty.addEnumValueToObjectSchema(
																		this.context.getClassSchema(), fullIRI.replaceFirst(iri, ""));
															}
														});
											}
										});
							}
						});

		// Loop through the class expressions in the defined / equivalent-to classes axiom and accept
		// visits from everything else.
		ax.classExpressions()
				.filter((e) -> !this.context.getBaseClass().equals(e) && !(e instanceof OWLObjectOneOf))
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
														// intersectionOperand.accept(this);
														OWLClass cls = (OWLClass) intersectionOperand;
														if (!this.context.isClassProcessed(cls)) {
															this.context.markClassAsProcessed(cls);
															intersectionOperand.accept(this);
														}
													} else if (intersectionOperand instanceof OWLObjectRestriction) {
														// For object restrictions, we need to set the object property name
														// first.
														final var objectPropertyExpression =
																((OWLObjectRestriction) intersectionOperand).getProperty();
														final var objectProperty =
																objectPropertyExpression.asOWLObjectProperty();
														final var propertyName = objectProperty.getIRI().getShortForm();
														this.context.addPropertyName(propertyName);
														this.context.withProcessedProperty(
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
														this.context.addPropertyName(propertyName);
														this.context.withProcessedProperty(
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
									// e.accept(this);
									OWLClass cls = (OWLClass) e;
									if (!this.context.isClassProcessed(cls)) {
										this.context.markClassAsProcessed(cls);
										e.accept(this);
									}
								} else if (e instanceof OWLObjectRestriction) {
									// For object restrictions, we need to set the object property name first.
									final var objectPropertyExpression = ((OWLObjectRestriction) e).getProperty();
									final var objectProperty = objectPropertyExpression.asOWLObjectProperty();
									final var propertyName = objectProperty.getIRI().getShortForm();
									this.context.addPropertyName(propertyName);
									this.context.withProcessedProperty(
											propertyName,
											() -> {
												e.accept(this);
											});
								} else if (e instanceof OWLDataRestriction) {
									// For data restrictions, we need to set the data property name first.
									final var dataPropertyExpression = ((OWLDataRestriction) e).getProperty();
									final var dataProperty = dataPropertyExpression.asOWLDataProperty();
									final var propertyName = dataProperty.getIRI().getShortForm();
									this.context.addPropertyName(propertyName);
									this.context.withProcessedProperty(
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

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		this.restrictionProcessor.processQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		this.restrictionProcessor.processQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(OWLObjectMinCardinality ce) {
		this.restrictionProcessor.processQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		this.restrictionProcessor.processQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(OWLObjectExactCardinality ce) {
		this.restrictionProcessor.processQuantifiedObjectRestriction(ce);
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		this.restrictionProcessor.processNaryBooleanClassExpression(ce);
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		this.restrictionProcessor.processNaryBooleanClassExpression(ce);
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		this.restrictionProcessor.processComplementOf(ce);
	}

	@Override
	public void visit(OWLObjectHasValue ce) {
		this.restrictionProcessor.processHasValue(ce);
	}

	@Override
	public void visit(OWLObjectOneOf ce) {
		this.restrictionProcessor.processOneOf(ce);
	}

	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		this.restrictionProcessor.processQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		this.restrictionProcessor.processQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(OWLDataMinCardinality ce) {
		this.restrictionProcessor.processQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(OWLDataMaxCardinality ce) {
		this.restrictionProcessor.processQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(OWLDataExactCardinality ce) {
		this.restrictionProcessor.processQuantifiedDataRestriction(ce);
	}

	@Override
	public void visit(OWLDataUnionOf ce) {
		this.restrictionProcessor.processNaryDataRange(ce);
	}

	@Override
	public void visit(OWLDataIntersectionOf ce) {
		this.restrictionProcessor.processNaryDataRange(ce);
	}

	@Override
	public void visit(OWLDataComplementOf ce) {
		this.restrictionProcessor.processComplementOf(ce);
	}

	@Override
	public void visit(OWLDataHasValue ce) {
		this.restrictionProcessor.processHasValue(ce);
	}

	@Override
	public void visit(OWLDataOneOf ce) {
		this.restrictionProcessor.processOneOf(ce);
	}
}
