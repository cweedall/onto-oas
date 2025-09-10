package edu.isi.oba.ontology.schema;

import edu.isi.oba.MapperDataProperty;
import edu.isi.oba.MapperProperty;
import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.exceptions.InvalidOntologyFormatException;
import edu.isi.oba.utils.ontology.OntologyDescriptionUtils;
import io.swagger.v3.oas.models.media.Schema;
import java.util.*;
import java.util.stream.Collectors;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

/** Utility class for building OpenAPI Schemas from OWL classes. */
public class SchemaBuilder {

	private SchemaBuilder() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Builds a basic OpenAPI schema for a given OWL class.
	 *
	 * @param baseClass The OWL class to generate a schema for.
	 * @param ontology The ontology containing the class.
	 * @param config The configuration object controlling schema generation.
	 * @return A Swagger/OpenAPI Schema object representing the OWL class.
	 */
	public static Schema getBaseClassBasicSchema(
			OWLClass baseClass, OWLOntology ontology, YamlConfig config) {
		final var basicClassSchema = new Schema<>();
		MapperProperty.setSchemaName(basicClassSchema, getPrefixedSchemaName(baseClass, ontology));
		MapperProperty.setSchemaDescription(
				basicClassSchema,
				OntologyDescriptionUtils.getDescription(
								baseClass, ontology, GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS))
						.orElse(null));
		MapperProperty.setSchemaType(basicClassSchema, "object");

		if (GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_PROPERTIES)) {
			getDefaultProperties(config).forEach(basicClassSchema::addProperty);
		}

		return basicClassSchema;
	}

	/**
	 * Generates a prefixed schema name for an OWL class using ontology prefix mappings.
	 *
	 * @param owlClass The OWL class whose name is to be prefixed.
	 * @param ontology The ontology containing prefix mappings.
	 * @return A string representing the prefixed schema name.
	 * @throws InvalidOntologyFormatException
	 */
	public static String getPrefixedSchemaName(OWLClass owlClass, OWLOntology ontology)
			throws InvalidOntologyFormatException {
		final var classIRI = owlClass.getIRI();
		final var classIRIAsString = classIRI.toString();
		final var classPrefix = classIRIAsString.replaceAll(classIRI.getShortForm() + "$", "");

		var prefixedSchemaName = owlClass.getIRI().getShortForm();

		final var format = ontology.getOWLOntologyManager().getOntologyFormat(ontology);
		if (format != null && format.isPrefixOWLDocumentFormat()) {
			final var map = format.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
			for (final var entry : map.entrySet()) {
				if (entry.getValue().equals(classPrefix)) {
					final var prefixName = entry.getKey().replaceAll(":", "");
					if (!"".equals(prefixName)) {
						prefixedSchemaName = prefixName + "-" + prefixedSchemaName;
					}
				}
			}
		} else {
			throw new InvalidOntologyFormatException(
					"Ontology has an invalid or null prefix document format. Unable to proceed.");
		}

		return prefixedSchemaName;
	}

	/**
	 * Get default schema properties.
	 *
	 * <p>These can be disabled by setting `default_properties` to `false` in the `config.yaml` file.
	 *
	 * @param config The configuration object controlling default property inclusion.
	 * @return A Map where key is property name and value is the property's Swagger/OpenAPI Schema.
	 */
	public static Map<String, Schema> getDefaultProperties(YamlConfig config) {
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

	/**
	 * Sets required properties for a class schema based on functional properties and schema
	 * constraints.
	 *
	 * @param classSchema The schema to update with required properties.
	 * @param functionalProperties A set of property names considered functional (i.e., required).
	 */
	public static void generateRequiredPropertiesForClassSchemas(
			Schema classSchema, Set<String> functionalProperties) {
		final Map<String, Schema> propertySchemas =
				classSchema.getProperties() == null ? new HashMap<>() : classSchema.getProperties();

		final Set<String> requiredProperties = new HashSet<>();

		propertySchemas.forEach(
				(propertyName, propertySchema) -> {
					if (propertySchema.getMinItems() != null && propertySchema.getMinItems() > 0) {
						MapperProperty.setNullableValueForPropertySchema(propertySchema, false);
						requiredProperties.add(propertyName);
					} else {
						MapperProperty.setNullableValueForPropertySchema(propertySchema, true);
						if (functionalProperties.contains(propertyName)) {
							requiredProperties.add(propertyName);
						}
					}
				});

		classSchema.setRequired(requiredProperties.stream().collect(Collectors.toList()));
	}
}
