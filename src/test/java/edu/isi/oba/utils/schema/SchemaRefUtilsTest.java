package edu.isi.oba.utils.schema;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

public class SchemaRefUtilsTest {

	@Test
	public void testDereferenceSchemas() {
		final var schemas = SchemaUtilsTestHelper.createSchemaMapWithRefs();
		final var dereferenced = SchemaRefUtils.getDereferencedSchemasParallel(schemas);

		assertNotNull(dereferenced.get("Person"));
		assertNull(dereferenced.get("Person").get$ref());
		assertNotNull(dereferenced.get("Person").getProperties().get("address"));
		assertNull(((Schema<?>) dereferenced.get("Person").getProperties().get("address")).get$ref());
	}
}
