package edu.isi.oba.utils.schema;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nonnull;

public class SchemaRefUtils {

	private SchemaRefUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Get a map of schemas which references removed and replaced by the full schema spec.
	 *
	 * @param originalSchemas a {@link Map} of {@link Schema}s and their names as keys
	 * @return a {@link Map} of {@link Schema}s with all references removed
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Schema> getDereferencedSchemasParallel(
			@Nonnull Map<String, Schema> originalSchemas) throws NullPointerException {

		Objects.requireNonNull(originalSchemas, "'originalSchemas' must not be null");

		// Build dependency graph
		final Map<String, Set<String>> dependencyGraph = new HashMap<>();
		for (final var entry : originalSchemas.entrySet()) {
			dependencyGraph.put(entry.getKey(), getAllRefs(entry.getValue()));
		}

		// Topologically sort schema names
		final var sortedSchemaNames = SchemaSortUtils.topologicalSort(dependencyGraph);

		// Shared cache for dereferenced schemas
		final Map<String, Schema<?>> dereferenceCache = new HashMap<>();

		// Caching dereferencing logic
		for (final var name : sortedSchemaNames) {
			dereferenceCache.computeIfAbsent(
					name,
					key -> {
						final Schema<?> schema = originalSchemas.get(key);
						return getDereferencedSchema(schema, dereferenceCache);
					});
		}

		return new TreeMap<>(dereferenceCache); // Optional: sort by schema name
	}

	private static Schema<?> getDereferencedSchema(
			@Nonnull Schema<?> schema, @Nonnull Map<String, Schema<?>> allSchemas)
			throws NullPointerException {
		Objects.requireNonNull(schema, "getDereferencedSchema() argument 'schema' must not be null");
		Objects.requireNonNull(
				allSchemas, "getDereferencedSchema() argument 'allSchemas' must not be null");

		final var derefSchema = SchemaCloneUtils.clone(schema);

		if (containsNonPropertyRefs(derefSchema)) {
			final var refSchemaName = getSchemaRefName(derefSchema);
			if (refSchemaName != null && allSchemas.get(refSchemaName) != null) {
				replacePropertySchemaReferenceWithFullSchema(derefSchema, allSchemas.get(refSchemaName));
				derefSchema.set$ref(null);
			}

			final var allOf = derefSchema.getAllOf();
			if (allOf != null) {
				for (final var allOfItemSchema : allOf) {
					final var allOfRefSchemaName = getSchemaRefName(allOfItemSchema);
					if (allOfRefSchemaName != null && allSchemas.get(allOfRefSchemaName) != null) {
						replacePropertySchemaReferenceWithFullSchema(
								allOfItemSchema, allSchemas.get(allOfRefSchemaName));
						allOfItemSchema.set$ref(null);
					}
				}
			}

			final var anyOf = derefSchema.getAnyOf();
			if (anyOf != null) {
				for (final var anyOfItemSchema : anyOf) {
					final var anyOfRefSchemaName = getSchemaRefName(anyOfItemSchema);
					if (anyOfItemSchema != null && allSchemas.get(anyOfRefSchemaName) != null) {
						replacePropertySchemaReferenceWithFullSchema(
								anyOfItemSchema, allSchemas.get(anyOfRefSchemaName));
						anyOfItemSchema.set$ref(null);
					}
				}
			}

			final var oneOf = derefSchema.getOneOf();
			if (oneOf != null) {
				for (final var oneOfItemSchema : oneOf) {
					final var oneOfRefSchemaName = getSchemaRefName(oneOfItemSchema);
					if (oneOfItemSchema != null && allSchemas.get(oneOfRefSchemaName) != null) {
						replacePropertySchemaReferenceWithFullSchema(
								oneOfItemSchema, allSchemas.get(oneOfRefSchemaName));
						oneOfItemSchema.set$ref(null);
					}
				}
			}
		}

		if (containsPropertiesWithRefs(derefSchema)) {
			if (derefSchema.getProperties() != null) {
				for (final var entry : derefSchema.getProperties().entrySet()) {
					entry.setValue(getDereferencedSchema(entry.getValue(), allSchemas));
				}
			}
		}

		return derefSchema;
	}

