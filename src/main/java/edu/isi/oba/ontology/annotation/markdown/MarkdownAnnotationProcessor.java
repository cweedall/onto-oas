package edu.isi.oba.ontology.annotation.markdown;

import edu.isi.oba.ontology.visitor.VisitorContext;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataRestriction;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectRestriction;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.search.EntitySearcher;

public class MarkdownAnnotationProcessor {
	private final Logger logger;
	private final VisitorContext context;

	public MarkdownAnnotationProcessor(VisitorContext context, Logger logger) {
		this.context = context;
		this.logger = logger;
	}

	/**
	 * Add annotation value to map for tracking markdown content.
	 *
	 * @param annotation an {@link OWLAnnotation} with the name of the markdown annotation and its
	 *     value.
	 * @param entity an {@link OWLEntity} with the class/property associated with the annotation.
	 */
	private void addMarkdownAnnotationsToMap(OWLAnnotation annotation, String annotationMappedTo) {
		final var annotationConfig = this.context.getConfigData().getAnnotationConfig();
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

							var propMarkdownValueMap =
									this.context.getMarkdownGenerationMap().get(markdownAnnotationName);
							if (propMarkdownValueMap == null) {
								propMarkdownValueMap = new TreeMap<>();
								this.context.addMarkdownGenerationMapEntry(
										markdownAnnotationName, propMarkdownValueMap);
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
	public void setMarkdownContentFromAxiomAnnotations() {
		for (final var refClass : this.context.getReferencedClasses()) {
			final var refClassName = refClass.getIRI().getShortForm();

			// Get markdown annotations from classes.
			EntitySearcher.getAnnotationObjects(refClass, this.context.getBaseClassOntology())
					.forEach(
							(annotation) -> {
								this.addMarkdownAnnotationsToMap(annotation, refClassName);
							});

			// Get markdown annotations from data properties.
			this.context
					.getBaseClassOntology()
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
													EntitySearcher.getAnnotationObjects(
																	dataProp, this.context.getBaseClassOntology())
															.forEach(
																	(annotation) -> {
																		this.addMarkdownAnnotationsToMap(
																				annotation,
																				refClassName + "#" + dataProp.getIRI().getShortForm());
																	});
												});
							});

			// Get markdown annotations from object properties.
			this.context
					.getBaseClassOntology()
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
													EntitySearcher.getAnnotationObjects(
																	objProp, this.context.getBaseClassOntology())
															.forEach(
																	(annotation) -> {
																		this.addMarkdownAnnotationsToMap(
																				annotation,
																				refClassName + "#" + objProp.getIRI().getShortForm());
																	});
												});
							});

			// Get annotations from subclass axioms/restrictions.
			this.context
					.getBaseClassOntology()
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
													EntitySearcher.getAnnotationObjects(
																	dataProp, this.context.getBaseClassOntology())
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
													EntitySearcher.getAnnotationObjects(
																	objProp, this.context.getBaseClassOntology())
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
}
