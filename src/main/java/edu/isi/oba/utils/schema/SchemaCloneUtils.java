package edu.isi.oba.utils.schema;

import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.media.Schema;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class SchemaCloneUtils {
	/**
	 * Deep-copies all values from one {@link Schema} to another. Somewhat similar to cloning.
	 *
	 * @param sourceSchema a {@link Schema} to copy values from
	 * @param targetSchema a {@link Schema} to copy/merge values to
	 */
	public static void getClonedSchemaWithDeepCopiedEnumValues(
			final Schema<?> sourceSchema, final Schema<?> targetSchema) {
		if (sourceSchema == null || targetSchema == null) {
			return;
		}

		final var enumValues = sourceSchema.getEnum();
		if (enumValues == null || enumValues.isEmpty()) {
			logger.log(Level.WARNING, "No enum values provided. Skipping enum application.");
			return;
		}

		final var firstValue = enumValues.get(0);
		if (firstValue == null) {
			return;
		}

		final var handler = SchemaEnumUtils.getHandlerForClass(firstValue.getClass());
		if (handler != null) {
			handler.apply(targetSchema, enumValues);
		} else {
			// Fallback: treat as string or object, log a warning
			logger.log(
					Level.WARNING,
					"No enum handler found for type: "
							+ firstValue.getClass().getName()
							+ ". Enum list of values: "
							+ enumValues);
			targetSchema.setType("string");
			SchemaEnumUtils.castAndCopy(targetSchema, enumValues, String.class);
		}
	}

	public static void deepCopyListTypeValues(
			final Schema<?> sourceSchema,
			final Schema<?> targetSchema,
			final ComplexSchemaListType listType) {
		if (sourceSchema == null || targetSchema == null) {
			return;
		}

		List<?> values = null;
		switch (listType) {
			case ALLOF_LIST:
				values = sourceSchema.getAllOf();
				break;
			case ANYOF_LIST:
				values = sourceSchema.getAnyOf();
				break;
			case ONEOF_LIST:
				values = sourceSchema.getOneOf();
				break;
			default:
				values = null;
				return;
		}

		if (values == null || values.isEmpty()) {
			logger.log(
					Level.WARNING,
					"No allOf/anyOf/oneOf values provided. Skipping complex schema list application.");
			return;
		}

		final var firstValue = values.get(0);
		if (firstValue == null) {
			return;
		}

		final var handler = ComplexSchemaListUtils.getHandlerForClass(firstValue.getClass());
		if (handler != null) {
			handler.apply(targetSchema, values, listType);
		} else {
			// Fallback: treat as string or object, log a warning
			logger.log(
					Level.WARNING,
					"No schema list handler found for type: "
							+ firstValue.getClass().getName()
							+ ". List of values: "
							+ values);
			targetSchema.setType("string");
			ComplexSchemaListUtils.castAndCopy(targetSchema, values, String.class, listType);
		}
	}

	/**
	 * Utility method to deeply clone one {@link Schema} and return a copy with not
	 * links/references/dependencies to the original one.
	 *
	 * @param sourceSchema a {@link Schema} to be copied
	 * @return a clones version of {@code sourceSchema} parameter
	 * @throws NullPointerException
	 */
	public static Schema<?> clone(@Nonnull Schema<?> sourceSchema) throws NullPointerException {
		Objects.requireNonNull(sourceSchema, "clone() argument 'sourceSchema' must not be null");

		Schema<?> targetSchema;
		try {
			targetSchema = sourceSchema.getClass().getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			targetSchema = new Schema<>();
			logger.log(
					Level.SEVERE,
					"Cannot clone {} to {} with the same Schema type.  Using generic raw Schema instead.",
					List.of(sourceSchema, targetSchema));
			logger.log(Level.SEVERE, e.getLocalizedMessage());
		}

		if (!sourceSchema.getClass().equals(targetSchema.getClass())) {
			logger.log(
					Level.SEVERE,
					"clone(): 'sourceSchema' ("
							+ sourceSchema.getClass()
							+ ") and"
							+ " 'targetSchema' ("
							+ targetSchema.getClass()
							+ ") are different Schema types.");
			return targetSchema;
		}

		// Copy primitive and immutable fields directly
		targetSchema.setName(sourceSchema.getName());
		targetSchema.setType(sourceSchema.getType());
		targetSchema.setFormat(sourceSchema.getFormat());
		targetSchema.setDescription(sourceSchema.getDescription());
		targetSchema.setDefault(sourceSchema.getDefault());
		targetSchema.setNullable(sourceSchema.getNullable());
		targetSchema.setReadOnly(sourceSchema.getReadOnly());
		targetSchema.setWriteOnly(sourceSchema.getWriteOnly());

		targetSchema.set$ref(sourceSchema.get$ref());

		// ... copy other relevant fields like example, enum, readOnly, writeOnly, etc.

		targetSchema.setTitle(sourceSchema.getTitle());
		targetSchema.setMultipleOf(sourceSchema.getMultipleOf());
		targetSchema.setMaximum(sourceSchema.getMaximum());
		targetSchema.setExclusiveMaximum(sourceSchema.getExclusiveMaximum());
		targetSchema.setMinimum(sourceSchema.getMinimum());
		targetSchema.setExclusiveMinimum(sourceSchema.getExclusiveMinimum());
		targetSchema.setMaxLength(sourceSchema.getMaxLength());
		targetSchema.setMinLength(sourceSchema.getMinLength());
		targetSchema.setPattern(sourceSchema.getPattern());
		targetSchema.setMaxItems(sourceSchema.getMaxItems());
		targetSchema.setMinItems(sourceSchema.getMinItems());
		targetSchema.setUniqueItems(sourceSchema.getUniqueItems());
		targetSchema.setMaxProperties(sourceSchema.getMaxProperties());
		targetSchema.setMinProperties(sourceSchema.getMinProperties());
		targetSchema.setNot(sourceSchema.getNot());
		if (sourceSchema.getExample() != null) {
			targetSchema.setExample(sourceSchema.getExample());
		}
		targetSchema.setExternalDocs(sourceSchema.getExternalDocs());
		targetSchema.setDeprecated(sourceSchema.getDeprecated());
		targetSchema.setDiscriminator(sourceSchema.getDiscriminator());
		targetSchema.setXml(sourceSchema.getXml());

		// Deep copy 'enum' list if present
		getClonedSchemaWithDeepCopiedEnumValues(sourceSchema, targetSchema);

		// Deep copy 'required' list if present
		if (sourceSchema.getRequired() != null) {
			final List<String> copiedRequiredItems = new LinkedList<>();
			for (final var entry : sourceSchema.getRequired()) {
				copiedRequiredItems.add(entry);
			}
			targetSchema.setRequired(copiedRequiredItems);
		}

		// Deep copy 'properties' map if present
		if (sourceSchema.getProperties() != null) {
			for (final var entry : sourceSchema.getProperties().entrySet()) {
				targetSchema.addProperty(entry.getKey(), clone(entry.getValue()));
			}
		}

		// Deep copy 'items' schema if present (for array schemas)
		if (sourceSchema.getItems() != null) {
			targetSchema.setItems(clone(sourceSchema.getItems()));
		}

		// Deep copy 'additionalProperties' if it's a Schema object
		if (sourceSchema.getAdditionalProperties() instanceof Schema) {
			targetSchema.setAdditionalProperties(
					clone((Schema<?>) sourceSchema.getAdditionalProperties()));
		} else if (sourceSchema.getAdditionalProperties() != null) {
			// Handle cases where additionalProperties is a Boolean
			targetSchema.setAdditionalProperties(sourceSchema.getAdditionalProperties());
		}

		// ... handle other complex nested objects like allOf, anyOf, oneOf, not, etc.

		// Deep copy 'allOf' list if present
		deepCopyListTypeValues(sourceSchema, targetSchema, ComplexSchemaListType.ALLOF_LIST);

		// Deep copy 'anyOf' list if present
		deepCopyListTypeValues(sourceSchema, targetSchema, ComplexSchemaListType.ANYOF_LIST);

		// Deep copy 'oneOf' list if present
		deepCopyListTypeValues(sourceSchema, targetSchema, ComplexSchemaListType.ONEOF_LIST);

		return targetSchema;
	}
}
