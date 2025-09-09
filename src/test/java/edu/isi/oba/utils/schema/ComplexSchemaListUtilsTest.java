package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ComplexSchemaListUtilsTest extends BaseTest {

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<ComplexSchemaListUtils> constructor =
				ComplexSchemaListUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void testGetHandlerForClassKnownType() {
		assertNotNull(ComplexSchemaListUtils.getHandlerForClass(String.class));
		assertNotNull(ComplexSchemaListUtils.getHandlerForClass(Integer.class));
	}

	@Test
	public void testGetHandlerForClassUnknownType() {
		assertNotNull(ComplexSchemaListUtils.getHandlerForClass(Object.class));
	}

	@Test
	public void testCastAndCopyValidValuesAllOf() {
		Schema<?> schema = new Schema<>();
		schema.setAllOf(new ArrayList<>()); // Initialize allOf list

		List<Object> values =
				Arrays.asList(
						new Schema<>().type("string").title("A"), new Schema<>().type("integer").title("B"));

		ComplexSchemaListUtils.castAndCopy(
				schema, values, Schema.class, ComplexSchemaListType.ALLOF_LIST);

		assertNotNull(schema.getAllOf());
		assertEquals(2, schema.getAllOf().size());
		assertEquals("A", schema.getAllOf().get(0).getTitle());
		assertEquals("B", schema.getAllOf().get(1).getTitle());
	}

	@Test
	public void testCastAndCopyValidValuesAnyOf() {
		Schema<?> schema = new Schema<>();
		schema.setAnyOf(new ArrayList<>()); // Initialize anyOf list

		List<Object> values =
				Arrays.asList(
						new Schema<>().type("boolean").title("X"), new Schema<>().type("number").title("Y"));

		ComplexSchemaListUtils.castAndCopy(
				schema, values, Schema.class, ComplexSchemaListType.ANYOF_LIST);

		assertNotNull(schema.getAnyOf());
		assertEquals(2, schema.getAnyOf().size());
		assertEquals("X", schema.getAnyOf().get(0).getTitle());
		assertEquals("Y", schema.getAnyOf().get(1).getTitle());
	}

	@Test
	public void testCastAndCopyValidValuesOneOf() {
		Schema<?> schema = new Schema<>();
		schema.setOneOf(new ArrayList<>()); // Initialize oneOf list

		List<Object> values =
				Arrays.asList(
						new Schema<>().type("object").title("Obj1"),
						new Schema<>().type("array").title("Obj2"));

		ComplexSchemaListUtils.castAndCopy(
				schema, values, Schema.class, ComplexSchemaListType.ONEOF_LIST);

		assertNotNull(schema.getOneOf());
		assertEquals(2, schema.getOneOf().size());
		assertEquals("Obj1", schema.getOneOf().get(0).getTitle());
		assertEquals("Obj2", schema.getOneOf().get(1).getTitle());
	}

	@Test
	public void testCastAndCopyEmptyValues() {
		Schema<?> schema = new Schema<>();
		schema.setAnyOf(new ArrayList<>());

		List<Object> values = List.of();

		ComplexSchemaListUtils.castAndCopy(
				schema, values, Schema.class, ComplexSchemaListType.ANYOF_LIST);
		assertTrue(schema.getAnyOf().isEmpty());
	}

	@Test
	public void testCastAndCopyNullValues() {
		Schema<?> schema = new Schema<>();
		schema.setOneOf(new ArrayList<>());

		ComplexSchemaListUtils.castAndCopy(
				schema, null, Schema.class, ComplexSchemaListType.ONEOF_LIST);
		assertTrue(schema.getOneOf().isEmpty());
	}

	@Test
	public void testGetTypeForKnownClass() {
		assertEquals("string", ComplexSchemaListUtils.getType(String.class));
		assertEquals("integer", ComplexSchemaListUtils.getType(Integer.class));
	}

	@Test
	public void testGetTypeForUnknownClass() {
		assertEquals("object", ComplexSchemaListUtils.getType(Object.class));
	}

	@Test
	public void testGetFormatForKnownClass() {
		assertEquals("int64", ComplexSchemaListUtils.getFormat(Long.class));
		assertEquals("float", ComplexSchemaListUtils.getFormat(Float.class));
	}

	@Test
	public void testGetFormatForUnknownClass() {
		assertNull(ComplexSchemaListUtils.getFormat(String.class));
		assertNull(ComplexSchemaListUtils.getFormat(Boolean.class));
	}
}
