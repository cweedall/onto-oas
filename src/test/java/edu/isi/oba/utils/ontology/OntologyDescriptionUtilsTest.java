package edu.isi.oba.utils.ontology;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.isi.oba.BaseTest;
import edu.isi.oba.utils.constants.ObaConstants;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * Unit tests for {@link OntologyDescriptionUtils}. Covers all branches of getDescription and
 * extractDescriptions methods.
 */
public class OntologyDescriptionUtilsTest extends BaseTest {

	@Test
	void testPrivateConstructor() throws Exception {
		var constructor = OntologyDescriptionUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(
				java.lang.reflect.InvocationTargetException.class, () -> constructor.newInstance());
	}

	/** Should return empty when no annotations and default descriptions are disabled. */
	@Test
	public void shouldReturnEmpty_whenNoAnnotationsAndDefaultDisabled() {
		final var entity = mock(OWLEntity.class);
		final var ontology = mock(OWLOntology.class);
		final var manager = mock(OWLOntologyManager.class);
		final var dataFactory = mock(OWLDataFactory.class);

		when(ontology.getOWLOntologyManager()).thenReturn(manager);
		when(manager.getOWLDataFactory()).thenReturn(dataFactory);

		final var description = OntologyDescriptionUtils.getDescription(entity, ontology, false);
		assertTrue(description.isEmpty());
	}

	/** Should return default description when no annotations and default descriptions are enabled. */
	@Test
	public void shouldReturnDefaultDescription_whenNoAnnotationsPresent() {
		final var entity = mock(OWLEntity.class);
		final var ontology = mock(OWLOntology.class);
		final var manager = mock(OWLOntologyManager.class);
		final var dataFactory = mock(OWLDataFactory.class);

		when(ontology.getOWLOntologyManager()).thenReturn(manager);
		when(manager.getOWLDataFactory()).thenReturn(dataFactory);

		final var description = OntologyDescriptionUtils.getDescription(entity, ontology, true);
		assertEquals(ObaConstants.DEFAULT_DESCRIPTION, description.orElse(""));
	}

	/** Should use default language when languageTag is null. */
	@Test
	public void shouldUseDefaultLanguage_whenLanguageTagIsNull() {
		final var entity = mock(OWLEntity.class);
		final var ontology = mock(OWLOntology.class);
		final var manager = mock(OWLOntologyManager.class);
		final var dataFactory = mock(OWLDataFactory.class);

		when(ontology.getOWLOntologyManager()).thenReturn(manager);
		when(manager.getOWLDataFactory()).thenReturn(dataFactory);

		final var description = OntologyDescriptionUtils.getDescription(entity, ontology, true, null);
		assertEquals(ObaConstants.DEFAULT_DESCRIPTION, description.orElse(""));
	}

	/** Should use default language when languageTag is blank. */
	@Test
	public void shouldUseDefaultLanguage_whenLanguageTagIsBlank() {
		final var entity = mock(OWLEntity.class);
		final var ontology = mock(OWLOntology.class);
		final var manager = mock(OWLOntologyManager.class);
		final var dataFactory = mock(OWLDataFactory.class);

		when(ontology.getOWLOntologyManager()).thenReturn(manager);
		when(manager.getOWLDataFactory()).thenReturn(dataFactory);

		final var description = OntologyDescriptionUtils.getDescription(entity, ontology, true, "   ");
		assertEquals(ObaConstants.DEFAULT_DESCRIPTION, description.orElse(""));
	}

	/** Should skip annotation when literal is not present. */
	@Test
	public void shouldSkipAnnotation_whenLiteralIsNotPresent() {
		final var entity = mock(OWLEntity.class);
		final var ontology = mock(OWLOntology.class);
		final var manager = mock(OWLOntologyManager.class);
		final var dataFactory = mock(OWLDataFactory.class);
		final var annotation = mock(OWLAnnotation.class);
		final var value = mock(OWLAnnotationValue.class);
		final var property = mock(OWLAnnotationProperty.class);
		final var iri = IRI.create("http://example.com/label");

		when(dataFactory.getOWLAnnotationProperty(iri)).thenReturn(property);
		when(manager.getOWLDataFactory()).thenReturn(dataFactory);
		when(ontology.getOWLOntologyManager()).thenReturn(manager);
		when(annotation.getValue()).thenReturn(value);
		when(value.asLiteral()).thenReturn(Optional.empty());

		final var ontologyStream = Stream.of(ontology);

		try (MockedStatic<EntitySearcher> searcher = mockStatic(EntitySearcher.class)) {
			searcher
					.when(
							() ->
									EntitySearcher.getAnnotationObjects(eq(entity), eq(ontologyStream), eq(property)))
					.thenReturn(Stream.of(annotation));

			final var description = OntologyDescriptionUtils.getDescription(entity, ontology, false);
			assertTrue(description.isEmpty());
		}
	}

