package edu.isi.oba;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.ConfigFlagType;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContext;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.DumperOptions;

class Serializer {
	String openapi_path;

	public Serializer(
			Mapper mapper,
			java.nio.file.Path dir,
			OpenAPI openAPI,
			LinkedHashMap<String, PathItem> custom_paths,
			YamlConfig configData)
			throws Exception {
		final var extensions = new HashMap<String, Object>();
		final String openapi_file =
				configData.getConfigFlagValue(ConfigFlagType.GENERATE_JSON_FILE)
						? "openapi.json"
						: "openapi.yaml";

		// Add placeholder.  Paths will be set after the examples.
		openAPI.setPaths(mapper.getPaths());
		final var schemas = mapper.getSchemas();
		final var components = new Components().schemas(schemas);
		openAPI.components(components);
		final var examples = ExamplesGenerator.generateExamples(openAPI);
		openAPI.getComponents().setExamples(examples);

		// Remove existing Tags so that we make sure everything is in alphabetical order with
		// the "tags" Set<Tag>.
		openAPI.setTags(null);

		// Gather all tags from the path items.
		final var tags = new HashSet<Tag>();
		openAPI
				.getPaths()
				.forEach(
						(k, v) -> {
							if (openAPI.getTags() != null) {
								// Copy the List of OpenAPI tags (defined in the configuration file, if at all).
								tags.addAll(openAPI.getTags().stream().collect(Collectors.toSet()));
							}

							// For each operation, grab any Tags that exist and add them to the Set of Tags.
							v.readOperationsMap()
									.forEach(
											(httpMethod, operation) -> {
												operation.getTags().stream()
														.forEach(
																(operationTagName) -> {
																	final var tagObj = new Tag();
																	tagObj.setName(operationTagName);

																	// There should always be one path/endpoint tag which is the
																	// schema's name.
																	// Getting the tags from the operation only returns a List<String>
																	// (where String is the name of the Tag).
																	// This appears to be a quirk between the operation tags and the
																	// global tags which have a description and externalUrl.
																	// This grabs the schema's description by searching for the
																	// schema's name.
																	var tagDescription = "";
																	if (schemas != null && schemas.get(operationTagName) != null) {
																		tagDescription = schemas.get(operationTagName).getDescription();
																	}

																	// Use a generic description if one was not found.
																	if (tagDescription == null || tagDescription.isBlank()) {
																		tagObj.setDescription(
																				operationTagName + " description not set in the ontology.");
																	} else {
																		tagObj.setDescription(tagDescription);
																	}

																	tags.add(tagObj);
																});
											});
						});

		// Convert Set to List and sort in alphabetical order (by Tag's name).
		openAPI.setTags(
				tags.stream()
						.sorted((tag1, tag2) -> tag1.getName().compareToIgnoreCase(tag2.getName()))
						.collect(Collectors.toList()));

		// Generate security schema
		final var securitySchemes = new HashMap<String, SecurityScheme>();
		final var securityScheme = getSecurityScheme(extensions);
		securitySchemes.put("BearerAuth", securityScheme);
		components.securitySchemes(securitySchemes);

		// add custom paths
		final var custom_extensions = new HashMap<String, Object>();
		custom_extensions.put("x-oba-custom", true);

		if (custom_paths != null)
			custom_paths.forEach(
					(k, v) -> {
						System.out.println("inserting custom query " + k);
						v.setExtensions(custom_extensions);
						openAPI.getPaths().addPathItem(k, v);
					});

		openAPI.setPaths(ExamplesGenerator.generatePathExamples(openAPI.getPaths(), examples));

		// Don't use .sortedOutput(true) because we are using SortedSchemaMixin to alphabetically sort
		// the desired entries.  Sorting _everything_ alphabetically messes up the YAML file by moving
		// the info section away from the top of the document, for example.
		final var openApiConfiguration = new SwaggerConfiguration().openAPI(openAPI).prettyPrint(true);

		final var ctx = new JaxrsOpenApiContext<>().openApiConfiguration(openApiConfiguration).init();

		// ctx.getOutputJsonMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
		ctx.getOutputJsonMapper().configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true);
		ctx.getOutputJsonMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
		ctx.getOutputJsonMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		ctx.getOutputJsonMapper().configure(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX, true);
		ctx.getOutputJsonMapper().addMixIn(Schema.class, SortedSchemaMixin.class);

