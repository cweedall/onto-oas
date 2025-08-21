package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

public class SchemaEnumUtilsTest {

	@Test
	public void testApplyStringEnum() {
		Schema<String> schema = new Schema<>();
		SchemaEnumUtils.ENUM_TYPE_HANDLERS
				.get(String.class)
				.apply(schema, java.util.List.of("A", "B", "C"));
		assertEquals(java.util.List.of("A", "B", "C"), schema.getEnum());
	}

	@Test
	public void testApplyIntegerEnum() {
		Schema<Integer> schema = new Schema<>();
		SchemaEnumUtils.ENUM_TYPE_HANDLERS.get(Integer.class).apply(schema, java.util.List.of(1, 2, 3));
		assertEquals(java.util.List.of(1, 2, 3), schema.getEnum());
	}
}
