package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.semanticweb.owlapi.model.OWLLiteral;

/**
 * Class for taking an existing {@link Schema} and updating in ways that are generic/shared between
 * object and data properties.
 */
public class MapperProperty {

	/**
	 * Sets the {@link Schema}'s name.
	 *
	 * @param schema a {@link Schema}
	 * @param name a {@link String} indicating the {@link Schema}'s name.
	 */
	public static void setSchemaName(Schema schema, String name) {
		schema.setName(name);
	}

	/**
	 * Sets the {@link Schema}'s description.
	 *
	 * @param schema a {@link Schema}
	 * @param description a {@link String} indicating the {@link Schema}'s name.
	 */
	public static void setSchemaDescription(Schema schema, String description) {
		schema.setDescription(description);
	}

	/**
	 * Sets the {@link Schema}'s type.
	 *
	 * @param schema a {@link Schema}
	 * @param type a {@link String} indicating {@link Schema}'s type.
	 */
	public static void setSchemaType(Schema schema, String type) {
		schema.setType(type);
	}

	/**
	 * Sets the {@link Schema}'s format.
	 *
	 * @param schema a {@link Schema}
	 * @param format a {@link String} indicating {@link Schema}'s format.
	 */
	public static void setSchemaFormat(Schema schema, String format) {
		schema.setFormat(format);
	}

	/**
	 * Sets the {@link Schema}'s default value.
	 *
	 * @param schema a {@link Schema}
	 * @param defaultValue an {@link Object} of the default value.
	 */
	public static void setSchemaDefaultValue(Schema schema, Object defaultValue) {
		schema.setDefault(defaultValue);
	}

	/**
	 * Sets the {@link Schema}'s list of enum values.
	 *
	 * @param schema a {@link Schema}
	 * @param enumValuesList a {@link List} of {@link Object}s of possible values.
	 */
	public static void setSchemaEnums(Schema schema, List<Object> enumValuesList) {
		schema.setEnum(enumValuesList);
	}

	/**
	 * Set the read-only value for a property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param isReadOnly a boolean value indicating read-only or not.
	 */
	public static void setReadOnlyValueForPropertySchema(Schema propertySchema, Boolean isReadOnly) {
		propertySchema.setReadOnly(isReadOnly);
	}

	/**
	 * Set the write-only value for a property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param isReadOnly a boolean value indicating write-only or not.
	 */
	public static void setWriteOnlyValueForPropertySchema(
			Schema propertySchema, Boolean isWriteOnly) {
		propertySchema.setWriteOnly(isWriteOnly);
	}

	/**
	 * Set the nullable value for a property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param isNullable a boolean value indicating nullable or not.
	 */
	public static void setNullableValueForPropertySchema(Schema propertySchema, Boolean isNullable) {
		propertySchema.setNullable(isNullable);
	}