		// ctx.getOutputYamlMapper().configure(MapperFeature.USE_ANNOTATIONS, true);
		ctx.getOutputYamlMapper().configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true);
		ctx.getOutputYamlMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
		ctx.getOutputYamlMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		ctx.getOutputYamlMapper().configure(SerializationFeature.WRITE_ENUM_KEYS_USING_INDEX, true);
		ctx.getOutputYamlMapper().addMixIn(Schema.class, SortedSchemaMixin.class);

		final var dumperOptions = new DumperOptions();
		// dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.LITERAL);
		dumperOptions.setPrettyFlow(true);
		dumperOptions.setProcessComments(true);
		dumperOptions.setSplitLines(false);

		YAMLFactory factory = (YAMLFactory) Yaml.mapper().getFactory();
		// factory.rebuild().dumperOptions(dumperOptions);
		factory
				.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
				.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
				.disable(YAMLGenerator.Feature.SPLIT_LINES)
				.enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
				.enable(YAMLGenerator.Feature.INDENT_ARRAYS)
				.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
				.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
		factory.rebuild().dumperOptions(dumperOptions);

		final var pretty = Yaml.pretty(openAPI);

		// write the filename
		final var content =
				configData.getConfigFlagValue(ConfigFlagType.GENERATE_JSON_FILE)
						? ctx.getOutputJsonMapper()
								.writer(new DefaultPrettyPrinter())
								.writeValueAsString(openAPI)
						: ctx.getOutputYamlMapper()
								.writer(new DefaultPrettyPrinter())
								.writeValueAsString(openAPI);
		this.openapi_path = dir + File.separator + openapi_file;
		final var file = new File(openapi_path);
		final var writer =
				Files.newBufferedWriter(
						file.toPath(),
						StandardCharsets.UTF_8,
						StandardOpenOption.WRITE,
						StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
		// writer.write(content);
		writer.write(pretty);
		writer.close();

		if (configData.getConfigFlagValue(ConfigFlagType.VALIDATE_GENERATED_OPENAPI_FILE)) {
			this.validate();
		}
	}

	private void validate() throws Exception {
		final var options = new ParseOptions();
		options.setResolve(true);
		final var result = new OpenAPIV3Parser().readLocation(openapi_path, null, options);
		final var messageList = result.getMessages();
		final var errors = new HashSet<String>(messageList);
		final var warnings = new HashSet<String>();
		errors.forEach(
				(errorMessage) -> {
					if (errorMessage.contains(".$ref target #/components/schemas/")
							&& errorMessage.contains("is not of expected type Examples")) {
						// Ignore.  The validator complains because the reference is to a Schemas item (which is
						// valid) instead of an Examples item.
					} else {
						throw new RuntimeException(errorMessage);
					}
				});
	}

	private SecurityScheme getSecurityScheme(Map<String, Object> extensions) {
		extensions.put("x-bearerInfoFunc", "openapi_server.controllers.user_controller.decode_token");
		final var securityScheme = new SecurityScheme();
		securityScheme.setType(SecurityScheme.Type.HTTP);
		securityScheme.bearerFormat("JWT");
		securityScheme.setScheme("bearer");
		securityScheme.setExtensions(extensions);
		return securityScheme;
	}

	/** The interface Sorted schema mixin. */
	@JsonPropertyOrder(
			value = {
				"description",
				"required",
				"minItems",
				"maxItems",
				"type",
				"format",
				"nullable",
				"default",
				"enum",
				"readOnly",
				"writeOnly"
			},
			alphabetic = true)
	abstract static class SortedSchemaMixin {

		@JsonAnyGetter
		@JsonPropertyOrder(alphabetic = true)
		public abstract Map<String, Object> getExtensions();

		@JsonAnySetter
		public abstract void addExtension(String name, Object value);

		@JsonIgnore
		public abstract boolean getExampleSetFlag();

		@JsonInclude(value = JsonInclude.Include.NON_NULL, content = JsonInclude.Include.ALWAYS)
		public abstract Object getExample();

		@JsonIgnore
		public abstract Map<String, Object> getJsonSchema();

		@JsonIgnore
		public abstract BigDecimal getExclusiveMinimumValue();

		@JsonIgnore
		public abstract BigDecimal getExclusiveMaximumValue();

		@JsonIgnore
		public abstract Map<String, Schema> getPatternProperties();

		@JsonIgnore
		public abstract Schema getContains();

		@JsonIgnore
		public abstract String get$id();

		// TODO: Figure out if anchors for enums can be supported.
		@JsonIgnore
		public abstract String get$anchor();

		@JsonIgnore
		public abstract String get$schema();

		@JsonIgnore
		public abstract Set<String> getTypes();

		@JsonIgnore
		public abstract Object getJsonSchemaImpl();

		@JsonIgnore
		public abstract List<Schema> getPrefixItems();

		@JsonIgnore
		public abstract String getContentEncoding();

		@JsonIgnore
		public abstract String getContentMediaType();

		@JsonIgnore
		public abstract Schema getContentSchema();

		@JsonIgnore
		public abstract Schema getPropertyNames();

		@JsonIgnore
		public abstract Object getUnevaluatedProperties();

		@JsonIgnore
		public abstract Integer getMaxContains();

		@JsonIgnore
		public abstract Integer getMinContains();

		@JsonIgnore
		public abstract Schema getAdditionalItems();

		@JsonIgnore
		public abstract Schema getUnevaluatedItems();

		@JsonIgnore
		public abstract Schema getIf();

		@JsonIgnore
		public abstract Schema getElse();

		@JsonIgnore
		public abstract Schema getThen();

		@JsonIgnore
		public abstract Map<String, Schema> getDependentSchemas();

		@JsonIgnore
		public abstract Map<String, List<String>> getDependentRequired();

		@JsonIgnore
		public abstract String get$comment();

		@JsonIgnore
		public abstract List<Object> getExamples();

		@JsonIgnore
		public abstract Object getConst();

		@JsonIgnore
		public abstract Boolean getBooleanSchemaValue();
	}
}
