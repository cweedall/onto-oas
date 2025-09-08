package edu.isi.oba.utils.ontology;

import edu.isi.oba.utils.constants.ObaConstants;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;

/** Utility class for extracting and resolving descriptions from OWL entities. */
public class OntologyDescriptionUtils {

	private OntologyDescriptionUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	static Stream<OWLAnnotation> getAnnotations(
			OWLEntity entity, OWLOntology ontology, OWLAnnotationProperty property) {
		return EntitySearcher.getAnnotationObjects(entity, Stream.of(ontology), property);
	}

	/**
	 * Returns the best description for an OWL entity in the specified language.
	 *
	 * @param entity the OWL entity
	 * @param ontology the ontology to search
	 * @param hasDefaultDescriptions whether to apply default descriptions
	 * @param languageTag the language tag
	 * @return an optional description string
	 */
	public static Optional<String> getDescription(
			OWLEntity entity, OWLOntology ontology, Boolean hasDefaultDescriptions, String languageTag) {
		if (languageTag == null || languageTag.isBlank()) {
			languageTag = ObaConstants.DEFAULT_LANGUAGE;
		}

		final var resolvedEntity = resolveEntityForDescription(entity, ontology);
		final var langDescMap = extractDescriptions(resolvedEntity, ontology);

		return getLanguageSpecificDescription(langDescMap, languageTag)
				.or(() -> applyFallbackDescription(langDescMap, hasDefaultDescriptions));
	}

	/**
	 * Returns the best English description for an OWL entity.
	 *
	 * @param entity the OWL entity
	 * @param ontology the ontology to search
	 * @param hasDefaultDescriptions whether to apply default descriptions
	 * @return an optional description string @Overload
	 */
	public static Optional<String> getDescription(
			OWLEntity entity, OWLOntology ontology, Boolean hasDefaultDescriptions) {
		return getDescription(entity, ontology, hasDefaultDescriptions, "en");
	}

	private static boolean hasDescriptionAnnotations(OWLEntity entity, OWLOntology ontology) {
		return ObaConstants.DESCRIPTION_PROPERTIES.stream()
						.map(
								description ->
										ontology
												.getOWLOntologyManager()
												.getOWLDataFactory()
												.getOWLAnnotationProperty(IRI.create(description)))
						.mapToLong(
								prop ->
										EntitySearcher.getAnnotationObjects(entity, Set.of(ontology).stream(), prop)
												.count())
						.sum()
				> 0;
	}

	private static OWLEntity resolveObjectPropertyRange(
			OWLObjectProperty property, OWLOntology ontology) {
		for (final var objPropRange :
				ontology.getObjectPropertyRangeAxioms(property.asObjectPropertyExpression())) {
			if (objPropRange.getRange() instanceof OWLClass) {
				return objPropRange.getRange().asOWLClass();
			}
		}
		return property;
	}

	private static OWLEntity resolveEntityForDescription(OWLEntity entity, OWLOntology ontology) {
		if (!(entity instanceof OWLObjectProperty)) return entity;
		if (hasDescriptionAnnotations(entity, ontology)) return entity;
		return resolveObjectPropertyRange((OWLObjectProperty) entity, ontology);
	}

	static List<String> getDescriptionProperties() {
		return ObaConstants.DESCRIPTION_PROPERTIES;
	}

	private static void addLiteralIfPresent(
			OWLAnnotation annotationObj, Map<String, String> langDescMap, String lineSeparator) {

		annotationObj
				.getValue()
				.asLiteral()
				.ifPresent(
						literal -> {
							final var normalized = normalizeLineBreaks(literal.getLiteral(), lineSeparator);
							langDescMap.put(literal.getLang() == null ? "" : literal.getLang(), normalized);
						});
	}

	private static Map<String, String> extractDescriptions(OWLEntity entity, OWLOntology ontology) {
		final var dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
		final var langDescMap = new HashMap<String, String>();

		for (final var description : getDescriptionProperties()) {
			final var property = dataFactory.getOWLAnnotationProperty(IRI.create(description));
			final var annotationObjects =
					getAnnotations(entity, ontology, property).collect(Collectors.toSet());

			for (final var annotationObj : annotationObjects) {
				addLiteralIfPresent(annotationObj, langDescMap, ObaConstants.YAML_LINE_SEPARATOR);
			}
		}

		return langDescMap;
	}

	private static Optional<String> getLanguageSpecificDescription(
			Map<String, String> langDescMap, String languageTag) {
		return Optional.ofNullable(langDescMap.getOrDefault(languageTag, langDescMap.get("")));
	}

	private static Optional<String> applyFallbackDescription(
			Map<String, String> langDescMap, Boolean hasDefaultDescriptions) {
		return Boolean.FALSE.equals(hasDefaultDescriptions)
				? Optional.empty()
				: Optional.of(ObaConstants.DEFAULT_DESCRIPTION);
	}

	private static String normalizeLineBreaks(String input, String replacement) {
		final String tempChar = "\u001A";
		return input
				.replace("\r\n", tempChar)
				.replace("\r", tempChar)
				.replace("\n", tempChar)
				.replace(tempChar, replacement);
	}
}
