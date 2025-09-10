package edu.isi.oba.ontology.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import edu.isi.oba.BaseTest;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.exceptions.InvalidOntologyFormatException;
import edu.isi.oba.utils.ontology.OntologyDescriptionUtils;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/** Unit tests for {@link SchemaBuilder}. */
public class SchemaBuilderTest extends BaseTest {

	private OWLClass owlClass;
	private OWLOntology ontology;
	private YamlConfig config;

	@BeforeEach
	void setUp() {
		owlClass = mock(OWLClass.class);
		ontology = mock(OWLOntology.class);
		config = mock(YamlConfig.class);
	}

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<SchemaBuilder> constructor = SchemaBuilder.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	void shouldReturnBasicSchema_whenBaseClassIsProvided() {
		try (MockedStatic<MapperProperty> mapperProperty = mockStatic(MapperProperty.class);
				MockedStatic<OntologyDescriptionUtils> descriptionUtils =
						mockStatic(OntologyDescriptionUtils.class);
				MockedStatic<GlobalFlags> globalFlags = mockStatic(GlobalFlags.class)) {

			IRI iri = mock(IRI.class);
			when(iri.toString()).thenReturn("http://example.org#TestClass");
			when(iri.getShortForm()).thenReturn("TestClass");
			when(owlClass.getIRI()).thenReturn(iri);

			OWLOntologyManager manager = mock(OWLOntologyManager.class);
			PrefixDocumentFormat format = mock(PrefixDocumentFormat.class);
			when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
			when(format.asPrefixOWLDocumentFormat()).thenReturn(format);
			when(format.getPrefixName2PrefixMap()).thenReturn(Map.of("ex:", "http://example.org#"));
			when(manager.getOntologyFormat(ontology)).thenReturn(format);
			when(ontology.getOWLOntologyManager()).thenReturn(manager);

			mapperProperty.when(() -> MapperProperty.setSchemaName(any(), any())).thenCallRealMethod();
			mapperProperty
					.when(() -> MapperProperty.setSchemaDescription(any(), any()))
					.thenCallRealMethod();
			mapperProperty.when(() -> MapperProperty.setSchemaType(any(), any())).thenCallRealMethod();

			descriptionUtils
					.when(() -> OntologyDescriptionUtils.getDescription(any(), any(), anyBoolean()))
					.thenReturn(Optional.of("description"));

			globalFlags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_PROPERTIES))
					.thenReturn(false);
			globalFlags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
					.thenReturn(true);

