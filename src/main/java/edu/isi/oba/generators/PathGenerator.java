package edu.isi.oba.generators;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.utils.StringUtils;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.HashMap;
import java.util.Map;
import org.semanticweb.owlapi.model.IRI;

public class PathGenerator {
	/**
	 * Generate all endpoint paths (including all operations for them) for a particular schema, based
	 * on any configuration requirements in the configuration file.
	 *
	 * @param schema a Swagger {@link Schema}.
	 * @param schemaIRI the {@link Schema}'s {@link IRI} (in the ontology).
	 * @param configData the {@link YamlConfig} configuration settings for conversion.
	 * @return a {@link Map} of endpoint path suffixes to Swagger OAS {@link PathItem}s - empty if not
	 *     configurable.
	 */
	public static Map<String, PathItem> generateAllPathItemsForSchema(
			Schema schema, IRI schemaIRI, YamlConfig configData) {
		final var pathNamePathItemMap = new HashMap<String, PathItem>();

		final var pluralPathName =
				"/"
						+ (configData.getConfigFlagValue(ConfigFlagType.USE_KEBAB_CASE_PATHS)
								? StringUtils.getLowerCasePluralOf(
										StringUtils.pascalCaseToKebabCase(schema.getName()))
								: StringUtils.getLowerCasePluralOf(schema.getName()));

		PathGenerator.generatePathSuffixPathItems(schema, schemaIRI, configData)
				.forEach(
						(pathSuffix, pathItem) -> {
							final var addedPathSuffix = pathSuffix == null ? "" : "/" + pathSuffix;
							pathNamePathItemMap.put(pluralPathName + addedPathSuffix, pathItem);
						});

		return pathNamePathItemMap;
	}

	/**
	 * Generate a singular {@link PathItem} for the path section of the OpenAPI specification.
	 *
	 * @param schemaName the {@link Schema}'s name (to be used for the path name).
	 * @param schemaIRI the {@link Schema}'s {@link IRI} (in the ontology).
	 * @return a {@link Map} of endpoint path suffixes to Swagger OAS {@link PathItem}s - empty if not
	 *     configurable.
	 */
	private static Map<String, PathItem> generatePathSuffixPathItems(
			Schema schema, IRI schemaIRI, YamlConfig configData) {
		final var suffixPathItemsMap = new HashMap<String, PathItem>();

		// -----------------------------
		// PLURAL records
		// -----------------------------
		if (configData.getConfigFlagValue(ConfigFlagType.PATH_GET_ALL)) {
			suffixPathItemsMap
					.computeIfAbsent(null, k -> new PathItem())
					.get(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.GET, CardinalityType.PLURAL, configData));
		}

		if (configData.getConfigFlagValue(ConfigFlagType.PATH_POST_BULK)) {
			final var postBulkSuffix =
					configData.getPath_config().getPost_paths().getPost_bulk().getPath_suffix();
			suffixPathItemsMap
					.computeIfAbsent(postBulkSuffix, k -> new PathItem())
					.post(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.POST, CardinalityType.PLURAL, configData));
		}

		// SEARCH - these are always PLURAL records
		// Currently - only supports searches via the POST operation (e.g. POST
		// /{resource-name}/_search).
		if (configData.getConfigFlagValue(ConfigFlagType.PATH_SEARCH_BY_POST)) {
			final var searchByPostSuffix =
					configData.getPath_config().getSearch_paths().getSearch_by_post().getPath_suffix();
			suffixPathItemsMap
					.computeIfAbsent(searchByPostSuffix, k -> new PathItem())
					.post(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.SEARCH, CardinalityType.PLURAL, configData));
		}

		// -----------------------------
		// SINGULAR records
		// -----------------------------
		if (configData.getConfigFlagValue(ConfigFlagType.PATH_GET_BY_ID)) {
			final var getSuffix =
					"{" + configData.getPath_config().getGet_paths().getGet_by_key().getKey_name() + "}";
			suffixPathItemsMap
					.computeIfAbsent(getSuffix, k -> new PathItem())
					.get(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.GET, CardinalityType.SINGULAR, configData));
		}

		if (configData.getConfigFlagValue(ConfigFlagType.PATH_DELETE_BY_ID)) {
			final var deleteSuffix =
					"{"
							+ configData.getPath_config().getDelete_paths().getDelete_by_key().getKey_name()
							+ "}";
			suffixPathItemsMap
					.computeIfAbsent(deleteSuffix, k -> new PathItem())
					.delete(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.DELETE, CardinalityType.SINGULAR, configData));
		}

		if (configData.getConfigFlagValue(ConfigFlagType.PATH_POST_SINGLE)) {
			suffixPathItemsMap
					.computeIfAbsent(null, k -> new PathItem())
					.post(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.POST, CardinalityType.SINGULAR, configData));
		}

		if (configData.getConfigFlagValue(ConfigFlagType.PATH_PUT_BY_ID)) {
			final var putSuffix =
					"{" + configData.getPath_config().getPut_paths().getPut_by_key().getKey_name() + "}";
			suffixPathItemsMap
					.computeIfAbsent(putSuffix, k -> new PathItem())
					.put(
							OperationGenerator.generateOperation(
									schema, schemaIRI, HttpMethod.PUT, CardinalityType.SINGULAR, configData));
		}

		return suffixPathItemsMap;
	}

	public static PathItem user_login(String schema_name) {
		ApiResponses apiResponses = new ApiResponses();

		final RequestBody requestBody = new RequestBody();

		String ref_text = "#/components/schemas/" + schema_name;
		Schema schema = new Schema().$ref(ref_text);

		MediaType mediaType = new MediaType().schema(schema);

		Content content = new Content().addMediaType("application/json", mediaType);
		requestBody.setContent(content);
		String requestDescription = "User credentials";
		requestBody.setDescription(requestDescription);

		Map<String, Header> headers = new HashMap<>();
		headers.put(
				"X-Rate-Limit",
				new Header()
						.description("calls per hour allowed by the user")
						.schema(new IntegerSchema().format("int32")));
		headers.put(
				"X-Expires-After",
				new Header()
						.description("date in UTC when token expires")
						.schema(new StringSchema().format("date-time")));
		apiResponses.addApiResponse(
				"200",
				new ApiResponse()
						.description("successful operation")
						.headers(headers)
						.content(
								new Content()
										.addMediaType("application/json", new MediaType().schema(new StringSchema()))));

		apiResponses.addApiResponse(
				"400",
				new ApiResponse()
						.description("unsuccessful operation")
						.content(
								new Content()
										.addMediaType("application/json", new MediaType().schema(new StringSchema()))));

		Map<String, Object> extensions = new HashMap<String, Object>();
		extensions.put("x-openapi-router-controller", "openapi_server.controllers.user_controller");
		Operation operation =
				new Operation()
						.description("Login the user")
						.extensions(extensions)
						.requestBody(requestBody)
						.responses(apiResponses);
		return new PathItem().post(operation);
	}
}
