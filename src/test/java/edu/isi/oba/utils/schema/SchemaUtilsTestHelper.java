package edu.isi.oba.utils.schema;

import io.swagger.v3.oas.models.media.Schema;
import java.util.HashMap;
import java.util.Map;

public class SchemaUtilsTestHelper {

	public static Schema<String> createStringEnumSchema() {
		final Schema<String> schema = new Schema<>();
		schema.setType("string");
		schema.setEnum(java.util.List.of("A", "B", "C"));
		return schema;
	}

	public static Schema<Integer> createIntegerEnumSchema() {
		final Schema<Integer> schema = new Schema<>();
		schema.setType("integer");
		schema.setEnum(java.util.List.of(1, 2, 3));
		return schema;
	}

	public static Schema<?> createSchemaWithProperties() {
		final Schema<Object> schema = new Schema<>();
		schema.setType("object");
		schema.addProperty("name", createStringEnumSchema());
		schema.addProperty("age", createIntegerEnumSchema());
		return schema;
	}

	/**
	 * Needs to support raw {@link Schema} because that's how {@code
	 * io.swagger.v3.oas.models.OpenAPI.getComponents().getSchemas()} is returned.
	 *
	 * @return a {@link Map} of schema names and their {@link Schema}s
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Schema> createSchemaMapWithRefs() {
		final Map<String, Schema> schemas = new HashMap<>();

		final Schema<Object> personSchema = new Schema<>();
		personSchema.setType("object");
		personSchema.addProperty("name", createStringEnumSchema());
		personSchema.addProperty("address", new Schema<>().$ref("#/components/schemas/Address"));

		final Schema<Object> addressSchema = new Schema<>();
		addressSchema.setType("object");
		addressSchema.addProperty("street", createStringEnumSchema());
		addressSchema.addProperty("zipcode", createIntegerEnumSchema());

		schemas.put("Person", personSchema);
		schemas.put("Address", addressSchema);

		return schemas;
	}
}
