package edu.isi.oba.generators;

import edu.isi.oba.config.ConfigPropertyNames;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.config.paths.OperationType;
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
	 * Generate all endpoint paths (including all operations for them) for all schemas, based on any
	 * configuration requirements in the configuration file.
	 *
	 * @param schemas a {@link Map} of class {@link IRI}s and {@link Schema}s.
	 * @param configData the {@link YamlConfig} configuration settings for conversion.
	 * @return a {@link Map} of endpoint paths to Swagger OAS {@link PathItem}s - empty if not
	 *     configurable.
	 */
	public static Map<String, PathItem> generateAllPathItems(
			Map<IRI, Schema> schemas, YamlConfig configData) {
		final var pathNamePathItemMap = new HashMap<String, PathItem>();
		if (schemas == null) {
			return pathNamePathItemMap;
		} else {
			schemas.forEach(
					(iri, schema) -> {
						pathNamePathItemMap.putAll(
								PathGenerator.generateAllPathItemsForSchema(schema, iri, configData));
					});

			return pathNamePathItemMap;
		}
	}

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
						+ (GlobalFlags.getFlag(ConfigPropertyNames.USE_KEBAB_CASE_PATHS)
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
		final var pathConfig = configData.getPathConfig();
		final var test = "";

		// -----------------------------
		// PLURAL records
		// -----------------------------
		if ((GlobalFlags.getFlag(ConfigPropertyNames.GET_ALL_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.GET_ALL))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.GET_ALL)) {
			suffixPathItemsMap
					.computeIfAbsent(null, k -> new PathItem())
					.get(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.GET_ALL, configData));
		}

		if ((GlobalFlags.getFlag(ConfigPropertyNames.POST_BULK_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.POST_BULK))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.POST_BULK)) {
			final var postBulkSuffix = pathConfig.getPostPaths().postBulk.getPath_suffix();
			suffixPathItemsMap
					.computeIfAbsent(postBulkSuffix, k -> new PathItem())
					.post(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.POST_BULK, configData));
		}

		if ((GlobalFlags.getFlag(ConfigPropertyNames.PUT_BULK_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.PUT_BULK))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.PUT_BULK)) {
			final var putBulkSuffix = pathConfig.getPutPaths().putBulk.getPathSuffix();
			suffixPathItemsMap
					.computeIfAbsent(putBulkSuffix, k -> new PathItem())
					.put(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.PUT_BULK, configData));
		}

		// SEARCH - these are always PLURAL records
		// Currently - only supports searches via the POST operation (e.g. POST
		// /{resource-name}/_search).
		if ((GlobalFlags.getFlag(ConfigPropertyNames.SEARCH_BY_POST_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.SEARCH_BY_POST))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.SEARCH_BY_POST)) {
			final var searchByPostSuffix = pathConfig.getSearchPaths().searchByPost.getPathSuffix();
			suffixPathItemsMap
					.computeIfAbsent(searchByPostSuffix, k -> new PathItem())
					.post(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.SEARCH_BY_POST, configData));
		}

		// -----------------------------
		// SINGULAR records
		// -----------------------------
		if ((GlobalFlags.getFlag(ConfigPropertyNames.GET_BY_KEY_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.GET_BY_KEY))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.GET_BY_KEY)) {
			final var getSuffix = "{" + pathConfig.getGetPaths().getByKey.getKeyName() + "}";
			suffixPathItemsMap
					.computeIfAbsent(getSuffix, k -> new PathItem())
					.get(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.GET_BY_KEY, configData));
		}

		if ((GlobalFlags.getFlag(ConfigPropertyNames.DELETE_BY_KEY_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.DELETE_BY_KEY))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.DELETE_BY_KEY)) {
			final var deleteSuffix = "{" + pathConfig.getDeletePaths().deleteByKey.getKeyName() + "}";
			suffixPathItemsMap
					.computeIfAbsent(deleteSuffix, k -> new PathItem())
					.delete(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.DELETE_BY_KEY, configData));
		}

		if ((GlobalFlags.getFlag(ConfigPropertyNames.POST_SINGLE_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.POST_SINGLE))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.POST_SINGLE)) {
			suffixPathItemsMap
					.computeIfAbsent(null, k -> new PathItem())
					.post(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.POST_SINGLE, configData));
		}

		if ((GlobalFlags.getFlag(ConfigPropertyNames.PUT_BY_KEY_ENABLE)
						&& !pathConfig
								.getPathsForClasses(schemaIRI)
								.denyOperationsContains(OperationType.PUT_BY_KEY))
				|| pathConfig
						.getPathsForClasses(schemaIRI)
						.allowOperationsContains(OperationType.PUT_BY_KEY)) {
			final var putSuffix = "{" + pathConfig.getPutPaths().putByKey.getKeyName() + "}";
			suffixPathItemsMap
					.computeIfAbsent(putSuffix, k -> new PathItem())
					.put(
							OperationGenerator.generateOperation(
									schema, schemaIRI, OperationType.PUT_BY_KEY, configData));
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
