package edu.isi.oba.ontology.schema;

import edu.isi.oba.MapperDataProperty;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashSet;
import java.util.Map;

public class DefaultPropertyFactory {

	private DefaultPropertyFactory() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Get default schema properties.
	 *
	 * <p>These can be disabled by setting `default_properties` to `false` in the `config.yaml` file.
	 *
	 * @return A Map where key is property name and value is the property's Swagger/OpenAPI Schema.
	 */
	public static Map<String, Schema> getDefaultProperties() {
		// Add some typical default properties (e.g. id, lable, type, and description)
		final var idPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"id",
						"identifier",
						new HashSet<String>() {
							{
								add("integer");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(idPropertySchema, false);
		final var labelPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"label",
						"short description of the resource",
						new HashSet<String>() {
							{
								add("string");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(labelPropertySchema, true);
		final var typePropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"type",
						"type(s) of the resource",
						new HashSet<String>() {
							{
								add("string");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(typePropertySchema, true);
		final var descriptionPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"description",
						"small description",
						new HashSet<String>() {
							{
								add("string");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(descriptionPropertySchema, true);

		// Also add some default property examples of different types (e.g. a date/time, a boolean, and
		// a float)
		final var eventDateTimePropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"eventDateTime",
						"a date/time of the resource",
						new HashSet<String>() {
							{
								add("dateTime");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(eventDateTimePropertySchema, true);
		final var isBoolPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"isBool",
						"a boolean indicator of the resource",
						new HashSet<String>() {
							{
								add("boolean");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(isBoolPropertySchema, true);
		final var quantityPropertySchema =
				MapperDataProperty.createDataPropertySchema(
						"quantity",
						"a number quantity of the resource",
						new HashSet<String>() {
							{
								add("float");
							}
						});
		MapperDataProperty.setNullableValueForPropertySchema(quantityPropertySchema, true);

		return Map.ofEntries(
				Map.entry(idPropertySchema.getName(), idPropertySchema),
				Map.entry(labelPropertySchema.getName(), labelPropertySchema),
				Map.entry(typePropertySchema.getName(), typePropertySchema),
				Map.entry(descriptionPropertySchema.getName(), descriptionPropertySchema),
				Map.entry(eventDateTimePropertySchema.getName(), eventDateTimePropertySchema),
				Map.entry(isBoolPropertySchema.getName(), isBoolPropertySchema),
				Map.entry(quantityPropertySchema.getName(), quantityPropertySchema));
	}
}
