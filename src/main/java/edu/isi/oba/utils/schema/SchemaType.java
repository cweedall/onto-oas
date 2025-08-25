package edu.isi.oba.utils.schema;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.iri.IRI;

/**
 * Represents the basic OpenAPI data types that can be assigned to a {@link
 * io.swagger.v3.oas.models.media.Schema}. This enum provides a mapping from Java types to OpenAPI
 * types.
 *
 * <ul>
 *   <li>{@code STRING} - Used for textual data, including dates, URIs, and IP addresses.
 *   <li>{@code NUMBER} - Used for floating-point numbers (e.g., {@code float}, {@code double}).
 *   <li>{@code INTEGER} - Used for whole numbers (e.g., {@code int}, {@code long}).
 *   <li>{@code BOOLEAN} - Used for {@code true}/{@code false} values.
 *   <li>{@code ARRAY} - Used for array types (not directly mapped from Java types here).
 *   <li>{@code OBJECT} - Used for complex objects or untyped values (e.g., {@code Object.class}).
 * </ul>
 */
enum SchemaType {
	STRING,
	NUMBER,
	INTEGER,
	BOOLEAN,
	ARRAY,
	OBJECT;

	private static final Map<Class<?>, SchemaType> BY_CLASS_TYPE = new HashMap<>();

	static {
		// Booleans
		BY_CLASS_TYPE.put(Boolean.class, BOOLEAN);
		// Integers
		BY_CLASS_TYPE.put(Integer.class, INTEGER);
		BY_CLASS_TYPE.put(Long.class, INTEGER);
		// Numbers
		BY_CLASS_TYPE.put(Double.class, NUMBER);
		BY_CLASS_TYPE.put(Float.class, NUMBER);
		BY_CLASS_TYPE.put(Number.class, NUMBER);
		// Strings
		BY_CLASS_TYPE.put(String.class, STRING);
		BY_CLASS_TYPE.put(LocalDate.class, STRING);
		BY_CLASS_TYPE.put(OffsetDateTime.class, STRING);
		BY_CLASS_TYPE.put(ZonedDateTime.class, STRING);
		BY_CLASS_TYPE.put(URI.class, STRING);
		BY_CLASS_TYPE.put(Inet4Address.class, STRING);
		BY_CLASS_TYPE.put(Inet6Address.class, STRING);
		BY_CLASS_TYPE.put(InetAddress.class, STRING);
		BY_CLASS_TYPE.put(IRI.class, STRING);
		// Objects
		BY_CLASS_TYPE.put(Object.class, OBJECT);
	}

	/**
	 * Returns the corresponding {@link SchemaType} for a given Java class.
	 *
	 * @param classType the Java class to map
	 * @param <T> the type of the class
	 * @return the corresponding {@code SchemaType}, or {@code null} if not mapped
	 */
	public static <T> SchemaType valueOfClass(Class<T> classType) {
		return BY_CLASS_TYPE.get(classType);
	}
}