			Schema schema = SchemaBuilder.getBaseClassBasicSchema(owlClass, ontology, config);
			assertNotNull(schema);
		}
	}

	@Test
	void shouldThrowException_whenOntologyFormatIsInvalid() {
		OWLOntology mockOntology = mock(OWLOntology.class);
		OWLOntologyManager mockManager = mock(OWLOntologyManager.class);
		when(mockOntology.getOWLOntologyManager()).thenReturn(mockManager);
		when(mockManager.getOntologyFormat(mockOntology)).thenReturn(null);

		OWLClass mockClass = mock(OWLClass.class);
		IRI mockIRI = mock(IRI.class);
		when(mockIRI.toString()).thenReturn("http://example.org#TestClass");
		when(mockIRI.getShortForm()).thenReturn("TestClass");
		when(mockClass.getIRI()).thenReturn(mockIRI);

		assertThrows(
				InvalidOntologyFormatException.class,
				() -> {
					SchemaBuilder.getPrefixedSchemaName(mockClass, mockOntology);
				});
	}

	@Test
	void shouldReturnPrefixedSchemaName_whenOntologyHasPrefixFormat() {
		IRI iri = mock(IRI.class);
		when(iri.toString()).thenReturn("http://example.org#TestClass");
		when(iri.getShortForm()).thenReturn("TestClass");
		when(owlClass.getIRI()).thenReturn(iri);

		OWLOntologyManager manager = mock(OWLOntologyManager.class);
		OWLDocumentFormat format = mock(PrefixDocumentFormat.class);
		when(((PrefixDocumentFormat) format).getPrefixName2PrefixMap())
				.thenReturn(Map.of("ex:", "http://example.org#"));
		when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
		when(((PrefixDocumentFormat) format).asPrefixOWLDocumentFormat())
				.thenReturn((PrefixDocumentFormat) format);
		when(manager.getOntologyFormat(ontology)).thenReturn(format);
		when(ontology.getOWLOntologyManager()).thenReturn(manager);

		String name = SchemaBuilder.getPrefixedSchemaName(owlClass, ontology);
		assertEquals("ex-TestClass", name);
	}

	@Test
	void shouldThrowFatalError_whenOntologyFormatIsInvalid() {
		IRI iri = mock(IRI.class);
		when(iri.toString()).thenReturn("http://example.org#TestClass");
		when(iri.getShortForm()).thenReturn("TestClass");
		when(owlClass.getIRI()).thenReturn(iri);

		OWLOntologyManager manager = mock(OWLOntologyManager.class);
		when(manager.getOntologyFormat(ontology)).thenReturn(null);
		when(ontology.getOWLOntologyManager()).thenReturn(manager);

		assertThrows(
				InvalidOntologyFormatException.class,
				() -> {
					SchemaBuilder.getPrefixedSchemaName(owlClass, ontology);
				});
	}

	@Test
	void shouldReturnDefaultProperties_whenConfigIsProvided() {
		Map<String, Schema> properties = SchemaBuilder.getDefaultProperties(config);
		assertNotNull(properties);
		assertTrue(properties.containsKey("id"));
		assertTrue(properties.containsKey("label"));
		assertTrue(properties.containsKey("type"));
		assertTrue(properties.containsKey("description"));
		assertTrue(properties.containsKey("eventDateTime"));
		assertTrue(properties.containsKey("isBool"));
		assertTrue(properties.containsKey("quantity"));
	}

	@Test
	void shouldSetRequiredProperties_whenFunctionalPropertiesAreProvided() {
		Schema schema = new Schema<>();
		schema.setProperties(new HashMap<>());
		schema.addProperty("id", new Schema<>().minItems(1));
		schema.addProperty("label", new Schema<>());
		schema.addProperty("type", new Schema<>());

		Set<String> functional = Set.of("label");

		SchemaBuilder.generateRequiredPropertiesForClassSchemas(schema, functional);

		List<String> required = schema.getRequired();
		assertTrue(required.contains("id"));
		assertTrue(required.contains("label"));
		assertFalse(required.contains("type"));
	}

	@Test
	void shouldThrowException_whenFormatIsNotPrefixDocumentFormat() {
		IRI iri = mock(IRI.class);
		when(iri.toString()).thenReturn("http://example.org#TestClass");
		when(iri.getShortForm()).thenReturn("TestClass");
		when(owlClass.getIRI()).thenReturn(iri);

		OWLOntologyManager manager = mock(OWLOntologyManager.class);
		OWLDocumentFormat format = mock(OWLDocumentFormat.class);
		when(format.isPrefixOWLDocumentFormat()).thenReturn(false);
		when(manager.getOntologyFormat(ontology)).thenReturn(format);
		when(ontology.getOWLOntologyManager()).thenReturn(manager);

		assertThrows(
				InvalidOntologyFormatException.class,
				() -> {
					SchemaBuilder.getPrefixedSchemaName(owlClass, ontology);
				});
	}

	@Test
	void shouldReturnUnprefixedSchemaName_whenPrefixDoesNotMatch()
			throws InvalidOntologyFormatException {
		IRI iri = mock(IRI.class);
		when(iri.toString()).thenReturn("http://example.org#TestClass");
		when(iri.getShortForm()).thenReturn("TestClass");
		when(owlClass.getIRI()).thenReturn(iri);

		OWLOntologyManager manager = mock(OWLOntologyManager.class);
		PrefixDocumentFormat format = mock(PrefixDocumentFormat.class);
		when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
		when(format.asPrefixOWLDocumentFormat()).thenReturn(format);
		when(format.getPrefixName2PrefixMap()).thenReturn(Map.of("other:", "http://unrelated.org#"));
		when(manager.getOntologyFormat(ontology)).thenReturn(format);
		when(ontology.getOWLOntologyManager()).thenReturn(manager);

		String name = SchemaBuilder.getPrefixedSchemaName(owlClass, ontology);
		assertEquals("TestClass", name); // No prefix added
	}

	@Test
	void shouldHandleNullProperties_whenGeneratingRequiredProperties() {
		Schema schema = new Schema<>();
		schema.setProperties(null); // explicitly set to null

		Set<String> functional = Set.of("label");

		assertDoesNotThrow(
				() -> {
					SchemaBuilder.generateRequiredPropertiesForClassSchemas(schema, functional);
				});

		// It's valid for required to be null if no required properties were added
		assertNull(schema.getRequired());
	}

	@Test
	void shouldNotRequireProperty_whenMinItemsIsNullAndNotFunctional() {
		Schema schema = new Schema<>();
		schema.setProperties(new HashMap<>());
		schema.addProperty("optional", new Schema<>()); // no minItems

		Set<String> functional = Set.of(); // not functional

		SchemaBuilder.generateRequiredPropertiesForClassSchemas(schema, functional);

		assertNull(schema.getRequired()); // no required properties added
	}

	@Test
	void shouldNotRequireProperty_whenMinItemsIsZeroAndNotFunctional() {
		Schema schema = new Schema<>();
		schema.setProperties(new HashMap<>());
		Schema optionalSchema = new Schema<>();
		optionalSchema.setMinItems(0);
		schema.addProperty("optional", optionalSchema);

		Set<String> functional = Set.of(); // not functional

		SchemaBuilder.generateRequiredPropertiesForClassSchemas(schema, functional);

		assertNull(schema.getRequired()); // no required properties added
	}
}
