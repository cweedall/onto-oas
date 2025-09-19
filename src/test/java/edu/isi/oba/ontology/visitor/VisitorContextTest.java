package edu.isi.oba.ontology.visitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

class VisitorContextTest {

	private Logger mockLogger;
	private YamlConfig mockConfig;
	private VisitorContext context;

	@BeforeEach
	void setUp() {
		mockLogger = mock(Logger.class);
		mockConfig = mock(YamlConfig.class);
		context = new VisitorContext(mockLogger, mockConfig);
	}

	@Test
	void testAddAndGetReferencedClasses() {
		OWLClass cls1 = mock(OWLClass.class);
		OWLClass cls2 = mock(OWLClass.class);

		context.addReferencedClass(cls1);
		context.addReferencedClasses(List.of(cls2));

		Set<OWLClass> result = context.getReferencedClasses();
		assertTrue(result.contains(cls1));
		assertTrue(result.contains(cls2));
	}

	@Test
	void testMarkAndCheckProcessedClass() {
		OWLClass cls = mock(OWLClass.class);
		assertFalse(context.isClassProcessed(cls));
		context.markClassAsProcessed(cls);
		assertTrue(context.isClassProcessed(cls));
	}

	@Test
	void testAddPropertyToSchema() {
		Schema schema = mock(Schema.class);
		context.classSchema = schema;

		context.addPropertyToSchema("prop1", schema);

		assertEquals(schema, context.getBasePropertiesMap().get("prop1"));
		verify(schema).addProperty("prop1", schema);
	}

	@Test
	void testAddPropertyToSchemaWhenClassSchemaIsNull() {
		context.classSchema = null;
		Schema schema = mock(Schema.class);

		context.addPropertyToSchema("prop2", schema);

		assertEquals(schema, context.getBasePropertiesMap().get("prop2"));
		// No exception should be thrown
	}

	@Test
	void testMarkPropertyAsRequired() {
		Schema schema = spy(new Schema<String>());
		context.classSchema = schema;
		context.classSchema.setRequired(new ArrayList<String>(List.of("existingProp")));

		context.markPropertyAsRequired("requiredProp");

		assertTrue(context.getRequiredProperties().contains("requiredProp"));
		assertTrue(schema.getRequired().contains("requiredProp"));
	}

	@Test
	void testMarkPropertyAsRequired_whenSchemaIsNull() {
		context.classSchema = null;

		context.markPropertyAsRequired("requiredProp");

		assertTrue(context.getRequiredProperties().contains("requiredProp"));
		assertNull(context.classSchema);
	}

	@Test
	void testMarkPropertyAsRequiredWhenRequiredListIsNull() {
		// Schema schema = mock(Schema.class);
		// when(schema.getRequired()).thenReturn(null);
		// context.classSchema = schema;
		Schema schema = spy(new Schema<>());
		context.classSchema = schema;
		context.classSchema.setRequired(null);

		context.markPropertyAsRequired("requiredProp");

		assertTrue(context.getRequiredProperties().contains("requiredProp"));
		verify(schema).setRequired(anyList());
	}

	@Test
	void testCurrentlyProcessedPropertyName() {
		context.setCurrentlyProcessedPropertyName("prop");
		assertEquals("prop", context.currentlyProcessedPropertyName);

		context.clearCurrentlyProcessedPropertyName();
		assertNull(context.currentlyProcessedPropertyName);
	}

	@Test
	void testWithProcessedProperty() {
		List<String> log = new ArrayList<>();
		context.withProcessedProperty(
				"tempProp",
				() -> {
					assertEquals("tempProp", context.currentlyProcessedPropertyName);
					log.add("executed");
				});
		assertNull(context.currentlyProcessedPropertyName);
		assertEquals(List.of("executed"), log);
	}

	@Test
	void testGetters() {
		OWLOntology ontology = mock(OWLOntology.class);
		OWLClass owlClass = mock(OWLClass.class);
		Schema schema = mock(Schema.class);

		context.baseClassOntology = ontology;
		context.baseClass = owlClass;
		context.classSchema = schema;

		assertEquals(ontology, context.getBaseClassOntology());
		assertEquals(owlClass, context.getBaseClass());
		assertEquals(schema, context.getClassSchema());
		assertEquals(context.basePropertiesMap, context.getBasePropertiesMap());
		assertEquals(context.requiredProperties, context.getRequiredProperties());
		assertEquals(context.processedClasses, context.getProcessedClasses());
		assertEquals(context.markdownGenerationMap, context.getMarkdownGenerationMap());
	}
}
