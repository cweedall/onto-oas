package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SchemaEnumUtilsTest extends BaseTest {

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
}
