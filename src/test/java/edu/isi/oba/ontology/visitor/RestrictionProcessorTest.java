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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.*;

public class RestrictionProcessorTest {

	private VisitorContext context;
	private Logger logger;
	private OWLObjectVisitor visitor;

	private Schema classSchema;
	private Schema propertySchema;

	@BeforeEach
	public void setUp() {
		context = mock(VisitorContext.class);
		logger = mock(Logger.class);
		visitor = mock(OWLObjectVisitor.class);

		classSchema = mock(Schema.class);
		propertySchema = new Schema();
		when(classSchema.getProperties()).thenReturn(Map.of("testProperty", propertySchema));
		when(context.getClassSchema()).thenReturn(classSchema);
		when(context.getCurrentlyProcessedPropertyName()).thenReturn("testProperty");
	}

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<RestrictionProcessor> constructor =
				RestrictionProcessor.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withObjectOneOf() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {

			OWLObjectOneOf oneOf = mock(OWLObjectOneOf.class);
			OWLObjectSomeValuesFrom restriction = mock(OWLObjectSomeValuesFrom.class);
			when(restriction.getFiller()).thenReturn(oneOf);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedObjectRestriction(
											any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processQuantifiedObjectRestriction(
					restriction, visitor, context, logger);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			verify(oneOf, times(1)).accept(visitor);
		}
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withNaryBooleanExpression() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mockedMapperObjProp =
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

			// -----------
			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedObjectRestriction(
											any(), any(), any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.then(invocationOnMock -> null);

			RestrictionProcessor.processQuantifiedObjectRestriction(
					restriction, visitor, context, logger);

			verify(booleanCE, times(0)).accept(visitor);
			mockedRestrictProc.verify(
					() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()), times(1));

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.getComplexObjectComposedSchema(booleanCE), times(1));

			mockedSchemaBuilder.verify(() -> SchemaBuilder.getPrefixedSchemaName(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withOtherExpressionType() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mockedMapperObjProp =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedObjectRestriction(
											any(), any(), any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.then(invocationOnMock -> null);

			RestrictionProcessor.processQuantifiedObjectRestriction(
					restriction, visitor, context, logger);

			verify(filler, times(0)).accept(visitor);
			mockedRestrictProc.verify(
					() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()), times(1));

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.getComplexObjectComposedSchema(any()), times(0));

			mockedSchemaBuilder.verify(() -> SchemaBuilder.getPrefixedSchemaName(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessQuantifiedDataRestriction_withOneOfAndNullCardinality() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {
			OWLDataOneOf oneOf = mock(OWLDataOneOf.class);
			OWLDataSomeValuesFrom restriction = mock(OWLDataSomeValuesFrom.class);
			when(restriction.getFiller()).thenReturn(oneOf);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedDataRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.then(invocationOnMock -> null);

			RestrictionProcessor.processQuantifiedDataRestriction(restriction, visitor, context, logger);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));
			verify(oneOf, times(1)).accept(visitor);
		}
	}

	@Test
	public void testProcessQuantifiedDataRestriction_withNaryDataRange() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mockedMapperDataProp =
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

			// -----------
			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedDataRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.then(invocationOnMock -> null);

			RestrictionProcessor.processQuantifiedDataRestriction(restriction, visitor, context, logger);

