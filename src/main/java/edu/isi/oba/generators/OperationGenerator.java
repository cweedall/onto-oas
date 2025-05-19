package edu.isi.oba.generators;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.ConfigFlagType;
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
				OperationGenerator.getOperationTemplate(schema, schemaIRI, method, cardinality, configData);

		final var parameters = new HashSet<Parameter>();
		final var apiResponses = new ApiResponses();

		final var auth = configData.getAuth() == null ? false : configData.getAuth().getEnable();

		final var operationIdSuffix =
				OperationGenerator.getOperationIdSuffix(method, cardinality, configData);

		// Set key parameter, if configured.
		parameters.addAll(OperationGenerator.setPathKeyParameter(method, cardinality, configData));

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

		final var operationId =
				StringUtils.getLowerCasePluralOf(
								StringUtils.pascalCaseToKebabCase(method.name() + schema.getName()))
						+ (operationIdSuffix == null ? "" : "-" + operationIdSuffix.toLowerCase());
		operation.setOperationId(operationId);

		return operation;
	}

	private static Operation getOperationTemplate(
			Schema schema,
			IRI schemaIRI,
			HttpMethod method,
			CardinalityType cardinality,
			YamlConfig configData) {
		final var schemaName = schema.getName();
		final var iriString = schemaIRI.getIRIString();
		final var pathIdNameInText =
				OperationGenerator.getPathKeyNameInText(method, cardinality, configData);

		String operationDescription = null;
		String operationSummary = null;

		switch (method) {
			case DELETE:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Delete an existing instance of ["
									+ schemaName
									+ "]("
									+ iriString
									+ ") by its "
									+ pathIdNameInText
									+ ".";
					operationSummary = "Delete an existing " + schemaName;
				}
				break;
			case GET:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Gets the details of a given ["
									+ schemaName
									+ "]("
									+ iriString
									+ ") by its "
									+ pathIdNameInText
									+ ".";
					operationSummary = "Get a single " + schemaName;
				} else {
					operationDescription =
							"Gets the details of all instances of [" + schemaName + "](" + iriString + ")" + ".";
					operationSummary = "Gets all instances of " + schemaName;
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
					operationSummary = "Create one " + schemaName;
				} else {
					operationDescription =
							"Create one or more new instances of [" + schemaName + "](" + iriString + ")" + ".";
					operationSummary = "Create one or more " + schemaName;
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
					operationSummary = "Update an existing " + schemaName;
				} else {
					operationDescription =
							"Update one or more instances of [" + schemaName + "](" + iriString + ").";
					operationSummary = "Update one or more " + schemaName;
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
					operationSummary = "Search for one or more " + schemaName;
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

	private static String getOperationIdSuffix(
			HttpMethod method, CardinalityType cardinality, YamlConfig configData) {
		final var pathConfig = configData.getPath_config();

		switch (method) {
			case DELETE:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					return pathConfig.getDelete_paths().getDelete_by_key().getKey_name();
				} else {
					return null;
				}
			case GET:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					return pathConfig.getGet_paths().getGet_by_key().getKey_name();
				} else {
					return null;
				}
			case POST:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					return null;
				} else {
					return pathConfig.getPost_paths().getPost_bulk().getPath_suffix();
				}
			case PUT:
				if (CardinalityType.SINGULAR.equals(cardinality)) {
					return pathConfig.getPut_paths().getPut_by_key().getKey_name();
				} else {
					return pathConfig.getPut_paths().getPut_bulk().getPath_suffix();
				}
			default:
				break;
		}

		return null;
	}

	private static String getPathKeyName(
			HttpMethod method, CardinalityType cardinality, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(cardinality)) {
			final var pathConfig = configData.getPath_config();

			switch (method) {
				case GET:
					return pathConfig.getGet_paths().getGet_by_key().getKey_name();
				case PUT:
					return pathConfig.getPut_paths().getPut_by_key().getKey_name();
				case DELETE:
					return pathConfig.getDelete_paths().getDelete_by_key().getKey_name();
				default:
					break;
			}
		}

		return null;
	}

	private static String getPathKeyNameInText(
			HttpMethod method, CardinalityType cardinality, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(cardinality)) {
			final var pathConfig = configData.getPath_config();

			switch (method) {
				case GET:
					return pathConfig.getGet_paths().getGet_by_key().getKey_name_in_text();
				case PUT:
					return pathConfig.getPut_paths().getPut_by_key().getKey_name_in_text();
				case DELETE:
					return pathConfig.getDelete_paths().getDelete_by_key().getKey_name_in_text();
				default:
					break;
			}
		}

		return null;
	}

	private static PathKeyType getPathKeyType(
			HttpMethod method, CardinalityType cardinality, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(cardinality)) {
			final var pathConfig = configData.getPath_config();

			switch (method) {
				case DELETE:
					return pathConfig.getDelete_paths().getDelete_by_key().getKey_type();
				case GET:
					return pathConfig.getGet_paths().getGet_by_key().getKey_type();
				case PUT:
					return pathConfig.getPut_paths().getPut_by_key().getKey_type();
				default:
					break;
			}
		}

		return null;
	}

	private static Set<Parameter> setPathKeyParameter(
			HttpMethod method, CardinalityType cardinality, YamlConfig configData) {
		if (CardinalityType.SINGULAR.equals(cardinality)) {
			final var keyName = OperationGenerator.getPathKeyName(method, cardinality, configData);
			final var keyNameInText =
					OperationGenerator.getPathKeyNameInText(method, cardinality, configData);

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
														OperationGenerator.getPathKeyType(method, cardinality, configData))));
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
		if (configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
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
					if (configData.getConfigFlagValue(ConfigFlagType.PATH_GET_BY_ID_RESPONSE_ARRAY)) {
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

		if (configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
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
		if (configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
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
		if (configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
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
				"Search criteria for instance(s) of the ["
						+ schema.getName()
						+ "]("
						+ schemaIRI.getIRIString()
						+ ") to be found.";

		// Set request
		final var searchCriteriaMap = new HashMap<String, Schema>();
		final var searchExamplesMap = new HashMap<String, Object>();

		final var searchByPost = configData.getPath_config().getSearch_paths().getSearch_by_post();

		if (searchByPost.getSearch_properties().size()
				!= searchByPost.getSearch_property_types().size()) {
			System.out.println(
					"Search by POST properties and property types do not contain the same number of items.");
			System.exit(1);
		}

		final var searchPropertiesIterator = searchByPost.getSearch_properties().iterator();
		final var searchPropertyTypesIterator = searchByPost.getSearch_property_types().iterator();

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

		var mediaType = new MediaType().schema(arraySchema);
		final var content = new Content().addMediaType("application/json", mediaType);
		final var responseOk =
				new ApiResponse()
						.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH))
						.content(content);

		final var apiResponses = new ApiResponses().addApiResponse("200", responseOk);

		if (configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
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

		return new ApiResponses();
	}
}
