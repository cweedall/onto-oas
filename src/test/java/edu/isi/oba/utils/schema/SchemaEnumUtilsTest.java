package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import edu.isi.oba.utils.schema.SchemaEnumUtils.EnumTypeHandler;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SchemaEnumUtilsTest extends BaseTest {

	@Test
	void shouldThrowException_whenConstructorIsInvoked() throws Exception {
		Constructor<SchemaEnumUtils> constructor = SchemaEnumUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, constructor::newInstance);
	}

	@Test
	public void testGetHandlerForClassKnownType() {
		// Should return a non-null handler for known types like String
		assertNotNull(SchemaEnumUtils.getHandlerForClass(String.class));
		assertNotNull(SchemaEnumUtils.getHandlerForClass(Integer.class));
	}

	@Test
	public void testGetHandlerForClassUnknownType() {
		// Should fall back to default handler for unknown types
		assertNotNull(SchemaEnumUtils.getHandlerForClass(Object.class));
	}

	@Test
	public void testCastAndCopyValidEnumValues() {
		Schema<?> schema = new Schema<>();
		List<Object> values = Arrays.asList("RED", "GREEN", "BLUE");

		// Should copy enum values correctly
		SchemaEnumUtils.castAndCopy(schema, values, String.class);
		assertEquals(values, schema.getEnum());
	}

	@Test
	public void testCastAndCopyEmptyEnumValues() {
		Schema<?> schema = new Schema<>();
		List<Object> values = List.of();

		// Should result in empty enum list
		SchemaEnumUtils.castAndCopy(schema, values, String.class);
		assertTrue(schema.getEnum() == null || schema.getEnum().isEmpty());
	}

	@Test
	public void testCastAndCopyNullEnumValues() {
		Schema<?> schema = new Schema<>();

		// Should handle null safely
		SchemaEnumUtils.castAndCopy(schema, null, String.class);
		assertNull(schema.getEnum());
	}

	@Test
	public void testCastAndCopyMixedTypes() {
		Schema<?> schema = new Schema<>();
		List<Object> values = Arrays.asList("ONE", 2, true);

		// Should log warnings but still attempt to copy
		SchemaEnumUtils.castAndCopy(schema, values, Object.class);
		assertEquals(values, schema.getEnum());
	}

	@Test
	public void shouldApplyFormat_whenHandlerHasFormat() {
		Schema<?> schema = new Schema<>();
		List<Object> values = List.of("http://example.com");
		SchemaEnumUtils.getHandlerForClass(URI.class).apply(schema, values);
		assertEquals("uri", schema.getFormat());
	}

	@Test
	public void shouldClearExistingEnumValuesBeforeCopying() {
		Schema<String> schema = new Schema<>();
		schema.setEnum(new java.util.ArrayList<>(List.of("OLD")));
		List<Object> values = List.of("NEW");
		SchemaEnumUtils.castAndCopy(schema, values, String.class);
		assertEquals(List.of("NEW"), schema.getEnum());
	}

	@Test
	public void shouldLogError_whenCastFails() {
		Schema<String> schema = new Schema<>();
		List<String> values = List.of("value");
		// Intentionally use incompatible type
		SchemaEnumUtils.castAndCopy(schema, values, Float.class);
		// No assertion needed; this is to trigger the catch block
	}

	@Test
	public void shouldApplyDefaultHandler_whenTypeIsUnknown() {
		Schema<?> schema = new Schema<>();
		List<Object> values = List.of("A", "B", "C");

		// Get the default handler by passing an unknown type
		EnumTypeHandler<?> handler = SchemaEnumUtils.getHandlerForClass(Object.class);

		// Invoke the handler
		handler.apply(schema, values);

		// Verify that the default behavior was applied
		assertEquals("string", schema.getType());
		assertEquals(values, schema.getEnum());
	}
}