			verify(filler, times(0)).accept(visitor);
			mockedRestrictProc.verify(
					() -> RestrictionProcessor.processQuantifiedDataRestriction(any(), any(), any(), any()),
					times(1));

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(filler), times(1));

			mockedMapperDataProp.verify(
					() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessQuantifiedDataRestriction_withDatatypeRestriction() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mockedMapperDataProp =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedDataRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.then(invocationOnMock -> null);

			RestrictionProcessor.processQuantifiedDataRestriction(restriction, visitor, context, logger);

			verify(filler, times(0)).accept(visitor);
			mockedRestrictProc.verify(
					() -> RestrictionProcessor.processQuantifiedDataRestriction(any(), any(), any(), any()),
					times(1));

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(any()), times(0));

			mockedMapperDataProp.verify(
					() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessQuantifiedObjectRestriction_withOtherDataRangeType() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mockedMapperDataProp =
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

			// -----------
			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processQuantifiedDataRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.then(invocationOnMock -> null);

			RestrictionProcessor.processQuantifiedDataRestriction(restriction, visitor, context, logger);

			verify(filler, times(0)).accept(visitor);
			mockedRestrictProc.verify(
					() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()),
					times(1));

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(any()), times(0));

			mockedMapperDataProp.verify(
					() -> MapperDataProperty.addDatatypeRestrictionToPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessNaryBooleanClassExpression() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperProperty> mockedMapperProp = mockStatic(MapperProperty.class);
				MockedStatic<MapperObjectProperty> mockedMapperObjProp =
						mockStatic(MapperObjectProperty.class); ) {
			OWLObjectUnionOf union = mock(OWLObjectUnionOf.class);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.getComplexObjectComposedSchema(union))
					.thenCallRealMethod();

			mockedMapperProp.when(() -> MapperProperty.setSchemaType(any(), any())).thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.processNaryBooleanClassExpression(any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processNaryBooleanClassExpression(union, visitor, context);

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.getComplexObjectComposedSchema(union), times(1));
			mockedMapperProp.verify(() -> MapperProperty.setSchemaType(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessNaryDataRange() {

		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperProperty> mockedMapperProp = mockStatic(MapperProperty.class);
				MockedStatic<MapperDataProperty> mockedMapperDataProp =
						mockStatic(MapperDataProperty.class); ) {
			OWLDataUnionOf union = mock(OWLDataUnionOf.class);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperDataProp
					.when(() -> MapperDataProperty.getComplexDataComposedSchema(union))
					.thenCallRealMethod();

			mockedMapperProp.when(() -> MapperProperty.setSchemaType(any(), any())).thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.processNaryDataRange(any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processNaryDataRange(union, visitor, context);

			// Verify that the expected static method was invoked
			mockedMapperDataProp.verify(
					() -> MapperDataProperty.getComplexDataComposedSchema(union), times(1));
			mockedMapperProp.verify(() -> MapperProperty.setSchemaType(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessHasValue_forObjectWithNamedIndividual() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mockedMapperObjProp =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processHasValue(any(OWLObjectHasValue.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processHasValue(hasValue, visitor, context);

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessHasValue_forObjectWithNonNamedIndividual() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mockedMapperObjProp =
						mockStatic(MapperObjectProperty.class); ) {
			OWLIndividual individual = mock(OWLIndividual.class);
			OWLObjectHasValue hasValue = mock(OWLObjectHasValue.class);
			when(hasValue.getFiller()).thenReturn(individual);

			// Simulate static method invocation for restriction processing
			// Static method behavior is mocked for controlled testing
			mockedMapperObjProp
					.when(() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processHasValue(any(OWLObjectHasValue.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processHasValue(hasValue, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mockedMapperObjProp.verify(
					() -> MapperObjectProperty.addHasValueOfPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessHasValue_forDataWithLiteralHavingDatatype() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
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

			// -----------
			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() -> RestrictionProcessor.processHasValue(any(OWLDataHasValue.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processHasValue(hasValue, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessHasValue_forDataWithLiteralWithoutDatatype() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {

			OWLLiteral literal = mock(OWLLiteral.class);
			when(literal.getDatatype()).thenReturn(null);

			OWLDataHasValue hasValue = mock(OWLDataHasValue.class);
			when(hasValue.getFiller()).thenReturn(literal);

			mapperDataProperty
					.when(() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() -> RestrictionProcessor.processHasValue(any(OWLDataHasValue.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processHasValue(hasValue, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addHasValueOfPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessOneOf_withObjectOneOf() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {
			OWLObjectOneOf oneOf = mock(OWLObjectOneOf.class);
			OWLIndividual individual = mock(OWLIndividual.class);
			OWLNamedIndividual namedIndividual = mock(OWLNamedIndividual.class);
			when(namedIndividual.getIRI()).thenReturn(mock(IRI.class));
			when(individual.asOWLNamedIndividual()).thenReturn(namedIndividual);
			when(oneOf.getIndividuals()).thenReturn(Collections.singleton(individual));

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.processOneOf(any(OWLObjectOneOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processOneOf(oneOf, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessOneOf_withDataOneOfAndEmptyValues() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {

			OWLDataOneOf oneOf = mock(OWLDataOneOf.class);
			when(oneOf.values()).thenReturn(Stream.<OWLLiteral>empty());

			mapperDataProperty
					.when(() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.processOneOf(any(OWLDataOneOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processOneOf(oneOf, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessOneOf_withDataOneOfAndSomeValues() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {

			OWLLiteral literal = mock(OWLLiteral.class);
			OWLDataOneOf oneOf = mock(OWLDataOneOf.class);
			when(oneOf.values()).thenReturn(Stream.<OWLLiteral>of(literal));

			mapperDataProperty
					.when(() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(() -> RestrictionProcessor.processOneOf(any(OWLDataOneOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processOneOf(oneOf, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addOneOfDataPropertySchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessComplementOf_objectWithEmptyPrefixMap() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processComplementOf(
											any(OWLObjectComplementOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processComplementOf(complement, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessComplementOf_objectWithOneValueInPrefixMap() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processComplementOf(
											any(OWLObjectComplementOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processComplementOf(complement, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));
		}
	}

	@Test
	public void testProcessComplementOf_withDataWithNoDatatypes() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {

			OWLDataComplementOf complement = mock(OWLDataComplementOf.class);
			when(complement.datatypesInSignature()).thenReturn(Stream.<OWLDatatype>empty());

			mapperDataProperty
					.when(() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processComplementOf(
											any(OWLDataComplementOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processComplementOf(complement, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()), times(0));
		}
	}

	@Test
	public void testProcessComplementOf_withDataWithOneDatatype() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {

			OWLDatatype datatype = mock(OWLDatatype.class);
			OWLDataComplementOf complement = mock(OWLDataComplementOf.class);
			when(complement.datatypesInSignature()).thenReturn(Stream.<OWLDatatype>of(datatype));

			mapperDataProperty
					.when(() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			mockedRestrictProc
					.when(
							() ->
									RestrictionProcessor.processComplementOf(
											any(OWLDataComplementOf.class), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.processComplementOf(complement, visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.setComplementOfForDataSchema(any(), any()), times(1));
		}
	}

	@Test
	public void testGetOrCreateSchema_withNoPropertyNameMeaningItsAClassSchemaInstead() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			when(context.getCurrentlyProcessedPropertyName()).thenReturn(null);

			// A property schema is initialized in the setUp() method
			Schema schema = RestrictionProcessor.getOrCreateSchema(visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			assertNotNull(schema);
			verify(context, times(1)).getClassSchema();
			assertEquals(context.getClassSchema(), schema);
		}
	}

	@Test
	public void testGetOrCreateSchema_withExistingPropertySchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
				mockStatic(RestrictionProcessor.class); ) {

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			final var propertyName = "testProperty";

			// A property schema is initialized in the setUp() method
			Schema schema = RestrictionProcessor.getOrCreateSchema(visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));
			assertNotNull(context.getClassSchema().getProperties().get(propertyName));
			assertNotNull(schema);
		}
	}

	@Test
	public void testGetOrCreateSchema_withNoPropertySchemaAndNoDefaultDescription() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperProperty> mapperProperty = mockStatic(MapperProperty.class); ) {
			GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, false);

			final var propertyName = "noPropertySchemaExistsForThisPropertyName";
			when(context.getCurrentlyProcessedPropertyName()).thenReturn(propertyName);

			mapperProperty
					.when(() -> MapperProperty.setSchemaName(any(Schema.class), eq(propertyName)))
					.then(invocationOnMock -> null);

			mapperProperty
					.when(() -> MapperProperty.setSchemaDescription(any(Schema.class), eq(null)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			// A property schema is initialized in the setUp() method
			Schema schema = RestrictionProcessor.getOrCreateSchema(visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperProperty.verify(
					() -> MapperProperty.setSchemaName(any(Schema.class), eq(propertyName)), times(1));

			mapperProperty.verify(
					() -> MapperProperty.setSchemaDescription(any(Schema.class), eq(null)), times(1));

			assertNotNull(schema);
			assertNotEquals(propertySchema, schema);
		}
	}

	@Test
	public void testGetOrCreateSchema_withNoPropertySchemaAndDefaultDescription() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperProperty> mapperProperty = mockStatic(MapperProperty.class); ) {
			GlobalFlags.setFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS, true);

			final var propertyName = "noPropertySchemaExistsForThisPropertyName";
			when(context.getCurrentlyProcessedPropertyName()).thenReturn(propertyName);

			mapperProperty
					.when(() -> MapperProperty.setSchemaName(any(Schema.class), eq(propertyName)))
					.then(invocationOnMock -> null);

			mapperProperty
					.when(
							() ->
									MapperProperty.setSchemaDescription(
											any(Schema.class), eq(ObaConstants.DEFAULT_DESCRIPTION)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.getOrCreateSchema(any(), any()))
					.thenCallRealMethod();

			// A property schema is initialized in the setUp() method
			Schema schema = RestrictionProcessor.getOrCreateSchema(visitor, context);

			mockedRestrictProc.verify(
					() -> RestrictionProcessor.getOrCreateSchema(any(), any()), times(1));

			// Verify that the expected static method was invoked
			mapperProperty.verify(
					() -> MapperProperty.setSchemaName(any(Schema.class), eq(propertyName)), times(1));

			mapperProperty.verify(
					() ->
							MapperProperty.setSchemaDescription(
									any(Schema.class), eq(ObaConstants.DEFAULT_DESCRIPTION)),
					times(1));

			assertNotNull(schema);
			assertNotEquals(propertySchema, schema);
		}
	}

	// -- ApplyObjectRestriction Tests --
	@Test
	public void testApplyObjectRestriction_withObjectSomeValuesFromAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectSomeValuesFromAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addSomeValuesFromToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectAllValuesFromAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
						mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectAllValuesFrom objRestriction = mock(OWLObjectAllValuesFrom.class);
			String range = "range";

			mapperObjProperty
					.when(() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectAllValuesFromAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
						mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectAllValuesFrom objRestriction = mock(OWLObjectAllValuesFrom.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(1));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectAllValuesFromAndRangeIsUnknownObject() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
						mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectAllValuesFrom objRestriction = mock(OWLObjectAllValuesFrom.class);
			Object range = mock(Object.class);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), any(String.class)),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), any(Schema.class)),
					times(0));
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectMinCardinalityAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

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
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
						mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectMinCardinality objRestriction = mock(OWLObjectMinCardinality.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addMinCardinalityToPropertySchema(
											any(), any(), any(String.class)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addMinCardinalityToPropertySchema(
									any(), any(), any(String.class)),
					times(0));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectMaxCardinalityAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

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
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
						mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectMaxCardinality objRestriction = mock(OWLObjectMaxCardinality.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addMaxCardinalityToPropertySchema(
											any(), any(), any(Schema.class)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addMaxCardinalityToPropertySchema(
									any(), any(), any(Schema.class)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyObjectRestriction_withObjectExactCardinalityAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

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
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
						mockStatic(MapperObjectProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLObjectExactCardinality objRestriction = mock(OWLObjectExactCardinality.class);
			Schema range = mock(Schema.class);

			mapperObjProperty
					.when(
							() ->
									MapperObjectProperty.addExactCardinalityToPropertySchema(
											any(), any(), any(String.class)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addExactCardinalityToPropertySchema(
									any(), any(), any(Schema.class)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyObjectRestriction_withOtherQuantifiedRestrictionType() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperObjectProperty> mapperObjProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyObjectRestriction(any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyObjectRestriction(schema, objRestriction, range, logger);

			// Verify that the expected static method was invoked
			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(0));

			mapperObjProperty.verify(
					() -> MapperObjectProperty.addAllOfToObjectPropertySchema(eq(schema), eq(range)),
					times(0));

			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addMaxCardinalityToPropertySchema(
									any(), any(), any(String.class)),
					times(0));

			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addMaxCardinalityToPropertySchema(
									any(), any(), any(String.class)),
					times(0));

			mapperObjProperty.verify(
					() ->
							MapperObjectProperty.addExactCardinalityToPropertySchema(
									any(), any(), any(String.class)),
					times(0));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	// -- ApplyDataRestriction Tests --
	@Test
	public void testApplyDataRestriction_withDataSomeValuesFromAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataSomeValuesFrom dataRestriction = mock(OWLDataSomeValuesFrom.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataSomeValuesFromAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataSomeValuesFrom dataRestriction = mock(OWLDataSomeValuesFrom.class);
			Schema range = mock(Schema.class);
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addSomeValuesFromToDataPropertySchema(eq(schema), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataAllValuesFromAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataAllValuesFrom dataRestriction = mock(OWLDataAllValuesFrom.class);
			String range = "range";
			Integer cardinality = 2;

			mapperDataProperty
					.when(() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)), times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataAllValuesFromAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataAllValuesFrom dataRestriction = mock(OWLDataAllValuesFrom.class);
			Schema range = mock(Schema.class);
			Integer cardinality = 2;

			mapperDataProperty
					.when(() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() -> MapperDataProperty.addAllOfDataPropertySchema(eq(schema), eq(range)), times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataMinCardinalityAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMinCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataMinCardinalityAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataMinCardinality dataRestriction = mock(OWLDataMinCardinality.class);
			Schema range = mock(Schema.class);
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() ->
									MapperDataProperty.addMinCardinalityToPropertySchema(
											eq(schema), eq(cardinality), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMinCardinalityToPropertySchema(
									eq(schema), eq(cardinality), any()),
					times(0));

			verify(logger, times(1)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataMaxCardinalityAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMaxCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataMaxCardinalityAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataMaxCardinality dataRestriction = mock(OWLDataMaxCardinality.class);
			Schema range = mock(Schema.class);
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() ->
									MapperDataProperty.addMaxCardinalityToPropertySchema(
											eq(schema), eq(cardinality), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addMaxCardinalityToPropertySchema(
									eq(schema), eq(cardinality), any()),
					times(0));

			verify(logger, times(1)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataExactCardinalityAndRangeIsAString() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
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

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addExactCardinalityToPropertySchema(
									eq(schema), eq(cardinality), eq(range)),
					times(1));

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataExactCardinalityAndRangeIsASchema() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLDataExactCardinality dataRestriction = mock(OWLDataExactCardinality.class);
			Schema range = mock(Schema.class);
			Integer cardinality = 2;

			mapperDataProperty
					.when(
							() ->
									MapperDataProperty.addExactCardinalityToPropertySchema(
											eq(schema), eq(cardinality), any()))
					.then(invocationOnMock -> null);

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

			// Verify that the expected static method was invoked
			mapperDataProperty.verify(
					() ->
							MapperDataProperty.addExactCardinalityToPropertySchema(
									eq(schema), eq(cardinality), any()),
					times(0));

			verify(logger, times(1)).log(eq(Level.SEVERE), anyString());
		}
	}

	@Test
	public void testApplyDataRestriction_withDataUnknownRestriction() {
		try (MockedStatic<RestrictionProcessor> mockedRestrictProc =
						mockStatic(RestrictionProcessor.class);
				MockedStatic<MapperDataProperty> mapperDataProperty =
						mockStatic(MapperDataProperty.class); ) {
			Schema schema = mock(Schema.class);
			OWLQuantifiedDataRestriction dataRestriction = mock(OWLQuantifiedDataRestriction.class);
			String range = "range";
			Integer cardinality = 2;

			mockedRestrictProc
					.when(() -> RestrictionProcessor.applyDataRestriction(any(), any(), any(), any(), any()))
					.thenCallRealMethod();

			RestrictionProcessor.applyDataRestriction(
					schema, dataRestriction, range, cardinality, logger);

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

			verify(logger, times(0)).log(eq(Level.SEVERE), anyString());
		}
	}
}
