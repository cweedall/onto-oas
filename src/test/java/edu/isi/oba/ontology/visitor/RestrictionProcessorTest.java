package edu.isi.oba.ontology.visitor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.isi.oba.MapperDataProperty;
import edu.isi.oba.MapperObjectProperty;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.ontology.schema.SchemaBuilder;
import edu.isi.oba.utils.constants.ObaConstants;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.*;

public class RestrictionProcessorTest {

	private VisitorContext context;
	private Logger logger;
	private OWLObjectVisitor visitor;
	private RestrictionProcessor processor;
	private RestrictionProcessor processorSpy;

	@BeforeEach
	public void setUp() {
		context = mock(VisitorContext.class);
		logger = mock(Logger.class);
		visitor = mock(OWLObjectVisitor.class);
		processor = new RestrictionProcessor(context, logger, visitor);
		processorSpy = Mockito.spy(processor);

		Schema classSchema = new Schema();
		when(context.getClassSchema()).thenReturn(classSchema);
		context.setClassSchema(classSchema);
		Schema schema = new Schema();
		context.getClassSchema().addProperty("testProperty", schema);
		context.setCurrentlyProcessedPropertyName("testProperty");
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withObjectOneOf() {
		OWLObjectOneOf oneOf = mock(OWLObjectOneOf.class);
		OWLObjectSomeValuesFrom restriction = mock(OWLObjectSomeValuesFrom.class);
		when(restriction.getFiller()).thenReturn(oneOf);

		doCallRealMethod().when(processorSpy).getOrCreateSchema();
		doCallRealMethod().when(processorSpy).processQuantifiedObjectRestriction(restriction);

		processorSpy.processQuantifiedObjectRestriction(restriction);

		verify(processorSpy, times(1)).getOrCreateSchema();

		verify(oneOf, times(1)).accept(visitor);
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withNaryBooleanExpression() {
		try (MockedStatic<MapperObjectProperty> mockedMapperObjProp =
						mockStatic(MapperObjectProperty.class);
				MockedStatic<SchemaBuilder> mockedSchemaBuilder = mockStatic(SchemaBuilder.class); ) {
			OWLNaryBooleanClassExpression booleanCE = mock(OWLNaryBooleanClassExpression.class);
			OWLObjectSomeValuesFrom restriction = mock(OWLObjectSomeValuesFrom.class);
			when(restriction.getFiller()).thenReturn(booleanCE);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.getComplexObjectComposedSchema(booleanCE))
					.thenReturn(new ComposedSchema());

			mockedSchemaBuilder
					.when(() -> SchemaBuilder.getPrefixedSchemaName(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processQuantifiedObjectRestriction(any());
			doCallRealMethod().when(processorSpy).applyObjectRestriction(any(), any(), any());

			processorSpy.processQuantifiedObjectRestriction(restriction);

			verify(booleanCE, times(0)).accept(visitor);
			verify(processorSpy, times(1)).applyObjectRestriction(any(), any(), any());

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.getComplexObjectComposedSchema(booleanCE), times(1));

			mockedSchemaBuilder.verify(() -> SchemaBuilder.getPrefixedSchemaName(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withOtherExpressionType() {
		try (MockedStatic<MapperObjectProperty> mockedMapperObjProp =
						mockStatic(MapperObjectProperty.class);
				MockedStatic<SchemaBuilder> mockedSchemaBuilder = mockStatic(SchemaBuilder.class); ) {

			IRI classIRI = mock(IRI.class);
			OWLClass owlClass = mock(OWLClass.class);
			OWLClassExpression filler = mock(OWLClass.class);
			OWLObjectSomeValuesFrom restriction = mock(OWLObjectSomeValuesFrom.class);
			when(classIRI.getShortForm()).thenReturn("name");
			when(owlClass.getIRI()).thenReturn(classIRI);
			when(filler.asOWLClass()).thenReturn(owlClass);
			when(restriction.getFiller()).thenReturn(filler);

			OWLOntology owlOntology = mock(OWLOntology.class);
			OWLOntologyManager owlOntologyManager = mock(OWLOntologyManager.class);
			OWLDocumentFormat owlDocumentFormat = mock(OWLDocumentFormat.class);
			PrefixDocumentFormat prefixDocumentFormat = mock(PrefixDocumentFormat.class);
			when(prefixDocumentFormat.getPrefixName2PrefixMap()).thenReturn(Map.of("name", "prefix"));
			when(owlDocumentFormat.asPrefixOWLDocumentFormat()).thenReturn(prefixDocumentFormat);
			when(owlDocumentFormat.isPrefixOWLDocumentFormat()).thenReturn(true);
			when(owlOntologyManager.getOntologyFormat(owlOntology)).thenReturn(owlDocumentFormat);
			when(owlOntology.getOWLOntologyManager()).thenReturn(owlOntologyManager);
			when(context.getBaseClassOntology()).thenReturn(owlOntology);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.getComplexObjectComposedSchema(any()))
					.thenReturn(new ComposedSchema());

			mockedSchemaBuilder
					.when(() -> SchemaBuilder.getPrefixedSchemaName(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processQuantifiedObjectRestriction(any());
			doCallRealMethod().when(processorSpy).applyObjectRestriction(any(), any(), any());

			processorSpy.processQuantifiedObjectRestriction(restriction);

			verify(filler, times(0)).accept(visitor);
			verify(processorSpy, times(1)).applyObjectRestriction(any(), any(), any());

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.getComplexObjectComposedSchema(any()), times(0));

			mockedSchemaBuilder.verify(() -> SchemaBuilder.getPrefixedSchemaName(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessQuantifiedDataRestriction_withOneOfAndNullCardinality() {
		OWLDataOneOf oneOf = mock(OWLDataOneOf.class);
		OWLDataSomeValuesFrom restriction = mock(OWLDataSomeValuesFrom.class);
		when(restriction.getFiller()).thenReturn(oneOf);

		doCallRealMethod().when(processorSpy).getOrCreateSchema();
		doCallRealMethod().when(processorSpy).processQuantifiedDataRestriction(restriction);
		doNothing().when(processorSpy).applyDataRestriction(any(), any(), any(), any());

		processorSpy.processQuantifiedDataRestriction(restriction);

		verify(processorSpy, times(1)).getOrCreateSchema();
		verify(oneOf, times(1)).accept(visitor);
	}

	@Test
	public void testProcessQuantifiedDataRestriction_withNaryDataRange() {
		try (MockedStatic<MapperDataProperty> mockedMapperDataProp =
				mockStatic(MapperDataProperty.class); ) {

			Integer cardinality = 1;
			OWLNaryDataRange filler = mock(OWLNaryDataRange.class);
			OWLDataCardinalityRestriction restriction = mock(OWLDataCardinalityRestriction.class);
			when(restriction.getCardinality()).thenReturn(cardinality);
			when(restriction.getFiller()).thenReturn(filler);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperDataProp
					.when(() -> MapperDataProperty.getComplexDataComposedSchema(filler))
					.thenReturn(new ComposedSchema());

			mockedMapperDataProp
					.when(() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processQuantifiedDataRestriction(any());
			doNothing().when(processorSpy).applyDataRestriction(any(), any(), any(), any());

			processorSpy.processQuantifiedDataRestriction(restriction);

			verify(filler, times(0)).accept(visitor);
			verify(processorSpy, times(1)).applyDataRestriction(any(), any(), any(), any());

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(filler), times(1));

			mockedMapperDataProp.verify(
					() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessQuantifiedDataRestriction_withDatatypeRestriction() {
		try (MockedStatic<MapperDataProperty> mockedMapperDataProp =
				mockStatic(MapperDataProperty.class); ) {

			Integer cardinality = 1;
			IRI datatypeIRI = mock(IRI.class);
			OWLDatatype datatype = mock(OWLDatatype.class);
			OWLFacetRestriction facetRestriction = mock(OWLFacetRestriction.class);
			OWLDatatypeRestriction filler = mock(OWLDatatypeRestriction.class);
			OWLDataCardinalityRestriction restriction = mock(OWLDataCardinalityRestriction.class);

			when(datatypeIRI.getShortForm()).thenReturn("name");
			when(datatype.getIRI()).thenReturn(datatypeIRI);
			when(filler.getDatatype()).thenReturn(datatype);
			when(filler.getFacetRestrictions()).thenReturn(Set.of(facetRestriction));
			when(restriction.getCardinality()).thenReturn(cardinality);
			when(restriction.getFiller()).thenReturn(filler);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperDataProp
					.when(() -> MapperDataProperty.getComplexDataComposedSchema(any()))
					.thenReturn(new ComposedSchema());

			mockedMapperDataProp
					.when(() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processQuantifiedDataRestriction(any());
			doNothing().when(processorSpy).applyDataRestriction(any(), any(), any(), any());

			processorSpy.processQuantifiedDataRestriction(restriction);

			verify(filler, times(0)).accept(visitor);
			verify(processorSpy, times(1)).applyDataRestriction(any(), any(), any(), any());

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(any()), times(0));

			mockedMapperDataProp.verify(
					() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withOtherDataRangeType() {
		try (MockedStatic<MapperDataProperty> mockedMapperDataProp =
				mockStatic(MapperDataProperty.class); ) {

			Integer cardinality = 1;
			IRI datatypeIRI = mock(IRI.class);
			OWLDatatype datatype = mock(OWLDatatype.class);
			OWLDataRange filler = mock(OWLDataRange.class);
			OWLDataCardinalityRestriction restriction = mock(OWLDataCardinalityRestriction.class);

			when(datatypeIRI.getShortForm()).thenReturn("name");
			when(datatype.getIRI()).thenReturn(datatypeIRI);
			when(filler.asOWLDatatype()).thenReturn(datatype);
			when(restriction.getCardinality()).thenReturn(cardinality);
			when(restriction.getFiller()).thenReturn(filler);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperDataProp
					.when(() -> MapperDataProperty.getComplexDataComposedSchema(any()))
					.thenReturn(new ComposedSchema());

			mockedMapperDataProp
					.when(() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processQuantifiedDataRestriction(any());
			doNothing().when(processorSpy).applyDataRestriction(any(), any(), any(), any());

			processorSpy.processQuantifiedDataRestriction(restriction);

			verify(filler, times(0)).accept(visitor);
			verify(processorSpy, times(1)).applyDataRestriction(any(), any(), any(), any());

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(any()), times(0));

			mockedMapperDataProp.verify(
					() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessNaryBooleanClassExpression() {
		try (MockedStatic<MapperProperty> mockedMapperProp = mockStatic(MapperProperty.class);
				MockedStatic<MapperObjectProperty> mockedMapperObjProp =
						mockStatic(MapperObjectProperty.class); ) {
			OWLObjectUnionOf union = mock(OWLObjectUnionOf.class);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.getComplexObjectComposedSchema(union))
					.thenCallRealMethod();

			mockedMapperProp.when(() -> MapperProperty.setSchemaType(any(), any())).thenCallRealMethod();

			processor.processNaryBooleanClassExpression(union);

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.getComplexObjectComposedSchema(union), times(1));
			mockedMapperProp.verify(() -> MapperProperty.setSchemaType(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessNaryDataRange() {

		try (MockedStatic<MapperProperty> mockedMapperProp = mockStatic(MapperProperty.class);
				MockedStatic<MapperDataProperty> mockedMapperDataProp =
						mockStatic(MapperDataProperty.class); ) {
			OWLDataUnionOf union = mock(OWLDataUnionOf.class);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperDataProp
					.when(() -> MapperDataProperty.getComplexDataComposedSchema(union))
					.thenCallRealMethod();

			mockedMapperProp.when(() -> MapperProperty.setSchemaType(any(), any())).thenCallRealMethod();

			processor.processNaryDataRange(union);

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(union), times(1));
			mockedMapperProp.verify(() -> MapperProperty.setSchemaType(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessHasValue_forObjectWithNamedIndividual() {
		try (MockedStatic<MapperObjectProperty> mockedMapperObjProp =
				mockStatic(MapperObjectProperty.class); ) {
			OWLNamedIndividual individual = mock(OWLNamedIndividual.class);
			OWLObjectHasValue hasValue = mock(OWLObjectHasValue.class);
			when(hasValue.getFiller()).thenReturn(individual);
			when(individual.getIRI()).thenReturn(mock(IRI.class));

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			processor.processHasValue(hasValue);

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessHasValue_forObjectWithNonNamedIndividual() {
		try (MockedStatic<MapperObjectProperty> mockedMapperObjProp =
				mockStatic(MapperObjectProperty.class); ) {
			OWLIndividual individual = mock(OWLIndividual.class);
			OWLObjectHasValue hasValue = mock(OWLObjectHasValue.class);
			when(hasValue.getFiller()).thenReturn(individual);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processHasValue(hasValue);

			processorSpy.processHasValue(hasValue);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessHasValue_forDataWithLiteralHavingDatatype() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {

			OWLLiteral literal = mock(OWLLiteral.class);
			OWLDatatype datatype = mock(OWLDatatype.class);
			String stringDatatype = "string";
			when(MapperDataProperty.getDataType(datatype)).thenReturn(stringDatatype);
			when(literal.getDatatype()).thenReturn(datatype);

			OWLDataHasValue hasValue = mock(OWLDataHasValue.class);
			when(hasValue.getFiller()).thenReturn(literal);

			mapperDataProperty
					.when(() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processHasValue(hasValue);

			processorSpy.processHasValue(hasValue);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessHasValue_forDataWithLiteralWithoutDatatype() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {

			OWLLiteral literal = mock(OWLLiteral.class);
			when(literal.getDatatype()).thenReturn(null);

			OWLDataHasValue hasValue = mock(OWLDataHasValue.class);
			when(hasValue.getFiller()).thenReturn(literal);

			mapperDataProperty
					.when(() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processHasValue(hasValue);

			processorSpy.processHasValue(hasValue);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessOneOf_withObjectOneOf() {
		OWLObjectOneOf oneOf = mock(OWLObjectOneOf.class);
		OWLIndividual individual = mock(OWLIndividual.class);
		OWLNamedIndividual namedIndividual = mock(OWLNamedIndividual.class);
		when(namedIndividual.getIRI()).thenReturn(mock(IRI.class));
		when(individual.asOWLNamedIndividual()).thenReturn(namedIndividual);
		when(oneOf.getIndividuals()).thenReturn(Collections.singleton(individual));

		doCallRealMethod().when(processorSpy).getOrCreateSchema();
		doCallRealMethod().when(processorSpy).processOneOf(oneOf);

		processorSpy.processOneOf(oneOf);

		verify(processorSpy, times(1)).getOrCreateSchema();
	}

	@Test
	public void testProcessOneOf_withDataOneOfAndEmptyValues() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {

			OWLDataOneOf oneOf = mock(OWLDataOneOf.class);
			when(oneOf.values()).thenReturn(Stream.<OWLLiteral>empty());

			mapperDataProperty
					.when(() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processOneOf(oneOf);

			processorSpy.processOneOf(oneOf);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessOneOf_withDataOneOfAndSomeValues() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {

			OWLLiteral literal = mock(OWLLiteral.class);
			OWLDataOneOf oneOf = mock(OWLDataOneOf.class);
			when(oneOf.values()).thenReturn(Stream.<OWLLiteral>of(literal));

			mapperDataProperty
					.when(() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processOneOf(oneOf);

			processorSpy.processOneOf(oneOf);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessComplementOf_objectWithEmptyPrefixMap() {
		OWLObjectComplementOf complement = mock(OWLObjectComplementOf.class);
		OWLClassExpression operand = mock(OWLClassExpression.class);
		OWLClass owlClass = mock(OWLClass.class);
		IRI classIRI = mock(IRI.class);
		when(owlClass.getIRI()).thenReturn(classIRI);
		when(operand.asOWLClass()).thenReturn(owlClass);
		when(complement.getOperand()).thenReturn(operand);

		OWLOntology owlOntology = mock(OWLOntology.class);
		OWLOntologyManager owlOntologyManager = mock(OWLOntologyManager.class);
		OWLDocumentFormat owlDocumentFormat = mock(OWLDocumentFormat.class);
		PrefixDocumentFormat prefixDocumentFormat = mock(PrefixDocumentFormat.class);
		when(prefixDocumentFormat.getPrefixName2PrefixMap()).thenReturn(Map.of());
		when(owlDocumentFormat.asPrefixOWLDocumentFormat()).thenReturn(prefixDocumentFormat);
		when(owlDocumentFormat.isPrefixOWLDocumentFormat()).thenReturn(true);
		when(owlOntologyManager.getOntologyFormat(owlOntology)).thenReturn(owlDocumentFormat);
		when(owlOntology.getOWLOntologyManager()).thenReturn(owlOntologyManager);
		when(context.getBaseClassOntology()).thenReturn(owlOntology);

		doCallRealMethod().when(processorSpy).getOrCreateSchema();
		doCallRealMethod().when(processorSpy).processComplementOf(complement);

		processorSpy.processComplementOf(complement);

		verify(processorSpy, times(1)).getOrCreateSchema();
	}

	@Test
	public void testProcessComplementOf_objectWithOneValueInPrefixMap() {
		OWLObjectComplementOf complement = mock(OWLObjectComplementOf.class);
		OWLClassExpression operand = mock(OWLClassExpression.class);
		OWLClass owlClass = mock(OWLClass.class);
		IRI classIRI = mock(IRI.class);
		when(owlClass.getIRI()).thenReturn(classIRI);
		when(operand.asOWLClass()).thenReturn(owlClass);
		when(complement.getOperand()).thenReturn(operand);

		OWLOntology owlOntology = mock(OWLOntology.class);
		OWLOntologyManager owlOntologyManager = mock(OWLOntologyManager.class);
		OWLDocumentFormat owlDocumentFormat = mock(OWLDocumentFormat.class);
		PrefixDocumentFormat prefixDocumentFormat = mock(PrefixDocumentFormat.class);
		when(prefixDocumentFormat.getPrefixName2PrefixMap()).thenReturn(Map.of("name", "prefix"));
		when(owlDocumentFormat.asPrefixOWLDocumentFormat()).thenReturn(prefixDocumentFormat);
		when(owlDocumentFormat.isPrefixOWLDocumentFormat()).thenReturn(true);
		when(owlOntologyManager.getOntologyFormat(owlOntology)).thenReturn(owlDocumentFormat);
		when(owlOntology.getOWLOntologyManager()).thenReturn(owlOntologyManager);
		when(context.getBaseClassOntology()).thenReturn(owlOntology);

		doCallRealMethod().when(processorSpy).getOrCreateSchema();
		doCallRealMethod().when(processorSpy).processComplementOf(complement);

		processorSpy.processComplementOf(complement);

		verify(processorSpy, times(1)).getOrCreateSchema();
	}

	@Test
	public void testProcessComplementOf_withDataWithNoDatatypes() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {

			OWLDataComplementOf complement = mock(OWLDataComplementOf.class);
			when(complement.datatypesInSignature()).thenReturn(Stream.<OWLDatatype>empty());

			mapperDataProperty
					.when(() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processComplementOf(complement);

			processorSpy.processComplementOf(complement);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessComplementOf_withDataWithOneDatatype() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {

			OWLDatatype datatype = mock(OWLDatatype.class);
			OWLDataComplementOf complement = mock(OWLDataComplementOf.class);
			when(complement.datatypesInSignature()).thenReturn(Stream.<OWLDatatype>of(datatype));

			mapperDataProperty
					.when(() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()))
					.then(invocationOnMock -> null);

			doCallRealMethod().when(processorSpy).getOrCreateSchema();
			doCallRealMethod().when(processorSpy).processComplementOf(complement);

			processorSpy.processComplementOf(complement);

			verify(processorSpy, times(1)).getOrCreateSchema();

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()), times(1));
		}
	}

	@Test
	public void testGetOrCreateSchema_withExistingPropertySchema() {

		// context.currentlyProcessedPropertyName =

		doCallRealMethod().when(processorSpy).getOrCreateSchema();

		// A property schema is initialized in the setUp() method
		processorSpy.getOrCreateSchema();

		verify(processorSpy, times(1)).getOrCreateSchema();
	}

	@Test
	public void testGetOrCreateSchema_withNoPropertySchemaAndNoDefaultDescription() {
		try (MockedStatic<MapperProperty> mapperProperty = mockStatic(MapperProperty.class); ) {
			GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, false);

			final var propertyName = "noPropertySchemaExistsForThisPropertyName";
			context.withProcessedProperty(
					propertyName,
					() -> {
						mapperProperty
								.when(() -> MapperProperty.setSchemaName(any(), eq(propertyName)))
								.then(invocationOnMock -> null);

						mapperProperty
								.when(() -> MapperProperty.setSchemaDescription(any(), eq(null)))
								.then(invocationOnMock -> null);

						doCallRealMethod().when(processorSpy).getOrCreateSchema();

						// A property schema is initialized in the setUp() method
						processorSpy.getOrCreateSchema();

						verify(processorSpy, times(1)).getOrCreateSchema();

						// Verify that the expected static method was invoked
						mapperProperty.verify(
								() -> MapperProperty.setSchemaName(any(), eq(propertyName)), times(1));

						mapperProperty.verify(
								() -> MapperProperty.setSchemaDescription(any(), eq(null)), times(1));

						assertNotNull(context.getClassSchema().getProperties().get(propertyName));
					});
		}
	}

	@Test
	public void testGetOrCreateSchema_withNoPropertySchemaAndDefaultDescription() {
		try (MockedStatic<MapperProperty> mapperProperty = mockStatic(MapperProperty.class); ) {
			GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, true);

			final var propertyName = "noPropertySchemaExistsForThisPropertyName";
			context.withProcessedProperty(
					propertyName,
					() -> {
						mapperProperty
								.when(() -> MapperProperty.setSchemaName(any(), eq(propertyName)))
								.then(invocationOnMock -> null);

						mapperProperty
								.when(
										() ->
												MapperProperty.setSchemaDescription(
														any(), eq(ObaConstants.DEFAULT_DESCRIPTION)))
								.then(invocationOnMock -> null);

						doCallRealMethod().when(processorSpy).getOrCreateSchema();

						// A property schema is initialized in the setUp() method
						processorSpy.getOrCreateSchema();

						verify(processorSpy, times(1)).getOrCreateSchema();

						// Verify that the expected static method was invoked
						mapperProperty.verify(
								() -> MapperProperty.setSchemaName(any(), eq(propertyName)), times(1));

						mapperProperty.verify(
								() ->
										MapperProperty.setSchemaDescription(
												any(), eq(ObaConstants.DEFAULT_DESCRIPTION)),
								times(1));

						assertNotNull(context.getClassSchema().getProperties().get(propertyName));
					});
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withObjectSomeValuesFromAndRangeIsAString() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectSomeValuesFrom objRestriction = mock(OWLObjectSomeValuesFrom.class);
			String range = "range";

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(
											eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectSomeValuesFromAndRangeIsASchema() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectSomeValuesFrom objRestriction = mock(OWLObjectSomeValuesFrom.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(
											eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withObjectAllValuesFromAndRangeIsAString() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectAllValuesFrom objRestriction = mock(OWLObjectAllValuesFrom.class);
			String range = "range";

			mapperObjProperty
					.when(() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectAllValuesFromAndRangeIsASchema() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectAllValuesFrom objRestriction = mock(OWLObjectAllValuesFrom.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectAllValuesFromAndRangeIsUnknownObject() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectAllValuesFrom objRestriction = mock(OWLObjectAllValuesFrom.class);
			Object range = mock(Object.class);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), any(String.class)),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), any(Schema.class)),
					times(0));
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withObjectMinCardinalityAndRangeIsAString() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectMinCardinality objRestriction = mock(OWLObjectMinCardinality.class);
			String range = "range";

			Integer cardinality = 2;
			when(objRestriction.getCardinality()).thenReturn(cardinality);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addMinCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addMinCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectMinCardinalityAndRangeIsASchema() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectMinCardinality objRestriction = mock(OWLObjectMinCardinality.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(() -> MapperObjectProperty.addMinCardinalityToPropertySchema(any(), any(), any()))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addMinCardinalityToPropertySchema(any(), any(), any()),
					times(0));

			verify(logger, times(1)).log(eq(Level.SEVERE), anyString());
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withObjectMaxCardinalityAndRangeIsAString() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectMaxCardinality objRestriction = mock(OWLObjectMaxCardinality.class);
			String range = "range";

			Integer cardinality = 2;
			when(objRestriction.getCardinality()).thenReturn(cardinality);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addMaxCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addMaxCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectMaxCardinalityAndRangeIsASchema() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectMaxCardinality objRestriction = mock(OWLObjectMaxCardinality.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(() -> MapperObjectProperty.addMaxCardinalityToPropertySchema(any(), any(), any()))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addMaxCardinalityToPropertySchema(any(), any(), any()),
					times(0));

			verify(logger, times(1)).log(eq(Level.SEVERE), anyString());
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withObjectExactCardinalityAndRangeIsAString() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectExactCardinality objRestriction = mock(OWLObjectExactCardinality.class);
			String range = "range";

			Integer cardinality = 2;
			when(objRestriction.getCardinality()).thenReturn(cardinality);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addExactCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addExactCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectExactCardinalityAndRangeIsASchema() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectExactCardinality objRestriction = mock(OWLObjectExactCardinality.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(() -> MapperObjectProperty.addExactCardinalityToPropertySchema(any(), any(), any()))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addExactCardinalityToPropertySchema(any(), any(), any()),
					times(0));

			verify(logger, times(1)).log(eq(Level.SEVERE), anyString());
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withOtherQuantifiedRestrictionType() {
		try (MockedStatic<MapperObjectProperty> mapperObjProperty =
				mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLQuantifiedObjectRestriction objRestriction = mock(OWLQuantifiedObjectRestriction.class);
			String range = "range";
			Integer cardinality = 2;

			mapperObjProperty
					.when(() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mapperObjProperty
					.when(() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addMaxCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addMaxCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addExactCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			processorSpy.applyObjectRestriction(schema, objRestriction, range);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addMaxCardinalityToPropertySchema(any(), any(), any()),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addMaxCardinalityToPropertySchema(any(), any(), any()),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addExactCardinalityToPropertySchema(any(), any(), any()),
					times(0));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyDataRestriction_withDataSomeValuesFrom() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataSomeValuesFrom dataRestriction = mock(OWLDataSomeValuesFrom.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			doCallRealMethod()
					.when(processorSpy)
					.applyDataRestriction(schema, dataRestriction, range, cardinality);

			processorSpy.applyDataRestriction(schema, dataRestriction, range, cardinality);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyDataRestriction_withDataAllValuesFrom() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataAllValuesFrom dataRestriction = mock(OWLDataAllValuesFrom.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			doCallRealMethod()
					.when(processorSpy)
					.applyDataRestriction(schema, dataRestriction, range, cardinality);

			processorSpy.applyDataRestriction(schema, dataRestriction, range, cardinality);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)), times(1));
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyDataRestriction_withDataMinCardinality() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataMinCardinality dataRestriction = mock(OWLDataMinCardinality.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() ->
									MapperDataProperty.addMinCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			doCallRealMethod()
					.when(processorSpy)
					.applyDataRestriction(schema, dataRestriction, range, cardinality);

			processorSpy.applyDataRestriction(schema, dataRestriction, range, cardinality);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMinCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyDataRestriction_withDataMaxCardinality() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataMaxCardinality dataRestriction = mock(OWLDataMaxCardinality.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() ->
									MapperDataProperty.addMaxCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			doCallRealMethod()
					.when(processorSpy)
					.applyDataRestriction(schema, dataRestriction, range, cardinality);

			processorSpy.applyDataRestriction(schema, dataRestriction, range, cardinality);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMaxCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyDataRestriction_withDataExactCardinality() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataExactCardinality dataRestriction = mock(OWLDataExactCardinality.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() ->
									MapperDataProperty.addExactCardinalityToPropertySchema(
											eq(schema), eq(cardinality), eq(range)))
					.then(invocationOnMock -> null);

			doCallRealMethod()
					.when(processorSpy)
					.applyDataRestriction(schema, dataRestriction, range, cardinality);

			processorSpy.applyDataRestriction(schema, dataRestriction, range, cardinality);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addExactCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyDataRestriction_withDataUnknownRestriction() {
		try (MockedStatic<MapperDataProperty> mapperDataProperty =
				mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLQuantifiedDataRestriction dataRestriction = mock(OWLQuantifiedDataRestriction.class);
			String range = "range";
			Integer cardinality = 2;

			doCallRealMethod()
					.when(processorSpy)
					.applyDataRestriction(schema, dataRestriction, range, cardinality);

			processorSpy.applyDataRestriction(schema, dataRestriction, range, cardinality);

			// Verify that no static method was invoked for unknown restriction type
			mapperDataProperty.verify(
					() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)),
					times(0));

			mapperDataProperty.verify(
					() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)), times(0));

			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMinCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(0));

			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMaxCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(0));

			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addExactCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(0));
		}
	}
}
