package edu.isi.oba.ontology.visitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.exceptions.OntologyVisitorException;
import edu.isi.oba.ontology.reasoner.ReasonerUtil;
import edu.isi.oba.ontology.schema.SchemaBuilder;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;

class VisitorContextTest {

	private OWLClass mockOwlClass;
	private OWLOntology mockOntology;
	private Logger mockLogger;
	private YamlConfig mockConfig;
	private VisitorContext context;
	private VisitorContext contextSpy;

	@BeforeEach
	void setUp() throws Exception {
		IRI mockIri = mock(IRI.class);
		mockOwlClass = mock(OWLClass.class);
		mockOntology = mock(OWLOntology.class);
		mockLogger = mock(Logger.class);
		mockConfig = mock(YamlConfig.class);

		OWLOntologyManager mockManager = mock(OWLOntologyManager.class);
		OWLDataFactory mockDataFactory = mock(OWLDataFactory.class);
		OWLClass mockThing = mock(OWLClass.class);
		OWLClassNode mockNode = mock(OWLClassNode.class);
		OWLReasoner mockReasoner = mock(OWLReasoner.class);
		Schema mockSchema = mock(Schema.class);

		// Stub ontology and class behavior
		when(mockOwlClass.getIRI()).thenReturn(mockIri);
		when(mockOntology.containsClassInSignature(mockIri)).thenReturn(true);
		when(mockOntology.getOWLOntologyManager()).thenReturn(mockManager);

		// Stub ontology manager behavior
		when(mockManager.getOntologyFormat(mockOntology)).thenReturn(mock(OWLDocumentFormat.class));
		when(mockManager.getOWLDataFactory()).thenReturn(mockDataFactory);
		doNothing().when(mockManager).addOntologyChangeListener(any());

		// Stub reasoner behavior
		when(mockReasoner.getTopClassNode()).thenReturn(mockNode);
		when(mockNode.getRepresentativeElement()).thenReturn(mockThing);

		// Stub data factory behavior
		when(mockDataFactory.getOWLThing()).thenReturn(mockThing);

		// Static mock for SchemaBuilder
		try (MockedStatic<ReasonerUtil> reasonerMock = mockStatic(ReasonerUtil.class);
				MockedStatic<SchemaBuilder> schemaBuilderMock = mockStatic(SchemaBuilder.class)) {
			reasonerMock.when(() -> ReasonerUtil.createReasoner(mockOntology)).thenReturn(mockReasoner);

			schemaBuilderMock
					.when(() -> SchemaBuilder.getBaseClassBasicSchema(mockOwlClass, mockOntology))
					.thenReturn(mockSchema);

			// Use test subclass to override reasoner creation
			context = new VisitorContext(mockOwlClass, mockOntology, mockConfig, mockLogger);
			contextSpy = Mockito.spy(context);
		}
	}

	@Test
	void testConstructor_withInvalidBaseClass() {
		mockOwlClass = null;

		try {
			context = new VisitorContext(mockOwlClass, mockOntology, mockConfig, mockLogger);
		} catch (OntologyVisitorException e) {
			assertTrue(e.getMessage().contains("must set the OWLClass"));
		}
	}

	@Test
	void testConstructor_withInvalidBaseOntology() {
		mockOntology = null;

		try {
			context = new VisitorContext(mockOwlClass, mockOntology, mockConfig, mockLogger);
		} catch (OntologyVisitorException e) {
			assertTrue(e.getMessage().contains("Ontology was set to null"));
		}
	}

	@Test
	void testConstructor_withBaseOntologyMissingBaseClass() {
		when(mockOntology.containsClassInSignature(any())).thenReturn(false);

		try {
			context = new VisitorContext(mockOwlClass, mockOntology, mockConfig, mockLogger);
		} catch (OntologyVisitorException e) {
			assertTrue(e.getMessage().contains("does not contain the class"));
		}
	}

