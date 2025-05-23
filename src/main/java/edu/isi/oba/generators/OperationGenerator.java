package edu.isi.oba.generators;

import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.config.paths.OperationType;
import edu.isi.oba.config.paths.PathKeyType;
import edu.isi.oba.utils.StringUtils;
import io.swagger.models.Method;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.semanticweb.owlapi.model.IRI;

public class OperationGenerator {
	/**
	 * Generate a Swagger Operation object based on the schema, specified method/operation,
	 * cardinality, and settings in the configuration file.
	 *
	 * @param schema a Swagger {@link Schema}.
	 * @param schemaIRI the {@link Schema}'s {@link IRI}.
	 * @param method the HTTP method/verb (e.g. DELETE, GET, PUT, POST) specified by {@link
	 *     HttpMethod}.
	 * @param cardinality the {@link CardinalityType} indicator for whether there is one or more
	 *     records involved with the method/operation.
	 * @param configData the {@link YamlConfig} configuration settings for conversion.
	 * @return a Swagger {@link Operation}.
	 */
	public static Operation generateOperation(
			Schema schema, IRI schemaIRI, OperationType operationType, YamlConfig configData) {
		final var method = operationType.getHttpMethod();
		final var cardinality = operationType.getCardinalityType();

		// Create new Operation object for the given Schema and the specified method / cardinality.
		final var operation =
				OperationGenerator.getOperationTemplate(schema, schemaIRI, operationType, configData);

		final var parameters = new HashSet<Parameter>();
		final var apiResponses = new ApiResponses();

		final var auth = configData.getAuth() == null ? false : configData.getAuth().getEnable();

		// Set key parameter, if configured.
		parameters.addAll(OperationGenerator.setPathKeyParameter(operationType, configData));

		// Depending on the method, get parameters, request body, and/or responses.
		switch (method) {
			case DELETE:
				parameters.addAll(OperationGenerator.getDeleteParameters()); // Placeholder
				apiResponses.putAll(OperationGenerator.getDeleteResponses(configData));
				break;
			case GET:
				parameters.addAll(OperationGenerator.getGetParameters(cardinality, configData));
				operation.setRequestBody(
						OperationGenerator.getGetRequestBody(schema, schemaIRI)); // Placeholder
				apiResponses.putAll(OperationGenerator.getGetResponses(schema, cardinality, configData));
				break;
			case PATCH:
				parameters.addAll(OperationGenerator.getPatchParameters()); // Placeholder
				operation.setRequestBody(
						OperationGenerator.getPatchRequestBody(schema, schemaIRI)); // Placeholder
				apiResponses.putAll(OperationGenerator.getPatchResponses()); // Placeholder
				break;
			case POST:
				parameters.addAll(OperationGenerator.getPostParameters()); // Placeholder
				operation.setRequestBody(OperationGenerator.getPostRequestBody(schema, schemaIRI));
				apiResponses.putAll(OperationGenerator.getPostResponses(configData));
				break;
			case PUT:
				parameters.addAll(OperationGenerator.getPutParameters()); // Placeholder
				operation.setRequestBody(OperationGenerator.getPutRequestBody(schema, schemaIRI));
				apiResponses.putAll(OperationGenerator.getPutResponses(configData));
				break;
			case SEARCH: // Currently - only supports searches via the POST operation (e.g. POST
				// /{resource-name}/_search).
				parameters.addAll(OperationGenerator.getSearchByPostParameters()); // Placeholder
				operation.setRequestBody(
						OperationGenerator.getSearchByPostRequestBody(schema, schemaIRI, configData));
				apiResponses.putAll(
						OperationGenerator.getSearchByPostResponses(schema, cardinality, configData));
				break;
			default:
				break;
		}

		if (auth) {
			SecurityRequirement securityRequirement = new SecurityRequirement();
			securityRequirement.addList("BearerAuth");
			operation.addSecurityItem(securityRequirement);

			if (Set.of(Method.PATCH, Method.PUT, Method.POST, Method.DELETE).contains(method)) {
				parameters.add(
						new QueryParameter()
								.description("Username")
								.name("user")
								.required(false)
								.schema(new StringSchema()));
			}
		}

		// Save parameters to the operation.  If no parameters, set operation parameters to null.
		if (parameters.isEmpty()) {
			operation.setParameters(null);
		} else {
			operation.setParameters(parameters.stream().collect(Collectors.toList()));
		}

		// Save responses to the operation.  If no responses, set operation responses to null.
		if (apiResponses.isEmpty()) {
			operation.setResponses(null);
		} else {
			operation.setResponses(apiResponses);
		}

		operation.setOperationId(OperationGenerator.getOperationId(schema, operationType, configData));

		return operation;
	}