	private static void replacePropertySchemaReferenceWithFullSchema(
			@Nonnull Schema<?> propertySchema, Schema<?> referenceSchema) throws NullPointerException {
		Objects.requireNonNull(
				propertySchema,
				"getDeepCopyReferenceSchemaIntoPropertySchema() argument 'propertySchema' must not be"
						+ " null");

		// Copy primitive and immutable fields directly
		if (propertySchema.getName() == null) {
			propertySchema.setName(referenceSchema.getName());
		}
		if (propertySchema.getType() == null) {
			propertySchema.setType(referenceSchema.getType());
		}
		if (propertySchema.getFormat() == null) {
			propertySchema.setFormat(referenceSchema.getFormat());
		}
		if (propertySchema.getDescription() == null) {
			propertySchema.setDescription(referenceSchema.getDescription());
		}
		if (propertySchema.getDefault() == null) {
			propertySchema.setDefault(referenceSchema.getDefault());
		}
		if (propertySchema.getNullable() == null) {
			propertySchema.setNullable(referenceSchema.getNullable());
		}
		if (propertySchema.getReadOnly() == null && referenceSchema.getReadOnly() != null) {
			propertySchema.setReadOnly(referenceSchema.getReadOnly());
		}
		if (propertySchema.getWriteOnly() == null && referenceSchema.getWriteOnly() != null) {
			propertySchema.setWriteOnly(referenceSchema.getWriteOnly());
		}

		if (propertySchema.get$ref() == null) {
			propertySchema.set$ref(referenceSchema.get$ref());
		}

		// ... copy other relevant fields like example, enum, readOnly, writeOnly, etc.

		if (propertySchema.getTitle() == null) {
			propertySchema.setTitle(referenceSchema.getTitle());
		}
		if (propertySchema.getMultipleOf() == null) {
			propertySchema.setMultipleOf(referenceSchema.getMultipleOf());
		}
		if (propertySchema.getMaximum() == null) {
			propertySchema.setMaximum(referenceSchema.getMaximum());
		}
		if (propertySchema.getExclusiveMaximum() == null) {
			propertySchema.setExclusiveMaximum(referenceSchema.getExclusiveMaximum());
		}
		if (propertySchema.getMinimum() == null) {
			propertySchema.setMinimum(referenceSchema.getMinimum());
		}
		if (propertySchema.getExclusiveMinimum() == null) {
			propertySchema.setExclusiveMinimum(referenceSchema.getExclusiveMinimum());
		}
		if (propertySchema.getMaxLength() == null) {
			propertySchema.setMaxLength(referenceSchema.getMaxLength());
		}
		if (propertySchema.getMinLength() == null) {
			propertySchema.setMinLength(referenceSchema.getMinLength());
		}
		if (propertySchema.getPattern() == null) {
			propertySchema.setPattern(referenceSchema.getPattern());
		}
		if (propertySchema.getMaxItems() == null) {
			propertySchema.setMaxItems(referenceSchema.getMaxItems());
		}
		if (propertySchema.getMinItems() == null) {
			propertySchema.setMinItems(referenceSchema.getMinItems());
		}
		if (propertySchema.getUniqueItems() == null) {
			propertySchema.setUniqueItems(referenceSchema.getUniqueItems());
		}
		if (propertySchema.getMaxProperties() == null) {
			propertySchema.setMaxProperties(referenceSchema.getMaxProperties());
		}
		if (propertySchema.getMinProperties() == null) {
			propertySchema.setMinProperties(referenceSchema.getMinProperties());
		}
		if (propertySchema.getNot() == null) {
			propertySchema.setNot(referenceSchema.getNot());
		}
		if (propertySchema.getExample() == null && referenceSchema.getExample() != null) {
			propertySchema.setExample(referenceSchema.getExample());
		}
		if (propertySchema.getExternalDocs() == null) {
			propertySchema.setExternalDocs(referenceSchema.getExternalDocs());
		}
		if (propertySchema.getDeprecated() == null) {
			propertySchema.setDeprecated(referenceSchema.getDeprecated());
		}
		if (propertySchema.getDiscriminator() == null) {
			propertySchema.setDiscriminator(referenceSchema.getDiscriminator());
		}
		if (propertySchema.getXml() == null) {
			propertySchema.setXml(referenceSchema.getXml());
		}

		// Deep copy 'enum' list if present
		if (referenceSchema.getEnum() != null) {
			SchemaCloneUtils.getClonedSchemaWithDeepCopiedEnumValues(referenceSchema, propertySchema);
		}

		// Deep copy 'required' list if present
		if (referenceSchema.getRequired() != null) {
			final List<String> copiedRequiredItems = new LinkedList<>();
			for (final var entry : referenceSchema.getRequired()) {
				copiedRequiredItems.add(entry);
			}
			propertySchema.setRequired(copiedRequiredItems);
		}

		// Deep copy 'properties' map if present
		if (propertySchema.getProperties() == null && referenceSchema.getProperties() != null) {
			for (final var entry : referenceSchema.getProperties().entrySet()) {
				propertySchema.addProperty(entry.getKey(), SchemaCloneUtils.clone(entry.getValue()));
			}
		}

		// Deep copy 'items' schema if present (for array schemas)
		if (propertySchema.getItems() == null && referenceSchema.getItems() != null) {
			propertySchema.setItems(SchemaCloneUtils.clone(referenceSchema.getItems()));
		}

		// Deep copy 'additionalProperties' if it's a Schema object
		if (propertySchema.getAdditionalProperties() == null) {
			if (referenceSchema.getAdditionalProperties() instanceof Schema) {
				propertySchema.setAdditionalProperties(
						SchemaCloneUtils.clone((Schema<?>) referenceSchema.getAdditionalProperties()));
			} else if (referenceSchema.getAdditionalProperties() != null) {
				// Handle cases where additionalProperties is a Boolean
				propertySchema.setAdditionalProperties(referenceSchema.getAdditionalProperties());
			}
		}

		// ... handle other complex nested objects like allOf, anyOf, oneOf, not, etc.

		// Deep copy 'allOf' list if present
		if (propertySchema.getAllOf() == null && referenceSchema.getAllOf() != null) {
			for (final var entry : referenceSchema.getAllOf()) {
				propertySchema.addAllOfItem(SchemaCloneUtils.clone(entry));
			}
		}

		// Deep copy 'anyOf' list if present
		if (propertySchema.getAnyOf() == null && referenceSchema.getAnyOf() != null) {
			for (final var entry : referenceSchema.getAnyOf()) {
				propertySchema.addAnyOfItem(SchemaCloneUtils.clone(entry));
			}
		}

		// Deep copy 'oneOf' list if present
		if (propertySchema.getOneOf() == null && referenceSchema.getOneOf() != null) {
			for (final var entry : referenceSchema.getOneOf()) {
				propertySchema.addOneOfItem(SchemaCloneUtils.clone(entry));
			}
		}
	}