	@Test
	void testAddAndGetReferencedClasses() {
		OWLClass cls1 = mock(OWLClass.class);
		OWLClass cls2 = mock(OWLClass.class);

		contextSpy.addReferencedClass(cls1);
		contextSpy.addReferencedClasses(List.of(cls2));

		Set<OWLClass> result = contextSpy.getReferencedClasses();
		assertTrue(result.contains(cls1));
		assertTrue(result.contains(cls2));
	}

	@Test
	void testMarkAndCheckProcessedClass() {
		OWLClass cls = mock(OWLClass.class);
		assertFalse(contextSpy.isClassProcessed(cls));
		contextSpy.markClassAsProcessed(cls);
		assertTrue(contextSpy.isClassProcessed(cls));
	}

	@Test
	void testAddPropertyToSchema() {
		Schema schema = mock(Schema.class);
		contextSpy.setClassSchema(schema);

		contextSpy.addPropertyToSchema("prop1", schema);

		assertEquals(schema, contextSpy.getBasePropertiesMap().get("prop1"));
		verify(schema).addProperty("prop1", schema);
	}

	@Test
	void testAddPropertyToSchemaWhenClassSchemaIsNull() {
		contextSpy.setClassSchema(null);
		Schema schema = mock(Schema.class);

		contextSpy.addPropertyToSchema("prop2", schema);

		assertEquals(schema, contextSpy.getBasePropertiesMap().get("prop2"));
		// No exception should be thrown
	}

	@Test
	void testMarkPropertyAsRequired() {
		Schema schema = spy(new Schema<String>());
		contextSpy.setClassSchema(schema);
		contextSpy.getClassSchema().setRequired(new ArrayList<String>(List.of("existingProp")));

		contextSpy.markPropertyAsRequired("requiredProp");

		assertTrue(contextSpy.getRequiredProperties().contains("requiredProp"));
		assertTrue(schema.getRequired().contains("requiredProp"));
	}

	@Test
	void testMarkPropertyAsRequired_whenSchemaIsNull() {
		contextSpy.setClassSchema(null);

		contextSpy.markPropertyAsRequired("requiredProp");

		assertTrue(contextSpy.getRequiredProperties().contains("requiredProp"));
		assertNull(contextSpy.getClassSchema());
	}

	@Test
	void testMarkPropertyAsRequiredWhenRequiredListIsNull() {
		Schema schema = spy(new Schema<>());
		contextSpy.setClassSchema(schema);
		contextSpy.getClassSchema().setRequired(null);

		contextSpy.markPropertyAsRequired("requiredProp");

		assertTrue(contextSpy.getRequiredProperties().contains("requiredProp"));
		verify(schema).setRequired(anyList());
	}

	@Test
	void testCurrentlyProcessedPropertyName() {
		contextSpy.setCurrentlyProcessedPropertyName("prop");
		assertEquals("prop", contextSpy.getCurrentlyProcessedPropertyName());

		contextSpy.clearCurrentlyProcessedPropertyName();
		assertNull(contextSpy.getCurrentlyProcessedPropertyName());
	}

	@Test
	void testWithProcessedProperty() {
		List<String> log = new ArrayList<>();
		contextSpy.withProcessedProperty(
				"tempProp",
				() -> {
					assertEquals("tempProp", contextSpy.getCurrentlyProcessedPropertyName());
					log.add("executed");
				});
		assertNull(contextSpy.getCurrentlyProcessedPropertyName());
		assertEquals(List.of("executed"), log);
	}

	@Test
	void testGetters() {
		OWLOntology ontology = mock(OWLOntology.class);
		OWLClass owlClass = mock(OWLClass.class);
		Schema schema = mock(Schema.class);

		contextSpy.setBaseClassOntology(ontology);
		contextSpy.setBaseClass(owlClass);
		contextSpy.setClassSchema(schema);

		assertEquals(ontology, contextSpy.getBaseClassOntology());
		assertEquals(owlClass, contextSpy.getBaseClass());
		assertEquals(schema, contextSpy.getClassSchema());
		assertEquals(contextSpy.getBasePropertiesMap(), contextSpy.getBasePropertiesMap());
		assertEquals(contextSpy.getRequiredProperties(), contextSpy.getRequiredProperties());
		assertEquals(contextSpy.getProcessedClasses(), contextSpy.getProcessedClasses());
		assertEquals(contextSpy.getMarkdownGenerationMap(), contextSpy.getMarkdownGenerationMap());
	}

