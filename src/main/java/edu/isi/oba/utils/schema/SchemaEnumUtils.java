package edu.isi.oba.utils.schema;

import io.swagger.v3.oas.models.media.Schema;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.apache.jena.iri.IRI;

public class SchemaEnumUtils {
	/** Handler interface for applying a list of enum values to a {@link Schema} with generic type. */
	public interface EnumTypeHandler<T> {
		void apply(Schema<?> schema, List<?> values);
	}

	private static <T> EnumTypeHandler<T> createHandler(
			String type, String format, Class<?> castType) {
		return (schema, values) -> {
			schema.setType(type);
			if (format != null) schema.setFormat(format);
			castAndCopy(schema, values, castType);
		};
	}

	/** Fallback handler for unknown enum types: treats values as strings */
	public static final EnumTypeHandler<Object> DEFAULT_ENUM_HANDLER =
			(schema, values) -> {
				schema.setType("string");
				castAndCopy(schema, values, String.class);
			};

	/** Enum type handler for known enum types used with {@link Schema}. */
	public static final Map<Class<?>, EnumTypeHandler<?>> ENUM_TYPE_HANDLERS =
			Map.ofEntries(
					Map.entry(String.class, createHandler("string", null, String.class)),
					Map.entry(Boolean.class, createHandler("boolean", null, Boolean.class)),
					Map.entry(Integer.class, createHandler("integer", null, Integer.class)),
					Map.entry(Long.class, createHandler("integer", "int64", Long.class)),
					Map.entry(Float.class, createHandler("number", "float", Number.class)),
					Map.entry(Double.class, createHandler("number", "double", Double.class)),
					Map.entry(LocalDate.class, createHandler("string", "date", String.class)),
					Map.entry(OffsetDateTime.class, createHandler("string", "date-time", String.class)),
					Map.entry(ZonedDateTime.class, createHandler("string", "date-time", String.class)),
					Map.entry(URI.class, createHandler("string", "uri", String.class)),
					Map.entry(Inet4Address.class, createHandler("string", "ipv4", String.class)),
					Map.entry(Inet6Address.class, createHandler("string", "ipv6", String.class)),
					Map.entry(InetAddress.class, createHandler("string", null, String.class)),
					Map.entry(IRI.class, createHandler("string", "iri", String.class)));

	private static <T> void deepCopyEnumValues(
			final Schema<T> targetSchema, final List<T> enumValues, final Class<T> enumType) {
		if (targetSchema == null) {
			return;
		}

		if (enumValues != null) {
			if (targetSchema.getEnum() != null) {
				targetSchema.getEnum().clear();
			}

			for (final var enumValue : enumValues) {
				targetSchema.addEnumItemObject(enumValue);
			}
		}
	}

	/**
	 * Helper method cast a {@link Schema} and {@link List} of values to have a consistent generic
	 * type <T>.
	 *
	 * @param <T> a generic type, determined by the {@code type} parameter
	 * @param schema a {@link Schema} of an unknown type
	 * @param values a {@link List} of values an unknown type
	 * @param type a {@link Class} type
	 */
	@SuppressWarnings("unchecked")
	public static <T> void castAndCopy(Schema<?> schema, List<?> values, Class<T> type) {
		Schema<T> castedSchema = (Schema<T>) schema;
		List<T> castedValues = (List<T>) values;
		deepCopyEnumValues(castedSchema, castedValues, type);
	}
}
