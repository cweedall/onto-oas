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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.iri.IRI;

public class ComplexSchemaListUtils {

	private ComplexSchemaListUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	public interface ComplexListTypeHandler<T> {
		void apply(Schema<?> schema, List<?> values, ComplexSchemaListType listType);
	}

	public static ComplexListTypeHandler<?> getHandlerForClass(Class<?> clazz) {
		ComplexListTypeHandler<?> handler = TYPE_HANDLERS.get(clazz);
		if (handler == null) {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(
							Level.WARNING,
							"Falling back to DEFAULT_TYPE_HANDLER for unrecognized complex schema list type: "
									+ clazz.getName());
		}
		return handler != null ? handler : DEFAULT_TYPE_HANDLER;
	}

	/**
	 * Fallback handler for a complex (allOf, anyOf, oneOf) List of unknown Schema types: treats
	 * values as strings
	 */
	public static final ComplexListTypeHandler<Object> DEFAULT_TYPE_HANDLER =
			(schema, values, listType) -> {
				castAndCopy(schema, values, String.class, listType);
			};

	public static final Map<Class<?>, ComplexListTypeHandler<?>> TYPE_HANDLERS = new HashMap<>();

	static {
		TYPE_HANDLERS.put(String.class, createHandler(String.class));
		TYPE_HANDLERS.put(Integer.class, createHandler(Integer.class));
		TYPE_HANDLERS.put(Long.class, createHandler(Long.class));
		TYPE_HANDLERS.put(Boolean.class, createHandler(Boolean.class));
		TYPE_HANDLERS.put(Double.class, createHandler(Double.class));
		TYPE_HANDLERS.put(Float.class, createHandler(Float.class));
		TYPE_HANDLERS.put(Number.class, createHandler(Number.class));
		TYPE_HANDLERS.put(LocalDate.class, createHandler(LocalDate.class));
		TYPE_HANDLERS.put(OffsetDateTime.class, createHandler(OffsetDateTime.class));
		TYPE_HANDLERS.put(ZonedDateTime.class, createHandler(ZonedDateTime.class));
		TYPE_HANDLERS.put(URI.class, createHandler(URI.class));
		TYPE_HANDLERS.put(Inet4Address.class, createHandler(Inet4Address.class));
		TYPE_HANDLERS.put(Inet6Address.class, createHandler(Inet6Address.class));
		TYPE_HANDLERS.put(InetAddress.class, createHandler(InetAddress.class));
		TYPE_HANDLERS.put(IRI.class, createHandler(IRI.class));
		TYPE_HANDLERS.put(Object.class, createHandler(Object.class));
	}

	private static <T> ComplexListTypeHandler<T> createHandler(Class<T> classType) {
		return (schema, values, listType) -> castAndCopy(schema, values, classType, listType);
	}

	/**
	 * Helper method cast a {@link Schema} and {@link List} of values to have a consistent generic
	 * type <T>.
	 *
	 * @param <T> a generic type, determined by the {@code type} parameter
	 * @param schema a {@link Schema} of an unknown type
	 * @param values a {@link List} of values an unknown type
	 * @param type a {@link Class} type
	 * @param listType a {@link ComplexSchemaListType} type
	 */
	@SuppressWarnings("unchecked")
	public static <T> void castAndCopy(
			Schema<?> schema, List<?> values, Class<T> classType, ComplexSchemaListType listType) {
		try {
			Schema<T> castedSchema = (Schema<T>) schema;
			List<T> castedValues = (List<T>) values;
			deepCopySchemaListValues(castedSchema, castedValues, classType, listType);
		} catch (ClassCastException e) {
			Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName())
					.log(Level.SEVERE, "Failed to cast schema or values to type: " + classType.getName(), e);
		}
	}

	private static <T> void deepCopySchemaListValues(
			final Schema<T> targetSchema,
			final List<?> values,
			final Class<T> classType,
			ComplexSchemaListType listType) {
		if (targetSchema == null) {
			return;
		}

		switch (listType) {
			case ALLOF_LIST:
				if (values != null) {
					if (targetSchema.getAllOf() != null) {
						targetSchema.getAllOf().clear();
					}

					for (final var allOfValue : values) {
						targetSchema.addAllOfItem((Schema<?>) allOfValue);
					}
				}
				break;
			case ANYOF_LIST:
				if (values != null) {
					if (targetSchema.getAnyOf() != null) {
						targetSchema.getAnyOf().clear();
					}

					for (final var anyOfValue : values) {
						targetSchema.addAnyOfItem((Schema<?>) anyOfValue);
					}
				}
				break;
			case ONEOF_LIST:
				if (values != null) {
					if (targetSchema.getOneOf() != null) {
						targetSchema.getOneOf().clear();
					}

					for (final var oneOfValue : values) {
						targetSchema.addOneOfItem((Schema<?>) oneOfValue);
					}
				}
				break;
		}
	}

	public static <T> String getType(final Class<T> classType) {
		if (classType == null) {
			return null;
		} else {
			final var type = SchemaType.valueOfClass(classType);
			if (type == null) return null;

			return type.name().toLowerCase();
		}
	}

	public static <T> String getFormat(final Class<T> classType) {
		if (classType == null) {
			return null;
		} else {
			final var formatType = SchemaFormatType.valueOfClass(classType);
			if (formatType == null) return null;

			return formatType.name().toLowerCase().replaceAll("_", "-");
		}
	}
}