	private static String getOperationId(
			Schema schema, OperationType operationType, YamlConfig configData) {
		final var operationId =
				StringUtils.getLowerCasePluralOf(
								StringUtils.pascalCaseToKebabCase(operationType.name() + "-" + schema.getName()))
						.replaceAll("_", "-");

		return operationId;
	}

	private static Operation getOperationTemplate(
			Schema schema, IRI schemaIRI, OperationType operationType, YamlConfig configData) {
		final var schemaName = schema.getName();
		final var iriString = schemaIRI.getIRIString();
		final var pathIdNameInText = OperationGenerator.getPathKeyNameInText(operationType, configData);

		String operationDescription = null;
		String operationSummary = null;

		final var cardinality = operationType.getCardinalityType();

		switch (operationType.getHttpMethod()) {
			case DELETE:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Delete an instance of ["
									+ schemaName
									+ "]("
									+ iriString
									+ ") by its "
									+ pathIdNameInText
									+ ".";
					operationSummary = "Delete one " + schemaName + " record";
				}
				break;
			case GET:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Gets an instance of ["
									+ schemaName
									+ "]("
									+ iriString
									+ ") by its "
									+ pathIdNameInText
									+ ".";
					operationSummary = "Get one " + schemaName + " record";
				} else {
					operationDescription =
							"Gets all instances of [" + schemaName + "](" + iriString + ")" + ".";
					operationSummary = "Gets all " + schemaName + " record(s)";
				}
				break;
			case PATCH:
				operationDescription = null;
				operationSummary = null;
				break;
			case POST:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Create a new instance of [" + schemaName + "](" + iriString + ")" + ".";
					operationSummary = "Create one " + schemaName + " record";
				} else {
					operationDescription =
							"Create one or more new instances of [" + schemaName + "](" + iriString + ")" + ".";
					operationSummary = "Create one or more " + schemaName + " record(s)";
				}

				break;
			case PUT:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Update an existing instance of ["
									+ schemaName
									+ "]("
									+ iriString
									+ ") by its "
									+ pathIdNameInText
									+ ".";
					operationSummary = "Update an existing " + schemaName + " record";
				} else {
					operationDescription =
							"Update one or more instances of [" + schemaName + "](" + iriString + ").";
					operationSummary = "Update one or more " + schemaName + " record(s)";
				}
				break;
			case SEARCH:
				// Should never be SINGULAR
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription = null;
					operationSummary = null;
				} else {
					operationDescription =
							"Search for instances of  [" + schemaName + "](" + iriString + ")" + ".";
					operationSummary = "Search for one or more " + schemaName + " record(s)";
				}
				break;
			default:
				break;
		}

		return new Operation()
				.description(operationDescription)
				.summary(operationSummary)
				.addTagsItem(schemaName);
	}

	private static Schema getSchemaBasedOnPathKeyType(PathKeyType keyType) {
		switch (keyType) {
			case STRING:
				return new StringSchema();
			case NUMBER:
				return new NumberSchema();
			case INTEGER:
				return new IntegerSchema();
			case BOOLEAN:
				return new BooleanSchema();
			default:
				return new Schema();
		}
	}

	private static String getPathKeyName(OperationType operationType, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(operationType.getCardinalityType())) {
			final var pathConfig = configData.getPathConfig();

			switch (operationType.getHttpMethod()) {
				case GET:
					return pathConfig.getGetPaths().getByKey.getKeyName();
				case PUT:
					return pathConfig.getPutPaths().putByKey.getKeyName();
				case DELETE:
					return pathConfig.getDeletePaths().deleteByKey.getKeyName();
				default:
					break;
			}
		}

		return null;
	}

	private static String getPathKeyNameInText(OperationType operationType, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(operationType.getCardinalityType())) {
			final var pathConfig = configData.getPathConfig();

			switch (operationType.getHttpMethod()) {
				case GET:
					return pathConfig.getGetPaths().getByKey.getKeyNameInText();
				case PUT:
					return pathConfig.getPutPaths().putByKey.getKeyNameInText();
				case DELETE:
					return pathConfig.getDeletePaths().deleteByKey.getKeyNameInText();
				default:
					break;
			}
		}

		return null;
	}

	private static PathKeyType getPathKeyType(OperationType operationType, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(operationType.getCardinalityType())) {
			final var pathConfig = configData.getPathConfig();

			switch (operationType.getHttpMethod()) {
				case DELETE:
					return pathConfig.getDeletePaths().deleteByKey.getKeyType();
				case GET:
					return pathConfig.getGetPaths().getByKey.getKeyType();
				case PUT:
					return pathConfig.getPutPaths().putByKey.getKeyType();
				default:
					break;
			}
		}

		return null;
	}

	private static Set<Parameter> setPathKeyParameter(
			OperationType operationType, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(operationType.getCardinalityType())) {
			final var keyName = OperationGenerator.getPathKeyName(operationType, configData);
			final var keyNameInText = OperationGenerator.getPathKeyNameInText(operationType, configData);

			if (keyName != null) {
				final var descriptionText = "The " + keyNameInText + " of the endpoint resource.";
				return new HashSet<Parameter>() {
					{
						add(
								new PathParameter()
										.description(descriptionText)
										.name(keyName)
										.required(true)
										.schema(
												OperationGenerator.getSchemaBasedOnPathKeyType(
														OperationGenerator.getPathKeyType(operationType, configData))));
					}
				};
			}
		}

		return new HashSet<Parameter>();
	}

	private static Set<Parameter> getDeleteParameters() {
		// TODO: implement
		return new HashSet<Parameter>();
	}

	private static ApiResponses getDeleteResponses(YamlConfig configData) {
		if (GlobalFlags.getFlag(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			return new ApiResponses()
					.addApiResponse(
							"204",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(204, Locale.ENGLISH)))
					.addApiResponse(
							"400",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(400, Locale.ENGLISH)))
					.addApiResponse(
							"401",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(401, Locale.ENGLISH)))
					.addApiResponse(
							"403",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(403, Locale.ENGLISH)))
					.addApiResponse(
							"500",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(500, Locale.ENGLISH)));
		}

		return new ApiResponses();
	}

	private static Set<Parameter> getGetParameters(
			CardinalityType cardinality, YamlConfig configData) {
		final var parameters = new HashSet<Parameter>();
		// Set parameters
		if (configData.getAuth().getEnable()) {
			parameters.add(
					new QueryParameter()
							.name("username")
							.description("Name of the user graph to query")
							.required(false)
							.schema(new StringSchema()));
		}

		switch (cardinality) {
			case PLURAL:
				{
					parameters.add(
							new QueryParameter()
									.name("label")
									.description("Filter by label")
									.required(false)
									.schema(new StringSchema()));
					parameters.add(
							new QueryParameter()
									.name("page")
									.description("Page number")
									.required(false)
									.schema(new IntegerSchema()._default(1)));
					parameters.add(
							new QueryParameter()
									.name("per_page")
									.description("Items per page")
									.required(false)
									.schema(
											new IntegerSchema()
													._default(100)
													.maximum(BigDecimal.valueOf(200))
													.minimum(BigDecimal.valueOf(1))));
					break;
				}
			case SINGULAR:
				{
					break;
				}
		}

		return parameters;
	}

	private static RequestBody getGetRequestBody(Schema schema, IRI schemaIRI) {
		// TODO: implement
		return null;
	}

	private static ApiResponses getGetResponses(
			Schema schema, CardinalityType cardinality, YamlConfig configData) {
		final var apiResponses = new ApiResponses();

		switch (cardinality) {
			case PLURAL:
				{
					// Set response
					ArraySchema arraySchema = new ArraySchema();
					arraySchema.setItems(new Schema().$ref("#/components/schemas/" + schema.getName()));

					var mediaType = new MediaType().schema(arraySchema);
					final var content = new Content().addMediaType("application/json", mediaType);
					final var responseOk =
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH))
									.content(content);
					apiResponses.addApiResponse("200", responseOk);
					break;
				}
			case SINGULAR:
				{
					MediaType mediaType = null;
					final var refSchema = new Schema().$ref("#/components/schemas/" + schema.getName());
					if (GlobalFlags.getFlag(ConfigPropertyNames.GET_BY_KEY_RESPONSE_ARRAY_ENABLE)) {
						final var oneOfSchema = new Schema();
						oneOfSchema.addOneOfItem(new ArraySchema().items(refSchema));
						oneOfSchema.addOneOfItem(
								new ArraySchema().items(new StringSchema()).maxItems(0).example(new ArrayList<>()));

						// When flag for "get by id response array", we're following non-RESTful standards.  An
						// empty array may be returned, instead of a "not found" HTTP response.  Therefore,
						// include an empty array as one of the response examples.
						mediaType =
								new MediaType()
										.schema(oneOfSchema)
										.addExamples("empty_array", new Example().value(List.of()));
					} else {
						mediaType = new MediaType().schema(refSchema);
					}
					// Set request
					var schemaExample = new Example();
					schemaExample.$ref(schema.get$ref());
					final var responseOk =
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH))
									.content(new Content().addMediaType("application/json", mediaType));
					apiResponses.addApiResponse("200", responseOk);
					break;
				}
		}

		if (GlobalFlags.getFlag(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			apiResponses
					.addApiResponse(
							"400",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(400, Locale.ENGLISH)))
					.addApiResponse(
							"401",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(401, Locale.ENGLISH)))
					.addApiResponse(
							"403",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(403, Locale.ENGLISH)))
					.addApiResponse(
							"500",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(500, Locale.ENGLISH)));
		}

		return apiResponses;
	}

	private static Set<Parameter> getPatchParameters() {
		// TODO: implement
		return new HashSet<Parameter>();
	}

	private static RequestBody getPatchRequestBody(Schema schema, IRI schemaIRI) {
		// TODO: implement
		return null;
	}

	private static ApiResponses getPatchResponses() {
		// TODO: implement
		return new ApiResponses();
	}

	private static Set<Parameter> getPostParameters() {
		// TODO: implement
		return new HashSet<Parameter>();
	}

	private static RequestBody getPostRequestBody(Schema schema, IRI schemaIRI) {
		String requestDescription =
				"Information about the ["
						+ schema.getName()
						+ "]("
						+ schemaIRI.getIRIString()
						+ ") to be created.";

		// Set request
		MediaType mediaType =
				new MediaType().schema(new Schema().$ref("#/components/schemas/" + schema.getName()));
		Content content = new Content().addMediaType("application/json", mediaType);

		final var requestBody = new RequestBody();
		requestBody.setContent(content);
		requestBody.setDescription(requestDescription);

		return requestBody;
	}

	private static ApiResponses getPostResponses(YamlConfig configData) {
		if (GlobalFlags.getFlag(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			return new ApiResponses()
					.addApiResponse(
							"201",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(201, Locale.ENGLISH)))
					.addApiResponse(
							"400",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(400, Locale.ENGLISH)))
					.addApiResponse(
							"401",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(401, Locale.ENGLISH)))
					.addApiResponse(
							"403",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(403, Locale.ENGLISH)))
					.addApiResponse(
							"500",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(500, Locale.ENGLISH)));
		}

		return new ApiResponses();
	}

	private static Set<Parameter> getPutParameters() {
		// TODO: implement
		return new HashSet<Parameter>();
	}

	private static RequestBody getPutRequestBody(Schema schema, IRI schemaIRI) {
		String requestDescription =
				"An old [" + schema.getName() + "](" + schemaIRI.getIRIString() + ") to be updated.";

		// Set request
		MediaType mediaType =
				new MediaType().schema(new Schema().$ref("#/components/schemas/" + schema.getName()));
		Content content = new Content().addMediaType("application/json", mediaType);

		final var requestBody = new RequestBody();
		requestBody.setContent(content);
		requestBody.setDescription(requestDescription);

		return requestBody;
	}

	private static ApiResponses getPutResponses(YamlConfig configData) {
		if (GlobalFlags.getFlag(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			new ApiResponses()
					.addApiResponse(
							"204",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(204, Locale.ENGLISH)))
					.addApiResponse(
							"400",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(400, Locale.ENGLISH)))
					.addApiResponse(
							"401",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(401, Locale.ENGLISH)))
					.addApiResponse(
							"403",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(403, Locale.ENGLISH)))
					.addApiResponse(
							"500",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(500, Locale.ENGLISH)));
		}

		return new ApiResponses();
	}

	private static Set<Parameter> getSearchByPostParameters() {
		// TODO: implement
		return new HashSet<Parameter>();
	}

	private static RequestBody getSearchByPostRequestBody(
			Schema schema, IRI schemaIRI, YamlConfig configData) {
		String requestDescription =
				"Search criteria for instance(s) of ["
						+ schema.getName()
						+ "]("
						+ schemaIRI.getIRIString()
						+ ") to be found.";

		// Set request
		final var searchCriteriaMap = new HashMap<String, Schema>();
		final var searchExamplesMap = new HashMap<String, Object>();

		final var searchByPost = configData.getPathConfig().getSearchPaths().searchByPost;

		if (searchByPost.getSearchProperties().size() != searchByPost.getSearchPropertyTypes().size()) {
			System.out.println(
					"Search by POST properties and property types do not contain the same number of items.");
			System.exit(1);
		}

		final var searchPropertiesIterator = searchByPost.getSearchProperties().iterator();
		final var searchPropertyTypesIterator = searchByPost.getSearchPropertyTypes().iterator();

		while (searchPropertiesIterator.hasNext() && searchPropertyTypesIterator.hasNext()) {
			final var searchProperty = searchPropertiesIterator.next();
			searchCriteriaMap.put(
					searchProperty,
					new ArraySchema()
							.items(
									new Schema()
											.$ref(
													"#/components/schemas/"
															+ schema.getName()
															+ "/properties/"
															+ searchProperty)));

			final var searchPropertyType = searchPropertyTypesIterator.next();
			Object searchPropertyValue = null;
			if (PathKeyType.BOOLEAN.equals(searchPropertyType)) {
				searchPropertyValue = Boolean.valueOf(false);
			} else if (PathKeyType.INTEGER.equals(searchPropertyType)) {
				searchPropertyValue = Integer.valueOf(2);
			} else if (PathKeyType.NUMBER.equals(searchPropertyType)) {
				searchPropertyValue = (Number) 5;
			} else {
				searchPropertyValue = String.valueOf("string");
			}

			searchExamplesMap.put(searchProperty, searchPropertyValue);
		}

		final var example = new Example().value(searchExamplesMap);

		MediaType mediaType =
				new MediaType()
						.schema(new ObjectSchema().properties(searchCriteriaMap))
						.examples(Map.of("example_search", example));
		Content content = new Content().addMediaType("application/json", mediaType);

		final var requestBody = new RequestBody();
		requestBody.setContent(content);
		requestBody.setDescription(requestDescription);

		return requestBody;
	}

	/**
	 * Create responses for Search By POST
	 *
	 * @param schema
	 * @param cardinality
	 * @param configData
	 * @return
	 */
	private static ApiResponses getSearchByPostResponses(
			Schema schema, CardinalityType cardinality, YamlConfig configData) {
		// Set response
		ArraySchema arraySchema = new ArraySchema();
		arraySchema.setItems(new Schema().$ref("#/components/schemas/" + schema.getName()));

		final var mediaType = new MediaType().schema(arraySchema);
		final var responseOk =
				new ApiResponse()
						.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH))
						.content(new Content().addMediaType("application/json", mediaType));

		final var apiResponses = new ApiResponses().addApiResponse("200", responseOk);

		if (GlobalFlags.getFlag(ConfigPropertyNames.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			apiResponses
					.addApiResponse(
							"204",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(204, Locale.ENGLISH)))
					.addApiResponse(
							"400",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(400, Locale.ENGLISH)))
					.addApiResponse(
							"401",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(401, Locale.ENGLISH)))
					.addApiResponse(
							"403",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(403, Locale.ENGLISH)))
					.addApiResponse(
							"500",
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(500, Locale.ENGLISH)));
		}

		return apiResponses;
	}
}