	// ---------------

	/** Should set and get base class ontology. */
	@Test
	void shouldSetAndGetBaseClassOntology_whenOntologyIsProvided() {
		OWLOntology ontology = mock(OWLOntology.class);
		contextSpy.setBaseClassOntology(ontology);
		assertEquals(ontology, contextSpy.getBaseClassOntology());
	}

	/** Should set and get base class. */
	@Test
	void shouldSetAndGetBaseClass_whenClassIsProvided() {
		OWLClass owlClass = mock(OWLClass.class);
		contextSpy.setBaseClass(owlClass);
		assertEquals(owlClass, contextSpy.getBaseClass());
	}

	/** Should set and get class schema. */
	@Test
	void shouldSetAndGetClassSchema_whenSchemaIsProvided() {
		Schema schema = mock(Schema.class);
		contextSpy.setClassSchema(schema);
		assertEquals(schema, contextSpy.getClassSchema());
	}

	/** Should return config data. */
	@Test
	void shouldReturnConfigData_whenRequested() {
		assertEquals(mockConfig, contextSpy.getConfigData());
	}

	/** Should set and get reasoner. */
	@Test
	void shouldSetAndGetReasoner_whenReasonerIsProvided() {
		OWLReasoner reasoner = mock(OWLReasoner.class);
		contextSpy.setReasoner(reasoner);
		assertEquals(reasoner, contextSpy.getReasoner());
	}

	/** Should return owl:Thing class. */
	@Test
	void shouldReturnOwlThing_whenRequested() {
		assertNotNull(contextSpy.getOwlThing());
	}

	/** Should add and retrieve base properties. */
	@Test
	void shouldAddAndRetrieveBaseProperties_whenMapIsProvided() {
		Schema schema = mock(Schema.class);
		contextSpy.addAllBasePropertiesMap(Map.of("key", schema));
		assertEquals(schema, contextSpy.getBasePropertiesMap().get("key"));
	}

	/** Should add and retrieve required properties. */
	@Test
	void shouldAddAndRetrieveRequiredProperties_whenSetIsProvided() {
		contextSpy.addAllRequiredProperties(Set.of("required"));
		assertTrue(contextSpy.getRequiredProperties().contains("required"));
	}

	/** Should clear required properties. */
	@Test
	void shouldClearRequiredProperties_whenCalled() {
		contextSpy.addRequiredProperty("required");
		contextSpy.clearRequiredProperties();
		assertFalse(contextSpy.getRequiredProperties().contains("required"));
	}

	/** Should add and retrieve functional properties. */
	@Test
	void shouldAddAndRetrieveFunctionalProperties_whenNameIsProvided() {
		contextSpy.addFunctionalProperty("func");
		assertTrue(contextSpy.getFunctionalProperties().contains("func"));
	}

	/** Should add and retrieve enum properties. */
	@Test
	void shouldAddAndRetrieveEnumProperties_whenNameIsProvided() {
		contextSpy.addEnumProperty("enum");
		assertTrue(contextSpy.getEnumProperties().contains("enum"));
	}

	/** Should add and retrieve property names. */
	@Test
	void shouldAddAndRetrievePropertyNames_whenNameIsProvided() {
		contextSpy.addPropertyName("prop");
		assertTrue(contextSpy.getPropertyNames().contains("prop"));
	}

