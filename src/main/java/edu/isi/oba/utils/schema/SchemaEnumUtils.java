package edu.isi.oba.utils.schema;

import io.swagger.v3.oas.models.media.Schema;
import java.lang.invoke.MethodHandles;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.iri.IRI;

public class SchemaEnumUtils {

	private SchemaEnumUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/** Handler interface for applying a list of enum values to a {@link Schema} with generic type. */
	@FunctionalInterface
	public interface EnumTypeHandler<T> {
		void apply(Schema<?> schema, List<?> values);
	}

	public static EnumTypeHandler<?> getHandlerForClass(Class<?> clazz) {
		EnumTypeHandler<?> handler = ENUM_TYPE_HANDLERS.get(clazz);
		if (handler == null) {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(
							Level.WARNING,
							"Falling back to DEFAULT_ENUM_HANDLER for unrecognized enum type: "
									+ clazz.getName());
		}
		return handler != null ? handler : DEFAULT_ENUM_HANDLER;
	}

	private static <T> EnumTypeHandler<T> createHandler(
			String type, String format, Class<?> castType) {
		return (schema, values) -> {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(
							Level.FINE,
							"Applying enum values to schema. Type: "
									+ type
									+ ", Format: "
									+ format
									+ ", Value type: "
									+ castType.getSimpleName());
			schema.setType(type);
			if (format != null) schema.setFormat(format);
			castAndCopy(schema, values, castType);
		};
	}

	/** Fallback handler for unknown enum types: treats values as strings */
	private static final EnumTypeHandler<Object> DEFAULT_ENUM_HANDLER =
			(schema, values) -> {
				schema.setType("string");
				castAndCopy(schema, values, String.class);
			};

	/** Enum type handler for known enum types used with {@link Schema}. */
	private static final Map<Class<?>, EnumTypeHandler<?>> ENUM_TYPE_HANDLERS =
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

		if (values == null || values.isEmpty()) {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.WARNING, "No enum values provided. Skipping enum application.");
			return;
		}

		for (Object value : values) {
			if (value != null && !type.isInstance(value)) {
				Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
						.log(
								Level.WARNING,
								"Enum value '" + value + "' is not of expected type: " + type.getName());
			}
		}

		try {
			Schema<T> castedSchema = (Schema<T>) schema;
			List<T> castedValues = (List<T>) values;
			deepCopyEnumValues(castedSchema, castedValues, type);
		} catch (ClassCastException e) {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.SEVERE, "Failed to cast schema or values to type: " + type.getName(), e);
		}
	}
}