	private static String getSchemaRefName(Schema<?> schema) {
		final var ref = schema.get$ref();
		return ref == null || ref.isBlank() ? null : ref.replaceAll("#/components/schemas/", "");
	}

	private static boolean containsNonPropertyRefs(@Nonnull Schema<?> schema)
			throws NullPointerException {
		Objects.requireNonNull(schema, "containsNonPropertyRefs() argument 'schema' must not be null");

		if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
			return true;
		}

		final var allOf = schema.getAllOf();
		if (allOf != null) {
			for (Schema<?> allOfItemSchema : allOf) {
				if (allOfItemSchema.get$ref() != null) {
					return true;
				}
			}
		}

		final var anyOf = schema.getAnyOf();
		if (anyOf != null) {
			for (Schema<?> anyOfItemSchema : anyOf) {
				if (anyOfItemSchema.get$ref() != null) {
					return true;
				}
			}
		}

		final var oneOf = schema.getOneOf();
		if (oneOf != null) {
			for (Schema<?> oneOfItemSchema : oneOf) {
				if (oneOfItemSchema.get$ref() != null) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean containsPropertiesWithRefs(@Nonnull Schema<?> schema)
			throws NullPointerException {
		Objects.requireNonNull(
				schema, "containsPropertiesWithRefs() argument 'schema' must not be null");

		if (schema.getProperties() == null || schema.getProperties().isEmpty()) {
			return false;
		}

		for (final var entry : schema.getProperties().entrySet()) {
			final var propSchema = entry.getValue();

			if (containsNonPropertyRefs(propSchema)) {
				return true;
			}
		}

		return false;
	}

	private static Set<String> collectRefs(@SuppressWarnings("rawtypes") List<Schema> schemas)
			throws NullPointerException {
		final Set<String> refs = new HashSet<>();

		if (schemas != null) {
			for (final var s : schemas) {
				if (s.get$ref() != null) {
					refs.add(s.get$ref().replace("#/components/schemas/", ""));
				}
			}
		}

		return refs;
	}

	private static Set<String> getNonPropertyRefs(@Nonnull Schema<?> schema)
			throws NullPointerException {
		Objects.requireNonNull(schema, "getNonPropertyRefs() argument 'schema' must not be null");

		final Set<String> refs = new HashSet<>();

		if (containsNonPropertyRefs(schema)) {
			refs.addAll(collectRefs(Collections.singletonList(schema)));

			if (schema.getAllOf() != null) {
				refs.addAll(collectRefs(schema.getAllOf()));
			}
			if (schema.getAnyOf() != null) {
				refs.addAll(collectRefs(schema.getAnyOf()));
			}
			if (schema.getOneOf() != null) {
				refs.addAll(collectRefs(schema.getOneOf()));
			}
		}

		return refs;
	}

	private static Set<String> getPropertyRefs(@Nonnull Schema<?> schema)
			throws NullPointerException {
		Objects.requireNonNull(schema, "getPropertyRefs() argument 'schema' must not be null");

		final Set<String> refsFromProperties = new HashSet<>();

		if (containsPropertiesWithRefs(schema)) {
			for (final var entry : schema.getProperties().entrySet()) {
				final var propSchema = entry.getValue();

				refsFromProperties.addAll(getNonPropertyRefs(propSchema));
			}
		}

		return refsFromProperties;
	}

	private static Set<String> getAllRefs(@Nonnull Schema<?> schema) throws NullPointerException {
		Objects.requireNonNull(schema, "getAllRefs() argument 'schema' must not be null");

		final Set<String> refsFromProperties = new HashSet<>();

		if (containsPropertiesWithRefs(schema)) {
			for (final var entry : schema.getProperties().entrySet()) {
				final var propSchema = entry.getValue();

				refsFromProperties.addAll(getNonPropertyRefs(propSchema));
				refsFromProperties.addAll(getPropertyRefs(propSchema));
			}
		}

		return refsFromProperties;
	}
}