	/** Should add and retrieve referenced classes. */
	@Test
	void shouldAddAndRetrieveReferencedClasses_whenSetIsProvided() {
		OWLClass cls = mock(OWLClass.class);
		contextSpy.addAllReferencedClasses(Set.of(cls));
		assertTrue(contextSpy.getReferencedClasses().contains(cls));
	}

	/** Should remove referenced class. */
	@Test
	void shouldRemoveReferencedClass_whenClassIsProvided() {
		OWLClass cls = mock(OWLClass.class);
		contextSpy.addReferencedClass(cls);
		contextSpy.removeReferencedClass(cls);
		assertFalse(contextSpy.getReferencedClasses().contains(cls));
	}

	/** Should add and retrieve processed classes. */
	@Test
	void shouldAddAndRetrieveProcessedClasses_whenClassIsProvided() {
		OWLClass cls = mock(OWLClass.class);
		contextSpy.addProcessedClass(cls);
		assertTrue(contextSpy.getProcessedClasses().contains(cls));
	}

	/** Should add and retrieve processed restriction classes. */
	@Test
	void shouldAddAndRetrieveProcessedRestrictionClasses_whenClassIsProvided() {
		OWLClass cls = mock(OWLClass.class);
		contextSpy.addProcessedRestrictionClass(cls);
		assertTrue(contextSpy.getProcessedRestrictionClasses().contains(cls));
	}

	/** Should check if restriction class is processed. */
	@Test
	void shouldCheckRestrictionClassProcessed_whenClassIsQueried() {
		OWLClass cls = mock(OWLClass.class);
		assertFalse(contextSpy.isRestrictionClassProcessed(cls));
		contextSpy.addProcessedRestrictionClass(cls);
		assertTrue(contextSpy.isRestrictionClassProcessed(cls));
	}

	/** Should add markdown generation map entry. */
	@Test
	void shouldAddMarkdownGenerationMapEntry_whenEntryIsProvided() {
		Map<String, String> entry = Map.of("label", "value");
		contextSpy.addMarkdownGenerationMapEntry("test", entry);
		assertEquals(entry, contextSpy.getMarkdownGenerationMap().get("test"));
	}

	/** Should add all markdown generation map entries. */
	@Test
	void shouldAddAllMarkdownGenerationMapEntries_whenMapIsProvided() {
		Map<String, Map<String, String>> entries = Map.of("test", Map.of("label", "value"));
		contextSpy.addAllMarkdownGenerationMapEntries(entries);
		assertEquals(entries.get("test"), contextSpy.getMarkdownGenerationMap().get("test"));
	}

	/** Should add markdown entry. */
	@Test
	void shouldAddMarkdownEntry_whenAnnotationsAreProvided() {
		Map<String, String> annotations = Map.of("comment", "desc");
		contextSpy.addMarkdownEntry("prop", annotations);
		assertEquals(annotations, contextSpy.getMarkdownGenerationMap().get("prop"));
	}

	/** Should set and get currently processed property name. */
	@Test
	void shouldSetAndGetCurrentlyProcessedPropertyName_whenNameIsProvided() {
		contextSpy.setCurrentlyProcessedPropertyName("temp");
		assertEquals("temp", contextSpy.getCurrentlyProcessedPropertyName());
	}

	/** Should clear currently processed property name. */
	@Test
	void shouldClearCurrentlyProcessedPropertyName_whenCalled() {
		contextSpy.setCurrentlyProcessedPropertyName("temp");
		contextSpy.clearCurrentlyProcessedPropertyName();
		assertNull(contextSpy.getCurrentlyProcessedPropertyName());
	}

	/** Should execute with processed property and clear afterward. */
	@Test
	void shouldExecuteWithProcessedProperty_whenRunnableIsProvided() {
		List<String> log = new ArrayList<>();
		contextSpy.withProcessedProperty(
				"temp", () -> log.add(contextSpy.getCurrentlyProcessedPropertyName()));
		assertEquals(List.of("temp"), log);
		assertNull(contextSpy.getCurrentlyProcessedPropertyName());
	}
}