	/**
	 * Convert the class {@link Schema} so that any properties that can be converted from arrays to
	 * non-arrays will be converted. Some properties cannot be converted (e.g. if they require
	 * multiple values) -> these properties are no converted.
	 *
	 * @param classSchemaToConvert a {@link Schema} to perform the conversion on.
	 * @param enumProperties a {@link Set} of {@link String} indicating the (short form) names of
	 *     properties which reference a class/object which is an enum.
	 * @param functionalProperties a {@link Set} of {@link String} indicating the (short form) names
	 *     of properties which are functional.
	 * @return a {@link Schema} with all possible non-array properties converted.
	 */
	public static void convertArrayToNonArrayPropertySchemas(
			Schema classSchemaToConvert,
			Set<String> enumProperties,
			Set<String> functionalProperties,
			Boolean fixSingularPluralPropertyNames) {
		// Keep track of properties that need to be pluralized.  Key is updated/pluralized property
		// name, and Schema value is the original schema.
		final var convertedPropertySchemas = new HashMap<String, Schema>();
		final Map<String, Schema> propertySchemas =
				classSchemaToConvert.getProperties() == null
						? new HashMap<>()
						: classSchemaToConvert.getProperties();

		// Loop through all of the properties and convert as necessary.
		propertySchemas.forEach(
				(propertyName, propertySchema) -> {
					// Only need to convert if the propertySchema is of type "array".
					if ("array".equals(propertySchema.getType())) {
						// Unsure if this should be done, but if the property items are sufficiently complex
						// (e.g. oneOf, allOf, anyOf), do no convert it(??).
						final var itemsSchema = propertySchema.getItems();

						if (itemsSchema != null) {
							final var maxItems = Objects.requireNonNullElse(propertySchema.getMaxItems(), -1);
							final var hasMaxItems = (propertySchema.getMaxItems() != null);
							final var minItems = Objects.requireNonNullElse(propertySchema.getMinItems(), -1);
							final var hasMinItems = (propertySchema.getMinItems() != null);

							boolean isFunctional =
									functionalProperties != null && functionalProperties.contains(propertyName);

							// We don't want to change object properties because the reference still needs to
							// occur within the array of property items.
							boolean isFunctionalObjProp =
									isFunctional && itemsSchema != null && itemsSchema.get$ref() != null;

							// Not currently used, but placeholder in case it is needed later.
							boolean isFunctionalDataProp =
									isFunctional && (itemsSchema == null || itemsSchema.get$ref() == null);

							// Initial indicator for whether the property should remain an array is if functional
							// (should not be / false) or not functional (should be / true);
							boolean shouldBeArray = !isFunctionalDataProp;

							final var hasObjPropReference = itemsSchema != null && itemsSchema.get$ref() != null;

							final var isEnumObjPropReference =
									hasObjPropReference
											&& enumProperties != null
											&& enumProperties.contains(propertyName);

							final var isArrayObjPropReference =
									hasObjPropReference
											&& (!(hasMaxItems || hasMinItems) || minItems > 1 || maxItems > 1);

							if (shouldBeArray) {
								shouldBeArray &= (minItems > 0 || maxItems > 1);

								shouldBeArray &= !(minItems == 1 && maxItems == 1);

								// Keep as array (even if only one item exists), if there is a single reference or
								// allOf/anyOf/oneOf/enum composed schemas are contained within the property's item.
								shouldBeArray |=
										itemsSchema != null
												&& (isArrayObjPropReference
														|| (itemsSchema.getAllOf() != null && !itemsSchema.getAllOf().isEmpty())
														|| (itemsSchema.getAnyOf() != null && !itemsSchema.getAnyOf().isEmpty())
														|| (itemsSchema.getOneOf() != null
																&& !itemsSchema.getOneOf().isEmpty()));
							}

							// By default, everything is an array.  If this property is not, then convert it from
							// an array to a single item.
							if (!shouldBeArray) {
								if (isEnumObjPropReference || hasObjPropReference) {
									propertySchema.setType(null);

									// Copy ref to allOf item.
									final var objRefSchema = new ObjectSchema();
									objRefSchema.set$ref(itemsSchema.get$ref());
									propertySchema.addAllOfItem(objRefSchema);

									/**
									 * The next values should exist and override whatever value the object reference
									 * has. That is why the allOf structure is being used.
									 */

									// Copy readOnly value to allOf item, if applicable.
									if (propertySchema.getReadOnly() != null) {
										final var isReadOnlySchema = new Schema();
										isReadOnlySchema.setReadOnly(propertySchema.getReadOnly());
										propertySchema.addAllOfItem(isReadOnlySchema);
										propertySchema.setReadOnly(null);
									}

									// Copy writeOnly value to allOf item, if applicable.
									if (propertySchema.getWriteOnly() != null) {
										final var isWriteOnlySchema = new Schema();
										isWriteOnlySchema.setWriteOnly(propertySchema.getWriteOnly());
										propertySchema.addAllOfItem(isWriteOnlySchema);
										propertySchema.setWriteOnly(null);
									}

									// Copy nullable value to allOf item, if applicable.
									if (propertySchema.getNullable() != null) {
										final var isNullableSchema = new Schema();
										isNullableSchema.setNullable(propertySchema.getNullable());
										propertySchema.addAllOfItem(isNullableSchema);
										propertySchema.setNullable(null);
									}

									// Copy default value to allOf item, if applicable.
									if (propertySchema.getDefault() != null) {
										final var defaultValueSchema = new Schema();
										defaultValueSchema.setDefault(propertySchema.getDefault());
										propertySchema.addAllOfItem(defaultValueSchema);
										propertySchema.setDefault(null);
									}

									// Copy description to allOf item, if applicable.
									if (propertySchema.getDescription() != null) {
										final var descriptionSchema = new Schema();
										descriptionSchema.setDescription(propertySchema.getDescription());
										propertySchema.addAllOfItem(descriptionSchema);
										propertySchema.setDescription(null);
									}
								} else {
									MapperProperty.setSchemaType(propertySchema, itemsSchema.getType());
									MapperProperty.setSchemaFormat(propertySchema, itemsSchema.getFormat());
									MapperProperty.setSchemaDefaultValue(propertySchema, itemsSchema.getDefault());
									MapperProperty.setSchemaEnums(propertySchema, itemsSchema.getEnum());
									// Anything else?
								}

								// Now clear out the original items.
								propertySchema.setItems(null);

								// Keep track of property names that are plural, but should be singular.
								if (propertyName.equals(ObaUtils.getPluralOf(propertyName))) {
									convertedPropertySchemas.put(
											ObaUtils.getSingularOf(propertyName), propertySchema);
								}
							} else {
								// Keep track of property names that are singular, but should be plural.
								if (!propertyName.equals(ObaUtils.getPluralOf(propertyName))) {
									convertedPropertySchemas.put(ObaUtils.getPluralOf(propertyName), propertySchema);
								}
							}

							// Because non-arrays are allowed by the configuration, we do not need min/max items
							// for an exact configuration of one.
							// NOTE: These values should only be removed if the property is marked as required
							// (via the configuration file).
							// The property *should* be marked required (if applicable) before calling this
							// method!
							if (isFunctional
									|| (!shouldBeArray
											&& classSchemaToConvert.getRequired() != null
											&& classSchemaToConvert.getRequired().contains(propertyName))) {

								if (maxItems == 1) {
									if (minItems == 1) {
										propertySchema.setMaxItems(null);
										propertySchema.setMinItems(null);
									} else {
										propertySchema.setMaxItems(null);

										// Setting nullable can be done on the schema ONLY IF there is not an allOf
										// sub-property.
										// In the latter case, add the nullable value to one of the allOf entries.
										if (propertySchema.getAllOf() != null) {
											var containsNullableSchema = false;

											for (final var allOfItem : propertySchema.getAllOf()) {
												((Schema) allOfItem).setNullable(true);
												containsNullableSchema = true;
											}

											if (!containsNullableSchema) {
												final var isNullableSchema = new Schema();
												isNullableSchema.setNullable(true);
												propertySchema.addAllOfItem(isNullableSchema);
												propertySchema.setNullable(null); // Just in case
											}
										} else {
											MapperProperty.setNullableValueForPropertySchema(propertySchema, true);
										}
									}
								}
							}

							if (!shouldBeArray && minItems < 1 && maxItems == 1) {
								propertySchema.setMinItems(null);
								propertySchema.setMaxItems(null);

								// Setting nullable can be done on the schema ONLY IF there is not an allOf
								// sub-property.
								// In the latter case, add the nullable value to one of the allOf entries.
								if (propertySchema.getAllOf() != null) {
									var containsNullableSchema = false;

									for (final var allOfItem : propertySchema.getAllOf()) {
										((Schema) allOfItem).setNullable(true);
										containsNullableSchema = true;
									}

									if (!containsNullableSchema) {
										final var isNullableSchema = new Schema();
										isNullableSchema.setNullable(true);
										propertySchema.addAllOfItem(isNullableSchema);
										propertySchema.setNullable(null); // Just in case
									}
								} else {
									MapperProperty.setNullableValueForPropertySchema(propertySchema, true);
								}
							}

							if (!isFunctional && minItems < 1) {
								classSchemaToConvert.getRequired().remove(propertyName);
							}
						}
					}
				});

		if (fixSingularPluralPropertyNames != null && fixSingularPluralPropertyNames.booleanValue()) {
			convertedPropertySchemas.forEach(
					(newPropertySchemaName, originalSchema) -> {
						propertySchemas.remove(originalSchema.getName());
						propertySchemas.put(newPropertySchemaName, originalSchema);

						if (classSchemaToConvert.getRequired().contains(originalSchema.getName())) {
							classSchemaToConvert.getRequired().remove(originalSchema.getName());
							classSchemaToConvert.getRequired().add(newPropertySchemaName);
						}

						logger.warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

						if (newPropertySchemaName.equals(ObaUtils.getPluralOf(originalSchema.getName()))) {
							logger.warning(
									"!!! Property \""
											+ originalSchema.getName()
											+ "\" is an array.  Should it be \""
											+ newPropertySchemaName
											+ "\" instead?? !!!");
						} else {
							logger.warning(
									"!!! Property \""
											+ originalSchema.getName()
											+ "\" is not an array.  Should it be \""
											+ newPropertySchemaName
											+ "\" instead?? !!!");
						}

						logger.warning("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					});
		}
	}

	/**
	 * Add a minimum cardinality value to the property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param cardinalityInt a minimum cardinality value.
	 */
	protected static void addMinCardinalityToPropertySchema(
			Schema propertySchema, Integer cardinalityInt) {
		propertySchema.setMinItems(cardinalityInt);

		if (cardinalityInt > 0) {
			MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
		}
	}

	/**
	 * Add a maximum cardinality value to the property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param cardinalityInt a maximum cardinality value.
	 */
	protected static void addMaxCardinalityToPropertySchema(
			Schema propertySchema, Integer cardinalityInt) {
		propertySchema.setMaxItems(cardinalityInt);

		if (cardinalityInt.intValue() < 2) {
			MapperProperty.setNullableValueForPropertySchema(propertySchema, true);
		}
	}

	/**
	 * Add an exact cardinality value to the property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param cardinalityInt an exact cardinality value.
	 */
	protected static void addExactCardinalityToPropertySchema(
			Schema propertySchema, Integer cardinalityInt) {
		propertySchema.setMinItems(cardinalityInt);
		propertySchema.setMaxItems(cardinalityInt);
	}

	/**
	 * Add a "hasValue" value to the property's {@link Schema}.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param hasValue an {@link Object} to add to the enum list. Expect this to be a string ($ref)
	 *     for object properties and the equivalent {@link OWLLiteral} datatype for data properties.
	 */
	protected static void addHasValueOfPropertySchema(Schema propertySchema, Object hasValue) {
		Schema itemsSchema = null;

		if (propertySchema.getItems() == null) {
			itemsSchema = new ComposedSchema();
		} else {
			itemsSchema = propertySchema.getItems();

			// default value and "has value" (i.e. specific enum(s)) takes priority over (and cannot
			// co-occur with) allOf/anyOf/oneOf.
			itemsSchema.setAllOf(null);
			itemsSchema.setAnyOf(null);
			itemsSchema.setOneOf(null);
		}

		// Only set the first value as default, in case there are multiple ones.
		if (itemsSchema.getDefault() == null) {
			itemsSchema.setDefault(hasValue);
		}

		// Only add if no enums already OR it's not contained with the enums yet.
		if (itemsSchema.getEnum() == null || !itemsSchema.getEnum().contains(hasValue)) {
			itemsSchema.addEnumItemObject(hasValue);
			MapperProperty.setSchemaType(itemsSchema, null);

			propertySchema.setItems(itemsSchema);
		}

		// Need to make sure the property's type is "array" because it has items.
		MapperProperty.setSchemaType(propertySchema, "array");
	}

	/**
	 * Set the property's {@link Schema} to indicate that it is functional. NOTE: This is basically a
	 * convenience method for calling {@link #addMaxCardinalityToPropertySchema(Schema, Integer)} with
	 * {@link Integer} value of 1.
	 *
	 * @param propertySchema a (data / object) property {@link Schema}.
	 * @param cardinalityInt a minimum cardinality value.
	 */
	public static void setFunctionalForPropertySchema(Schema propertySchema) {
		MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
		MapperProperty.addMaxCardinalityToPropertySchema(propertySchema, Integer.valueOf(1));
	}
}
