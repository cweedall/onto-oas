package edu.isi.oba.ontology.schema;

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
			DefaultPropertyFactory.getDefaultProperties(config).forEach(basicClassSchema::addProperty);
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
