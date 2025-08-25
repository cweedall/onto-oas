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
 * Represents specific OpenAPI format strings that provide additional semantic meaning to a base
 * schema type (typically {@code string}, {@code integer}, or {@code number}).
 *
 * <p>This enum is used to enrich OpenAPI schema definitions with format metadata where applicable.
 * Formats help tools like Swagger UI, code generators, and validators interpret the data more
 * precisely.
 *
 * <p>Some Java types map to OpenAPI types that do not have a corresponding format string. In such
 * cases, the format is set to {@code null} to indicate that no specific format is defined by the
 * OpenAPI specification.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code Long.class} → {@code integer} with format {@code int64}
 *   <li>{@code Float.class} → {@code number} with format {@code float}
 *   <li>{@code String.class} → {@code string} with no format (hence {@code null})
 *   <li>{@code Boolean.class} → {@code boolean} with no format (hence {@code null})
 * </ul>
 *
 * <p>Note: This enum is typically used in conjunction with {@link SchemaType} to fully describe
 * both the type and format of a schema property.
 */
enum SchemaFormatType {
	FLOAT,
	INT64,
	DOUBLE,
	DATE,
	DATE_TIME,
	URI,
	IPV4,
	IPV6,
	IRI;

	private static final Map<Class<?>, SchemaFormatType> BY_CLASS_TYPE = new HashMap<>();

	static {
		// Booleans
		BY_CLASS_TYPE.put(Boolean.class, null);
		// Integers
		BY_CLASS_TYPE.put(Integer.class, null);
		BY_CLASS_TYPE.put(Long.class, INT64);
		// Numbers
		BY_CLASS_TYPE.put(Double.class, DOUBLE);
		BY_CLASS_TYPE.put(Float.class, FLOAT);
		BY_CLASS_TYPE.put(Number.class, null);
		// Strings
		BY_CLASS_TYPE.put(String.class, null);
		BY_CLASS_TYPE.put(LocalDate.class, DATE);
		BY_CLASS_TYPE.put(OffsetDateTime.class, DATE_TIME);
		BY_CLASS_TYPE.put(ZonedDateTime.class, DATE_TIME);
		BY_CLASS_TYPE.put(URI.class, URI);
		BY_CLASS_TYPE.put(Inet4Address.class, IPV4);
		BY_CLASS_TYPE.put(Inet6Address.class, IPV6);
		BY_CLASS_TYPE.put(InetAddress.class, null);
		BY_CLASS_TYPE.put(IRI.class, IRI);
		// Objects
		BY_CLASS_TYPE.put(Object.class, null);
	}

	/**
	 * Returns the corresponding {@link SchemaFormatType} for a given Java class.
	 *
	 * @param classType the Java class to map
	 * @param <T> the type of the class
	 * @return the corresponding {@code SchemaFormatType}, or {@code null} if not mapped
	 */
	public static <T> SchemaFormatType valueOfClass(Class<T> classType) {
		return BY_CLASS_TYPE.get(classType);
	}
}
