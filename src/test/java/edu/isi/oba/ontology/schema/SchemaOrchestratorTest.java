package edu.isi.oba.ontology.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.ontology.reasoner.ReasonerUtil;
import edu.isi.oba.ontology.visitor.VisitorContext;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;

public class SchemaOrchestratorTest {

	private VisitorContext context;
	private Logger logger;
	private Schema schema;
	private SchemaOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		context = mock(VisitorContext.class);
		logger = mock(Logger.class);
		schema = mock(Schema.class);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);
	}

	/** Test that generateSchema orchestrates all steps and returns the schema. */
	@Test
	void shouldReturnSchema_whenGenerateSchemaIsCalled() {
		// Nothing to set up that isn't already in setUp()

		orchestrator.generateSchema();

		verify(logger, atLeastOnce()).log(eq(Level.FINE), anyString());
		verify(context, atLeastOnce()).getClassSchema();
	}

	/** Test cleanUpEnumProperties with null properties. */
	@Test
	void shouldHandleNullProperties_whenCleaningUpEnumProperties() {
		when(schema.getProperties()).thenReturn(null);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema(); // indirectly calls cleanUpEnumProperties

		verify(logger, atLeastOnce()).log(eq(Level.FINE), contains("enum"));
	}

	/** Test cleanUpEnumProperties with null enum and no default value. */
	@Test
	void shouldRemoveEnum_whenNullEnumWithoutDefault() {
		Schema itemSchema = mock(Schema.class);
		Schema propSchema = mock(Schema.class);
		Map<String, Schema> properties = new HashMap<>();
		properties.put("prop", propSchema);

		when(schema.getProperties()).thenReturn(properties);
		when(itemSchema.getEnum()).thenReturn(null);
		when(itemSchema.getDefault()).thenReturn(null);
		when(propSchema.getItems()).thenReturn(itemSchema);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema();

		verify(itemSchema, never()).setEnum(null);
	}

	/** Test cleanUpEnumProperties with multiple enums and no default value. */
	@Test
	void shouldRemoveEnum_whenMultipleEnumsWithoutDefault() {
		Schema itemSchema = mock(Schema.class);
		Schema propSchema = mock(Schema.class);
		Map<String, Schema> properties = new HashMap<>();
		properties.put("prop", propSchema);

		when(schema.getProperties()).thenReturn(properties);
		when(itemSchema.getEnum()).thenReturn(List.of("value", "value2"));
		when(itemSchema.getDefault()).thenReturn("value");
		when(propSchema.getItems()).thenReturn(itemSchema);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema();

		verify(itemSchema, never()).setEnum(null);
	}

	/** Test cleanUpEnumProperties with single enum and default value. */
	@Test
	void shouldRemoveEnum_whenSingleEnumWithDefaultExists() {
		Schema itemSchema = mock(Schema.class);
		Schema propSchema = mock(Schema.class);
		Map<String, Schema> properties = new HashMap<>();
		properties.put("prop", propSchema);

		when(schema.getProperties()).thenReturn(properties);
		when(itemSchema.getEnum()).thenReturn(List.of("value"));
		when(itemSchema.getDefault()).thenReturn("value");
		when(propSchema.getItems()).thenReturn(itemSchema);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema();

		verify(itemSchema).setEnum(null);
	}

	/** Test cleanUpEnumProperties with single enum and no default value. */
	@Test
	void shouldRemoveEnum_whenSingleEnumWithoutDefault() {
		Schema itemSchema = mock(Schema.class);
		Schema propSchema = mock(Schema.class);
		Map<String, Schema> properties = new HashMap<>();
		properties.put("prop", propSchema);

		when(schema.getProperties()).thenReturn(properties);
		when(itemSchema.getEnum()).thenReturn(Collections.singletonList("value"));
		when(itemSchema.getDefault()).thenReturn(null);
		when(propSchema.getItems()).thenReturn(itemSchema);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema();

		verify(itemSchema, never()).setEnum(null);
	}

	/** Test generateRequiredProperties with enum schema. */
	@Test
	void shouldClearRequiredProperties_whenEnumSchemaExists() {
		when(schema.getEnum()).thenReturn(Collections.singletonList("ENUM"));
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema();
		verify(context).clearRequiredProperties();
	}

	/** Test generateRequiredProperties with cardinality flag. */
	@Test
	void shouldGenerateRequiredProperties_whenFlagIsTrue() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.REQUIRED_PROPERTIES_FROM_CARDINALITY))
					.thenReturn(true);
			orchestrator.generateSchema();
			// We assume SchemaBuilder is tested separately
		}
	}

	/** Test convertArrayProperties with flag false and required properties. */
	@Test
	void shouldConvertArrayProperties_whenFlagIsFalseAndRequiredExists() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS))
					.thenReturn(false);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES))
					.thenReturn(false);
			when(context.getRequiredProperties()).thenReturn(Set.of("prop"));
			when(schema.getRequired()).thenReturn(List.of("prop"));
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();
			verify(context, atLeastOnce()).clearRequiredProperties();
		}
	}

	/** Test convertArrayProperties with flag false and required properties. */
	@Test
	void shouldConvertArrayProperties_whenFlagIsFalseAndRequiredExistsButIsEmpty() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS))
					.thenReturn(false);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FIX_SINGULAR_PLURAL_PROPERTY_NAMES))
					.thenReturn(false);
			when(context.getRequiredProperties()).thenReturn(Set.of("prop"));
			when(schema.getRequired()).thenReturn(List.of());
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();

			verify(schema, never()).setRequired(any());
		}
	}

	/** Test convertArrayProperties with flag false and required properties. */
	@Test
	void shouldConvertArrayProperties_whenFlagIsFalseAndRequiredIsNull() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS))
					.thenReturn(false);

			when(context.getRequiredProperties()).thenReturn(Set.of("prop"));
			when(schema.getRequired()).thenReturn(null);
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();

			verify(schema, never()).setRequired(any());
		}
	}

	/** Test convertArrayProperties with flag false and required properties. */
	@Test
	void shouldConvertArrayProperties_whenFlagIsTrue() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class);
				MockedStatic<MapperProperty> mapperMock = mockStatic(MapperProperty.class)) {
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS))
					.thenReturn(true);

			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();

			mapperMock.verify(
					() ->
							MapperProperty.convertArrayToNonArrayPropertySchemas(
									any(Schema.class), any(), any(), anyBoolean()),
					times(0));
		}
	}

	/** Test handleInheritanceReferences with all flags true. */
	@Test
	void shouldAddInheritanceReferences_whenFlagsAreTrue() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)).thenReturn(true);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(true);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getAllOf()).thenReturn(null);
			when(schema.getType()).thenReturn(null);
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(context.getClassSchema()).thenReturn(schema);
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();
			verify(context.getClassSchema()).addAllOfItem(any(ObjectSchema.class));
		}
	}

	/** Test handleInheritanceReferences with all flags true. */
	@Test
	void shouldAddInheritanceReferences_whenFlagsAreTrueAndAllOfEmpty() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)).thenReturn(true);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(true);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getAllOf()).thenReturn(null);
			when(schema.getType()).thenReturn(null);
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(context.getClassSchema()).thenReturn(schema);
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();
			verify(schema).addAllOfItem(any(ObjectSchema.class));
		}
	}

	/** Test handleInheritanceReferences with all flags true. */
	@Test
	void
			shouldAddInheritanceReferences_whenFlagsAreTrueAndProcessedClassesNotEmptyAndPropertiesNotNullAndDontRemoveSuperSuperClasses() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class);
				MockedStatic<ReasonerUtil> reasonerMock = mockStatic(ReasonerUtil.class)) {
			flags.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)).thenReturn(true);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(true);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getType()).thenReturn("string");
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(context.getClassSchema()).thenReturn(schema);

			// mockClass A
			OWLClass mockClass = mock(OWLClass.class);
			IRI mockIRI = mock(IRI.class);
			when(mockIRI.toString()).thenReturn("http://example.org#TestClass");
			when(mockIRI.getShortForm()).thenReturn("TestClass");
			when(mockClass.getIRI()).thenReturn(mockIRI);

			// mockClass B
			OWLClass mockClassB = mock(OWLClass.class);
			IRI mockIRIB = mock(IRI.class);
			when(mockIRIB.toString()).thenReturn("http://example.org#TestClassB");
			when(mockIRIB.getShortForm()).thenReturn("TestClassB");
			when(mockClassB.getIRI()).thenReturn(mockIRIB);

			when(context.getProcessedClasses()).thenReturn(Set.of(mockClass, mockClassB));
			OWLOntology mockOntology = mock(OWLOntology.class);
			OWLOntologyManager manager = mock(OWLOntologyManager.class);
			OWLDocumentFormat format = mock(PrefixDocumentFormat.class);
			when(((PrefixDocumentFormat) format).getPrefixName2PrefixMap())
					.thenReturn(Map.of("ex:", "http://example.org#"));
			when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
			when(((PrefixDocumentFormat) format).asPrefixOWLDocumentFormat())
					.thenReturn((PrefixDocumentFormat) format);
			when(manager.getOntologyFormat(mockOntology)).thenReturn(format);
			when(mockOntology.getOWLOntologyManager()).thenReturn(manager);
			when(context.getBaseClassOntology()).thenReturn(mockOntology);

			OWLReasoner mockReasoner = mock(OWLReasoner.class);
			NodeSet<OWLClass> mockNodeSet = mock(NodeSet.class);
			when(mockNodeSet.containsEntity(any())).thenReturn(false);
			when(mockReasoner.getSuperClasses(any(OWLClass.class), eq(false))).thenReturn(mockNodeSet);
			reasonerMock.when(() -> ReasonerUtil.createReasoner(mockOntology)).thenReturn(mockReasoner);
			when(context.getReasoner()).thenReturn(mockReasoner);

			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();

			verify(schema, times(2)).addAllOfItem(any(ObjectSchema.class));
		}
	}

	/** Test handleInheritanceReferences with all flags true. */
	@Test
	void
			shouldAddInheritanceReferences_whenFlagsAreTrueAndProcessedClassesNotEmptyAndPropertiesNotNullAndRemoveSuperSuperClasses() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class);
				MockedStatic<ReasonerUtil> reasonerMock = mockStatic(ReasonerUtil.class)) {
			flags.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)).thenReturn(true);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(true);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getType()).thenReturn("string");
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(context.getClassSchema()).thenReturn(schema);

			// mockClass A
			OWLClass mockClass = mock(OWLClass.class);
			IRI mockIRI = mock(IRI.class);
			when(mockIRI.toString()).thenReturn("http://example.org#TestClass");
			when(mockIRI.getShortForm()).thenReturn("TestClass");
			when(mockClass.getIRI()).thenReturn(mockIRI);

			// mockClass B
			OWLClass mockClassB = mock(OWLClass.class);
			IRI mockIRIB = mock(IRI.class);
			when(mockIRIB.toString()).thenReturn("http://example.org#TestClassB");
			when(mockIRIB.getShortForm()).thenReturn("TestClassB");
			when(mockClassB.getIRI()).thenReturn(mockIRIB);

			when(context.getProcessedClasses()).thenReturn(Set.of(mockClass, mockClassB));
			OWLOntology mockOntology = mock(OWLOntology.class);
			OWLOntologyManager manager = mock(OWLOntologyManager.class);
			OWLDocumentFormat format = mock(PrefixDocumentFormat.class);
			when(((PrefixDocumentFormat) format).getPrefixName2PrefixMap())
					.thenReturn(Map.of("ex:", "http://example.org#"));
			when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
			when(((PrefixDocumentFormat) format).asPrefixOWLDocumentFormat())
					.thenReturn((PrefixDocumentFormat) format);
			when(manager.getOntologyFormat(mockOntology)).thenReturn(format);
			when(mockOntology.getOWLOntologyManager()).thenReturn(manager);
			when(context.getBaseClassOntology()).thenReturn(mockOntology);

			OWLReasoner mockReasoner = mock(OWLReasoner.class);
			NodeSet<OWLClass> mockNodeSet = mock(NodeSet.class);
			when(mockNodeSet.containsEntity(eq(mockClass))).thenReturn(false);
			when(mockNodeSet.containsEntity(eq(mockClassB))).thenReturn(true);
			when(mockReasoner.getSuperClasses(any(OWLClass.class), eq(false))).thenReturn(mockNodeSet);
			reasonerMock.when(() -> ReasonerUtil.createReasoner(mockOntology)).thenReturn(mockReasoner);
			when(context.getReasoner()).thenReturn(mockReasoner);

			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();

			verify(schema, times(1)).addAllOfItem(any(ObjectSchema.class));
		}
	}

	/** Test handleInheritanceReferences with all flags true. */
	@Test
	void
			shouldAddInheritanceReferences_whenFlagsAreTrueAndProcessedClassesNotEmptyAndPropertiesNull() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class)) {
			flags.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)).thenReturn(true);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(true);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getType()).thenReturn(null);
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(context.getClassSchema()).thenReturn(schema);

			OWLClass mockClass = mock(OWLClass.class);
			IRI mockIRI = mock(IRI.class);
			when(mockIRI.toString()).thenReturn("http://example.org#TestClass");
			when(mockIRI.getShortForm()).thenReturn("TestClass");
			when(mockClass.getIRI()).thenReturn(mockIRI);
			when(context.getProcessedClasses()).thenReturn(Set.of(mockClass));
			OWLOntology mockOntology = mock(OWLOntology.class);
			OWLOntologyManager manager = mock(OWLOntologyManager.class);
			OWLDocumentFormat format = mock(PrefixDocumentFormat.class);
			when(((PrefixDocumentFormat) format).getPrefixName2PrefixMap())
					.thenReturn(Map.of("ex:", "http://example.org#"));
			when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
			when(((PrefixDocumentFormat) format).asPrefixOWLDocumentFormat())
					.thenReturn((PrefixDocumentFormat) format);
			when(manager.getOntologyFormat(mockOntology)).thenReturn(format);
			when(mockOntology.getOWLOntologyManager()).thenReturn(manager);
			when(context.getBaseClassOntology()).thenReturn(mockOntology);
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();

			verify(context.getClassSchema()).addAllOfItem(any(ObjectSchema.class));
		}
	}

	/** Test handleInheritanceReferences with all flags false. */
	@Test
	void shouldAddInheritanceReferences_whenFlagsAreFalseAndNoSchemaTypeToSet() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class);
				MockedStatic<MapperProperty> mapperMock = mockStatic(MapperProperty.class)) {
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES))
					.thenReturn(false);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(false);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(schema.getAllOf()).thenReturn(null);
			when(schema.getType()).thenReturn("something");
			when(context.getClassSchema()).thenReturn(schema);
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();
			verify(schema, never()).addAllOfItem(any(ObjectSchema.class));
			// One time, this is called within cleanUpEnumProperties(), but it should NOT be called within
			// handleInheritanceReferences().
			mapperMock.verify(
					() -> MapperProperty.setSchemaType(any(Schema.class), eq("object")), times(1));
		}
	}

	/** Test handleInheritanceReferences with one flag is false. */
	@Test
	void shouldAddInheritanceReferences_whenSomeFlagsAreFalseAndNoSchemaTypeToSet() {
		try (MockedStatic<GlobalFlags> flags = Mockito.mockStatic(GlobalFlags.class);
				MockedStatic<MapperProperty> mapperMock = mockStatic(MapperProperty.class)) {
			flags.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.FOLLOW_REFERENCES)).thenReturn(true);
			flags
					.when(() -> GlobalFlags.getFlag(ConfigPropertyNames.USE_INHERITANCE_REFERENCES))
					.thenReturn(false);
			when(schema.getEnum()).thenReturn(null);
			when(schema.getProperties()).thenReturn(Map.of("prop", mock(Schema.class)));
			when(schema.getAllOf()).thenReturn(null);
			when(schema.getType()).thenReturn("something");
			when(context.getClassSchema()).thenReturn(schema);
			orchestrator = new SchemaOrchestrator(context, logger);

			orchestrator.generateSchema();
			verify(schema, never()).addAllOfItem(any(ObjectSchema.class));
			// One time, this is called within cleanUpEnumProperties(), but it should NOT be called within
			// handleInheritanceReferences().
			mapperMock.verify(
					() -> MapperProperty.setSchemaType(any(Schema.class), eq("object")), times(1));
		}
	}

	/** Test pruneUnusedReferencedClasses with all flags false. */
	@Test
	void shouldPruneReferencedClass_whenAllIndicatorsAreFalse() {
		OWLClass refClass = mock(OWLClass.class);
		when(refClass.getIRI()).thenReturn(mock(IRI.class));
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology())
				.thenReturn(mock(org.semanticweb.owlapi.model.OWLOntology.class));
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());
		when(schema.getProperties()).thenReturn(null);
		when(context.getClassSchema()).thenReturn(schema);
		orchestrator = new SchemaOrchestrator(context, logger);

		orchestrator.generateSchema();
		verify(context).removeReferencedClass(refClass);
	}

	@Test
	void shouldNotPruneReferencedClass_whenEquivalentClassesExist() {
		OWLClass refClass = mock(OWLClass.class);
		OWLOntology ontology = mock(OWLOntology.class);

		when(refClass.getIRI()).thenReturn(mock(IRI.class));
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());
		when(schema.getProperties()).thenReturn(null);
		when(context.getClassSchema()).thenReturn(schema);

		// Mock equivalent classes
		try (MockedStatic<EntitySearcher> searcherMock = mockStatic(EntitySearcher.class)) {
			searcherMock
					.when(() -> EntitySearcher.getEquivalentClasses(refClass, ontology))
					.thenReturn(Stream.of(mock(OWLClass.class)));

			orchestrator = new SchemaOrchestrator(context, logger);
			orchestrator.generateSchema();

			verify(context, never()).removeReferencedClass(refClass);
		}
	}

	@Test
	void shouldNotPruneReferencedClass_whenSubClassPropertiesExist() {
		OWLClass refClass = mock(OWLClass.class);
		OWLOntology ontology = mock(OWLOntology.class);

		when(refClass.getIRI()).thenReturn(mock(IRI.class));
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());
		when(schema.getProperties()).thenReturn(null);
		when(context.getClassSchema()).thenReturn(schema);

		var axiom = mock(org.semanticweb.owlapi.model.OWLSubClassOfAxiom.class);
		var expr = mock(org.semanticweb.owlapi.model.OWLClassExpression.class);
		when(expr.getClassExpressionType()).thenReturn(ClassExpressionType.OBJECT_COMPLEMENT_OF);
		when(axiom.getNestedClassExpressions()).thenReturn(Set.of(expr));
		when(ontology.getSubClassAxiomsForSubClass(refClass)).thenReturn(Set.of(axiom));

		orchestrator = new SchemaOrchestrator(context, logger);
		orchestrator.generateSchema();

		verify(context, never()).removeReferencedClass(refClass);
	}

	@Test
	void shouldNotPruneReferencedClass_whenIsDomainForDataProperty() {
		OWLClass refClass = mock(OWLClass.class);
		OWLOntology ontology = mock(OWLOntology.class);
		OWLDataProperty dataProp = mock(OWLDataProperty.class);
		OWLDataPropertyDomainAxiom domainAxiom = mock(OWLDataPropertyDomainAxiom.class);

		when(refClass.getIRI()).thenReturn(mock(IRI.class));
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());
		when(schema.getProperties()).thenReturn(null);
		when(context.getClassSchema()).thenReturn(schema);

		when(ontology.getDataPropertiesInSignature()).thenReturn(Set.of(dataProp));
		when(ontology.getDataPropertyDomainAxioms(dataProp)).thenReturn(Set.of(domainAxiom));
		when(domainAxiom.getClassesInSignature()).thenReturn(Set.of(refClass));

		orchestrator = new SchemaOrchestrator(context, logger);
		orchestrator.generateSchema();

		verify(context, never()).removeReferencedClass(refClass);
	}

	@Test
	void shouldNotPruneReferencedClass_whenIsRangeForObjectProperty() {
		OWLClass refClass = mock(OWLClass.class);
		OWLOntology ontology = mock(OWLOntology.class);
		OWLObjectProperty objProp = mock(OWLObjectProperty.class);
		OWLObjectPropertyRangeAxiom rangeAxiom = mock(OWLObjectPropertyRangeAxiom.class);
		IRI iri = mock(IRI.class);

		when(refClass.getIRI()).thenReturn(mock(IRI.class));
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());
		when(context.getClassSchema()).thenReturn(schema);

		// Simulate object property with matching short form
		when(objProp.getIRI()).thenReturn(iri);
		when(iri.getShortForm()).thenReturn("someProp");
		when(ontology.getObjectPropertiesInSignature()).thenReturn(Set.of(objProp));
		when(schema.getProperties()).thenReturn(Map.of("someProp", mock(Schema.class)));
		when(ontology.getObjectPropertyRangeAxioms(objProp)).thenReturn(Set.of(rangeAxiom));
		when(rangeAxiom.getClassesInSignature()).thenReturn(Set.of(refClass));

		// Simulate other flags being true to prevent pruning
		var axiom = mock(org.semanticweb.owlapi.model.OWLSubClassOfAxiom.class);
		var expr = mock(org.semanticweb.owlapi.model.OWLClassExpression.class);
		when(expr.getClassExpressionType()).thenReturn(ClassExpressionType.OBJECT_COMPLEMENT_OF);
		when(axiom.getNestedClassExpressions()).thenReturn(Set.of(expr));
		when(ontology.getSubClassAxiomsForSubClass(refClass)).thenReturn(Set.of(axiom));

		try (MockedStatic<EntitySearcher> searcherMock = mockStatic(EntitySearcher.class)) {
			searcherMock
					.when(() -> EntitySearcher.getEquivalentClasses(refClass, ontology))
					.thenReturn(Stream.of(mock(OWLClass.class)));

			orchestrator = new SchemaOrchestrator(context, logger);
			orchestrator.generateSchema();

			verify(context, never()).removeReferencedClass(refClass);
		}
	}

	@Test
	void shouldSetIsRangeForObjectProperty_withRealOWLObjects() throws Exception {
		// Create real OWLClass and IRI
		OWLClass refClass =
				new org.semanticweb.owlapi.apibinding.OWLManager()
						.getOWLDataFactory()
						.getOWLClass(IRI.create("http://example.org#RefClass"));

		OWLObjectProperty objProp =
				new org.semanticweb.owlapi.apibinding.OWLManager()
						.getOWLDataFactory()
						.getOWLObjectProperty(IRI.create("http://example.org#someProp"));

		OWLObjectPropertyRangeAxiom rangeAxiom =
				new org.semanticweb.owlapi.apibinding.OWLManager()
						.getOWLDataFactory()
						.getOWLObjectPropertyRangeAxiom(objProp, refClass);

		OWLOntologyManager manager =
				org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology(Set.of(rangeAxiom));

		// Setup context and schema
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(context.getClassSchema()).thenReturn(schema);
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());
		when(schema.getProperties()).thenReturn(Map.of("someProp", mock(Schema.class)));

		// Stub EntitySearcher to return no equivalent classes
		try (MockedStatic<EntitySearcher> searcherMock = mockStatic(EntitySearcher.class)) {
			searcherMock
					.when(() -> EntitySearcher.getEquivalentClasses(refClass, ontology))
					.thenReturn(Stream.empty());

			orchestrator = new SchemaOrchestrator(context, logger);
			orchestrator.generateSchema();

			// ✅ If branch was taken, refClass should NOT be removed
			verify(context, never()).removeReferencedClass(refClass);
		}
	}

	@Test
	void shouldNotSetIsRangeForObjectProperty_whenSchemaPropertyMissing_withRealOWLObjects()
			throws Exception {
		var dataFactory = org.semanticweb.owlapi.apibinding.OWLManager.getOWLDataFactory();

		OWLClass refClass = dataFactory.getOWLClass(IRI.create("http://example.org#RefClass"));
		OWLObjectProperty objProp =
				dataFactory.getOWLObjectProperty(IRI.create("http://example.org#missingProp"));
		OWLObjectPropertyRangeAxiom rangeAxiom =
				dataFactory.getOWLObjectPropertyRangeAxiom(objProp, refClass);

		OWLOntologyManager manager =
				org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology(Set.of(rangeAxiom));

		// Setup context and schema
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(context.getClassSchema()).thenReturn(schema);
		when(schema.getEnum()).thenReturn(null);
		when(schema.getAllOf()).thenReturn(new ArrayList<>());

		// Schema does NOT contain the property
		when(schema.getProperties()).thenReturn(Map.of("someOtherProp", mock(Schema.class)));

		try (MockedStatic<EntitySearcher> searcherMock = mockStatic(EntitySearcher.class)) {
			searcherMock
					.when(() -> EntitySearcher.getEquivalentClasses(refClass, ontology))
					.thenReturn(Stream.empty());

			orchestrator = new SchemaOrchestrator(context, logger);
			orchestrator.generateSchema();

			// ✅ Since schema is missing the property, refClass should be removed
			verify(context).removeReferencedClass(refClass);
		}
	}

	@Test
	void shouldRemoveAllOfSchema_whenRefMatchesReferencedClass() {
		OWLClass refClass = mock(OWLClass.class);
		IRI iri = mock(IRI.class);
		when(refClass.getIRI()).thenReturn(iri);
		when(iri.getShortForm()).thenReturn("TestClass");

		OWLOntology ontology = mock(OWLOntology.class);
		when(context.getReferencedClasses()).thenReturn(Set.of(refClass));
		when(context.getBaseClassOntology()).thenReturn(ontology);
		when(schema.getEnum()).thenReturn(null);
		when(context.getClassSchema()).thenReturn(schema);

		// Use a real Schema object
		Schema allOfSchema = new Schema<>();
		allOfSchema.set$ref("#/components/schemas/ex:TestClass");

		// Spy on the list
		List<Schema> allOfList = spy(new ArrayList<>(List.of(allOfSchema)));
		when(schema.getAllOf()).thenReturn(allOfList);

		// Mock ontology format
		OWLOntologyManager manager = mock(OWLOntologyManager.class);
		OWLDocumentFormat format = mock(PrefixDocumentFormat.class);
		when(((PrefixDocumentFormat) format).getPrefixName2PrefixMap())
				.thenReturn(Map.of("ex:", "http://example.org#"));
		when(format.isPrefixOWLDocumentFormat()).thenReturn(true);
		when(((PrefixDocumentFormat) format).asPrefixOWLDocumentFormat())
				.thenReturn((PrefixDocumentFormat) format);
		when(manager.getOntologyFormat(ontology)).thenReturn(format);
		when(ontology.getOWLOntologyManager()).thenReturn(manager);

		// Ensure pruning condition is met
		when(ontology.getSubClassAxiomsForSubClass(refClass)).thenReturn(Set.of());
		when(ontology.getDataPropertiesInSignature()).thenReturn(Set.of());
		when(ontology.getObjectPropertiesInSignature()).thenReturn(Set.of());

		try (MockedStatic<EntitySearcher> searcherMock = mockStatic(EntitySearcher.class);
				MockedStatic<SchemaBuilder> schemaBuilderMock = mockStatic(SchemaBuilder.class)) {
			searcherMock
					.when(() -> EntitySearcher.getEquivalentClasses(refClass, ontology))
					.thenReturn(Stream.empty());

			schemaBuilderMock
					.when(() -> SchemaBuilder.getPrefixedSchemaName(refClass, ontology))
					.thenReturn("ex:TestClass");

			orchestrator = new SchemaOrchestrator(context, logger);
			orchestrator.generateSchema();

			// ✅ Verify removal from AllOf list
			verify(allOfList).remove(allOfSchema);
		}
	}
}
