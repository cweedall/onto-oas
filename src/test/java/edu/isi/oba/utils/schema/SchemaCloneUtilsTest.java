package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

public class SchemaCloneUtilsTest {

	@Test
	public void testCloneSchemaWithEnum() {
		Schema<String> original = SchemaUtilsTestHelper.createStringEnumSchema();
		Schema<?> clone = SchemaCloneUtils.clone(original);
		assertEquals(original.getEnum(), clone.getEnum());
		assertEquals(original.getType(), clone.getType());
	}

	@Test
	public void testCloneSchemaWithProperties() {
		Schema<?> original = SchemaUtilsTestHelper.createSchemaWithProperties();
		Schema<?> clone = SchemaCloneUtils.clone(original);
		assertNotNull(clone.getProperties());
		assertEquals(original.getProperties().size(), clone.getProperties().size());
	}
}
