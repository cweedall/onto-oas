package edu.isi.oba.utils.schema;

import edu.isi.oba.BaseTest;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Arrays;
import java.util.List;

public class SchemaUtilsTestHelper extends BaseTest {
	public static Schema<String> createStringSchema(String title) {
		Schema<String> schema = new Schema<>();
		schema.setType("string");
		schema.setTitle(title);
		return schema;
	}

	public static Schema<Integer> createIntegerSchema(String title) {
		Schema<Integer> schema = new Schema<>();
		schema.setType("integer");
		schema.setTitle(title);
		return schema;
	}

	public static Schema<Object> createObjectSchema(String title) {
		Schema<Object> schema = new Schema<>();
		schema.setType("object");
		schema.setTitle(title);
		return schema;
	}

	public static Schema<Object> createRefSchema(String ref) {
		Schema<Object> schema = new Schema<>();
		schema.set$ref(ref);
		return schema;
	}

	public static Schema<Object> createEnumSchema(String title, Object... values) {
		Schema<Object> schema = new Schema<>();
		schema.setType("string");
		schema.setTitle(title);
		List<Object> enumValues = Arrays.asList(values);
		schema.setEnum(enumValues);
		return schema;
	}
}
