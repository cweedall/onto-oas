package edu.isi.oba.utils.schema;

/**
 * Represents the types of composite schema constructs supported by OpenAPI 3.0. These are used to
 * combine multiple schema definitions into a single schema.
 *
 * <ul>
 *   <li>{@code ALLOF_LIST} - Corresponds to the OpenAPI {@code allOf} keyword, requiring all
 *       subschemas to be valid.
 *   <li>{@code ANYOF_LIST} - Corresponds to the OpenAPI {@code anyOf} keyword, requiring at least
 *       one subschema to be valid.
 *   <li>{@code ONEOF_LIST} - Corresponds to the OpenAPI {@code oneOf} keyword, requiring exactly
 *       one subschema to be valid.
 * </ul>
 */
enum ComplexSchemaListType {
	ALLOF_LIST,
	ANYOF_LIST,
	ONEOF_LIST
}