	/** Should use empty string as language key when literal language is null. */
	@Test
	public void shouldUseEmptyLangKey_whenLiteralLangIsNull() {
		try (MockedStatic<OntologyDescriptionUtils> utilsMock =
				mockStatic(OntologyDescriptionUtils.class, CALLS_REAL_METHODS)) {
			utilsMock
					.when(OntologyDescriptionUtils::getDescriptionProperties)
					.thenReturn(List.of("http://example.com/label"));

			final var entity = mock(OWLEntity.class);
			final var ontology = mock(OWLOntology.class);
			final var manager = mock(OWLOntologyManager.class);
			final var dataFactory = mock(OWLDataFactory.class);
			final var annotation = mock(OWLAnnotation.class);
			final var value = mock(OWLAnnotationValue.class);
			final var literal = mock(OWLLiteral.class);
			final var property = mock(OWLAnnotationProperty.class);
			final var iri = IRI.create("http://example.com/label");

			when(ontology.getOWLOntologyManager()).thenReturn(manager);
			when(manager.getOWLDataFactory()).thenReturn(dataFactory);
			when(dataFactory.getOWLAnnotationProperty(iri)).thenReturn(property);
			when(annotation.getValue()).thenReturn(value);
			when(value.asLiteral()).thenReturn(Optional.of(literal));
			when(literal.getLiteral()).thenReturn("Some description");
			when(literal.getLang()).thenReturn(null);

			utilsMock
					.when(
							() -> OntologyDescriptionUtils.getAnnotations(eq(entity), eq(ontology), eq(property)))
					.thenReturn(Stream.of(annotation));

			final var description = OntologyDescriptionUtils.getDescription(entity, ontology, false, "");
			assertTrue(description.isPresent(), "Expected description to be present");
			assertEquals("Some description", description.get());
		}
	}

	@Test
	public void shouldCoverLiteralPresentBranch_withRealObjects()
			throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();

		OWLClass owlClass = dataFactory.getOWLClass(IRI.create("http://example.com/Class"));
		OWLAnnotationProperty labelProperty =
				dataFactory.getOWLAnnotationProperty(IRI.create("http://example.com/label"));
		OWLAnnotation annotation =
				dataFactory.getOWLAnnotation(
						labelProperty, dataFactory.getOWLLiteral("Real description", "en"));

		OWLAxiom axiom = dataFactory.getOWLAnnotationAssertionAxiom(owlClass.getIRI(), annotation);
		manager.addAxiom(ontology, axiom);

		try (MockedStatic<OntologyDescriptionUtils> utilsMock =
				mockStatic(OntologyDescriptionUtils.class, CALLS_REAL_METHODS)) {

			utilsMock
					.when(OntologyDescriptionUtils::getDescriptionProperties)
					.thenReturn(List.of("http://example.com/label"));

			Optional<String> description =
					OntologyDescriptionUtils.getDescription(owlClass, ontology, false, "en");

			assertTrue(description.isPresent());
			assertEquals("Real description", description.get());
		}
	}

	@Test
	public void shouldReturnOriginalEntity_whenNoRangeOrAnnotationsPresent() {
		final var objProp = mock(OWLObjectProperty.class);
		final var ontology = mock(OWLOntology.class);
		final var manager = mock(OWLOntologyManager.class);
		final var dataFactory = mock(OWLDataFactory.class);

		when(ontology.getOWLOntologyManager()).thenReturn(manager);
		when(manager.getOWLDataFactory()).thenReturn(dataFactory);
		when(ontology.getObjectPropertyRangeAxioms(any())).thenReturn(Set.of());

		OWLEntity resolved =
				OntologyDescriptionUtils.getDescription(objProp, ontology, false)
						.map(desc -> objProp)
						.orElse(objProp);

		assertEquals(objProp, resolved);
	}
}
