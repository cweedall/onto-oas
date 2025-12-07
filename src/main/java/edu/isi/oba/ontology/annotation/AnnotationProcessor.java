package edu.isi.oba.ontology.annotation;

import edu.isi.oba.MapperDataProperty;
import edu.isi.oba.MapperObjectProperty;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.config.ontology.annotations.AnnotationConfig;
import edu.isi.oba.utils.ontology.OntologyDescriptionUtils;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;

public final class AnnotationProcessor {
	private AnnotationProcessor() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * For an OWLEntity (that is an object/data property), check its description and annotations. If
	 * it has either, update the property schema with description and/or write/read only annotation
	 * flags.
	 *
	 * @param schema
	 * @param entity
	 * @param ontology
	 * @param config
	 */
	public static void applyEntityAnnotations(
			@Nonnull Schema schema,
			@Nonnull OWLEntity entity,
			@Nonnull OWLOntology ontology,
			@Nullable AnnotationConfig config) {
		if (!(entity.isOWLDataProperty() || entity.isOWLObjectProperty())
				|| config == null
				|| schema == null) return;

		final var propertyName = entity.getIRI().getShortForm();

		// If it has properties, assume it's a classs schema.  Otherwise, it's an enum or property
		// schema already.
		final var propertySchema =
				schema.getProperties() == null ? schema : (Schema) schema.getProperties().get(propertyName);

		// Description
		if (propertySchema.getDescription() == null || propertySchema.getDescription().isBlank()) {
			final var description =
					OntologyDescriptionUtils.getDescription(
									entity, ontology, GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
							.orElse(null);

			if (entity.isOWLDataProperty()) {
				MapperDataProperty.setSchemaDescription(propertySchema, description);
			} else {
				MapperObjectProperty.setSchemaDescription(propertySchema, description);
			}
		}

		final var propertyAnnotations = config.getPropertyAnnotations();
		if (propertyAnnotations == null) return;

		final Set<OWLAnnotation> annotations =
				EntitySearcher.getAnnotations(entity, ontology).collect(Collectors.toSet());

		for (var annotation : annotations) {
			final var annotationName = annotation.getProperty().getIRI().getShortForm();

			// Read-only
			if (propertyAnnotations.getReadOnlyFlagName() != null
					&& propertyAnnotations.getReadOnlyFlagName().equals(annotationName)) {
				if (entity.isOWLDataProperty()) {
					MapperDataProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
				} else {
					MapperObjectProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
				}
			}

			// Write-only
			if (propertyAnnotations.getWriteOnlyFlagName() != null
					&& propertyAnnotations.getWriteOnlyFlagName().equals(annotationName)) {
				if (entity.isOWLDataProperty()) {
					MapperDataProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
				} else {
					MapperObjectProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
				}
			}

			// Example value (only for data properties)
			if (entity.isOWLDataProperty()
					&& propertyAnnotations.getExampleValueName() != null
					&& propertyAnnotations.getExampleValueName().equals(annotationName)) {
				MapperDataProperty.setExampleValueForPropertySchema(propertySchema, annotation);
			}
		}
	}

	/**
	 * For an OWLAxiom, check its annotations. If has a read/write one then update the property schema
	 * with write/read only annotation flags.
	 *
	 * @param schema
	 * @param axiom
	 * @param propertyName
	 * @param config
	 */
	public static void applyAxiomAnnotations(
			@Nonnull Schema schema,
			@Nonnull OWLAxiom axiom,
			@Nonnull String propertyName,
			@Nullable AnnotationConfig config) {
		if (schema.getProperties() == null || config == null) return;

		final var propertySchema = (Schema) schema.getProperties().get(propertyName);
		if (propertySchema == null) return;

		final var propertyAnnotations = config.getPropertyAnnotations();
		if (propertyAnnotations == null) return;

		final var readOnlyFlag = propertyAnnotations.getReadOnlyFlagName();
		final var writeOnlyFlag = propertyAnnotations.getWriteOnlyFlagName();
		final var exampleFlag = propertyAnnotations.getExampleValueName();

		for (var annotation : axiom.annotations().collect(Collectors.toSet())) {
			final var annotationName = annotation.getProperty().getIRI().getShortForm();

			// Read-only
			if (readOnlyFlag != null && !readOnlyFlag.isBlank() && readOnlyFlag.equals(annotationName)) {
				MapperProperty.setReadOnlyValueForPropertySchema(propertySchema, true);
			}

			// Write-only
			if (writeOnlyFlag != null
					&& !writeOnlyFlag.isBlank()
					&& writeOnlyFlag.equals(annotationName)) {
				MapperProperty.setWriteOnlyValueForPropertySchema(propertySchema, true);
			}

			// Example value (only for data properties)
			if (exampleFlag != null && !exampleFlag.isBlank() && exampleFlag.equals(annotationName)) {
				MapperDataProperty.setExampleValueForPropertySchema(propertySchema, annotation);
			}
		}
	}

	// public static void applyPropertyFlagsFromAnnotations(
	// 		@Nonnull Schema schema,
	// 		@Nonnull OWLDataProperty property,
	// 		@Nonnull OWLOntology ontology,
	// 		@Nullable AnnotationConfig config) {
	// 	applyPropertyFlagsFromAnnotations(schema, property, ontology, config, true);
	// }

	// public static void applyPropertyFlagsFromAnnotations(
	// 		@Nonnull Schema schema,
	// 		@Nonnull OWLObjectProperty property,
	// 		@Nonnull OWLOntology ontology,
	// 		@Nullable AnnotationConfig config) {
	// 	applyPropertyFlagsFromAnnotations(schema, property, ontology, config, false);
	// }

	// public static void applyPropertyFlagsFromAnnotations(
	// 		@Nonnull Schema schema,
	// 		@Nonnull OWLEntity propertyEntity,
	// 		@Nonnull OWLOntology ontology,
	// 		@Nullable AnnotationConfig annotationConfig,
	// 		boolean isDataProperty) {
	// 	if (annotationConfig == null || schema == null) return;

	// 	final var propertyAnnotationConfig = annotationConfig.getPropertyAnnotations();

	// 	if (propertyAnnotationConfig == null) return;

	// 	final var propertyName = propertyEntity.getIRI().getShortForm();
	// 	final Set<OWLAnnotation> annotations =
	// 			EntitySearcher.getAnnotations(propertyEntity, ontology).collect(Collectors.toSet());

	// 	// Read-only
	// 	final var readOnlyFlag = propertyAnnotationConfig.getReadOnlyFlagName();
	// 	if (readOnlyFlag != null && !readOnlyFlag.isBlank()) {
	// 		boolean isReadOnly =
	// 				annotations.stream()
	// 						.anyMatch(a -> readOnlyFlag.equals(a.getProperty().getIRI().getShortForm()));
	// 		if (isReadOnly) {
	// 			if (isDataProperty) {
	// 				MapperDataProperty.setReadOnlyValueForPropertySchema(schema, true);
	// 			} else {
	// 				MapperObjectProperty.setReadOnlyValueForPropertySchema(schema, true);
	// 			}
	// 		}
	// 	}

	// 	// Write-only
	// 	final var writeOnlyFlag = propertyAnnotationConfig.getWriteOnlyFlagName();
	// 	if (writeOnlyFlag != null && !writeOnlyFlag.isBlank()) {
	// 		boolean isWriteOnly =
	// 				annotations.stream()
	// 						.anyMatch(a -> writeOnlyFlag.equals(a.getProperty().getIRI().getShortForm()));
	// 		if (isWriteOnly) {
	// 			if (isDataProperty) {
	// 				MapperDataProperty.setWriteOnlyValueForPropertySchema(schema, true);
	// 			} else {
	// 				MapperObjectProperty.setWriteOnlyValueForPropertySchema(schema, true);
	// 			}
	// 		}
	// 	}

	// 	// Example value (only for data properties)
	// 	if (isDataProperty) {
	// 		final var exampleFlag = propertyAnnotationConfig.getExampleValueName();
	// 		if (exampleFlag != null && !exampleFlag.isBlank()) {
	// 			for (var a : annotations) {
	// 				if (exampleFlag.equals(a.getProperty().getIRI().getShortForm())) {
	// 					MapperDataProperty.setExampleValueForPropertySchema(schema, a);
	// 				}
	// 			}
	// 		}
	// 	}
	// }
}
