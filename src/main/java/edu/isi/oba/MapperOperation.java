package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.ConfigFlagType;
import edu.isi.oba.config.paths.PathKeyType;
import io.swagger.models.Method;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.http.impl.EnglishReasonPhraseCatalog;

enum Cardinality {
	SINGULAR,
	PLURAL
}

class MapperOperation {
	private final YamlConfig configData;
	private final String path_id_name;
	private boolean auth;
	private final String schemaName;
	private final String schemaURI;
	private final List<Parameter> parameters = new ArrayList<>();
	private final RequestBody requestBody = new RequestBody();
	private final ApiResponses apiResponses = new ApiResponses();
	private final Cardinality cardinality;
	private final Schema schema;
	private final Operation operation;

	public Operation getOperation() {
		return operation;
	}

	public MapperOperation(
			String schemaName,
			String schemaURI,
			Method method,
			Cardinality cardinality,
			YamlConfig configData) {
		this.configData = configData;
		this.path_id_name = this.getPathKeyName(method);
		this.auth = this.configData.getAuth() == null ? false : this.configData.getAuth().getEnable();
		this.cardinality = cardinality;
		this.schemaName = schemaName;
		this.schemaURI = schemaURI;
		schema = new Schema().$ref("#/components/schemas/" + this.schemaName);

		// Set key parameter, if configured.
		this.setPathKeyParameter(method);

		switch (method) {
			case GET:
				setOperationGet();
				break;
			case PATCH:
				setOperationPatch();
				break;
			case PUT:
				setOperationPut();
				break;
			case POST:
				setOperationPost();
				break;
			case DELETE:
				setOperationDelete();
				break;
			default:
				break;
		}

		// Create new Operation object for the given Schema and the specified method / cardinality.
		this.operation = this.getOperationTemplate(method, schemaName, schemaURI, cardinality);

		if (this.auth) {
			SecurityRequirement securityRequirement = new SecurityRequirement();
			securityRequirement.addList("BearerAuth");
			this.operation.addSecurityItem(securityRequirement);

			if (Set.of(Method.PATCH, Method.PUT, Method.POST, Method.DELETE).contains(method)) {
				this.parameters.add(
						new QueryParameter()
								.description("Username")
								.name("user")
								.required(false)
								.schema(new StringSchema()));
			}
		}

		this.operation.setParameters(this.parameters);
		this.operation.setResponses(this.apiResponses);

		if (Set.of(Method.PATCH, Method.PUT, Method.POST).contains(method)) {
			this.operation.setRequestBody(this.requestBody);
		}

		if (Cardinality.SINGULAR.equals(cardinality)) {
			this.operation.setOperationId(
					StringUtils.getLowerCasePluralOf(
									StringUtils.pascalCaseToKebabCase(method.name() + this.schemaName))
							+ "-"
							+ this.path_id_name);
		} else {
			this.operation.setOperationId(
					StringUtils.getLowerCasePluralOf(
							StringUtils.pascalCaseToKebabCase(method.name() + this.schemaName)));
		}
	}

	private Operation getOperationTemplate(
			Method method, String schemaName, String schemaURI, Cardinality cardinality) {
		String operationDescription = null;
		String operationSummary = null;

		switch (method) {
			case DELETE:
				if (Cardinality.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Delete an existing instance of ["
									+ schemaName
									+ "]("
									+ schemaURI
									+ ") by its "
									+ this.path_id_name;
					operationSummary = "Delete an existing " + schemaName;
				}
				break;
			case GET:
				if (Cardinality.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Gets the details of a given ["
									+ schemaName
									+ "]("
									+ schemaURI
									+ ") by its "
									+ this.path_id_name;
					operationSummary = "Get a single " + schemaName;
				} else {
					operationDescription =
							"Gets the details of all instances of [" + schemaName + "](" + schemaURI + ")";
					operationSummary = "Gets all instances of " + schemaName;
				}
				break;
			case PATCH:
				operationDescription = null;
				operationSummary = null;
				break;
			case POST:
				if (Cardinality.SINGULAR.equals(cardinality)) {
					operationDescription = "Create a new instance of [" + schemaName + "](" + schemaURI + ")";
					operationSummary = "Create one " + schemaName;
				} else {
					operationDescription =
							"Create one or more new instances of [" + schemaName + "](" + schemaURI + ")";
					operationSummary = "Create one or more " + schemaName;
				}

				break;
			case PUT:
				if (Cardinality.SINGULAR.equals(cardinality)) {
					operationDescription =
							"Update an existing instance of ["
									+ schemaName
									+ "]("
									+ schemaURI
									+ ") by its "
									+ this.path_id_name;
					operationSummary = "Update an existing " + schemaName;
				} else {
					operationDescription = "Update bulk instances of [" + schemaName + "](" + schemaURI + ")";
					operationSummary = "Update one or more " + schemaName;
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

	private Schema getSchemaBasedOnPathKeyType(PathKeyType keyType) {
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

	private String getPathKeyName(Method method) {
		if (Cardinality.SINGULAR.equals(cardinality)) {
			final var pathConfig = this.configData.getPath_config();

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

	private PathKeyType getPathKeyType(Method method) {
		if (Cardinality.SINGULAR.equals(cardinality)) {
			final var pathConfig = this.configData.getPath_config();

			switch (method) {
				case GET:
					return pathConfig.getGet_paths().getGet_by_key().getKey_type();
				case PUT:
					return pathConfig.getPut_paths().getPut_by_key().getKey_type();
				case DELETE:
					return pathConfig.getDelete_paths().getDelete_by_key().getKey_type();
				default:
					break;
			}
		}

		return null;
	}

	private void setPathKeyParameter(Method method) {
		if (Cardinality.SINGULAR.equals(cardinality)) {
			final var keyName = this.getPathKeyName(method);

			if (keyName != null) {
				final var descriptionText = "The " + keyName + " of the endpoint resource.";
				this.parameters.add(
						new PathParameter()
								.description(descriptionText)
								.name(keyName)
								.required(true)
								.schema(this.getSchemaBasedOnPathKeyType(this.getPathKeyType(method))));
			}
		}
	}

	private void setOperationGet() {
		ApiResponse responseOk;
		// Set parameters
		if (this.auth)
			this.parameters.add(
					new QueryParameter()
							.name("username")
							.description("Name of the user graph to query")
							.required(false)
							.schema(new StringSchema()));

		switch (cardinality) {
			case PLURAL:
				{
					// Set response
					ArraySchema schema = new ArraySchema();
					schema.setItems(this.schema);

					var mediaType = new MediaType().schema(schema);
					var schemaExample = new Example();
					schemaExample.$ref(this.schema.get$ref());
					Content content = new Content().addMediaType("application/json", mediaType);
					responseOk =
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH))
									.content(content);
					this.apiResponses.addApiResponse("200", responseOk);
					this.parameters.add(
							new QueryParameter()
									.name("label")
									.description("Filter by label")
									.required(false)
									.schema(new StringSchema()));
					this.parameters.add(
							new QueryParameter()
									.name("page")
									.description("Page number")
									.required(false)
									.schema(new IntegerSchema()._default(1)));
					this.parameters.add(
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
					// Set request
					var mediaType = new MediaType().schema(schema);
					var schemaExample = new Example();
					schemaExample.$ref(this.schema.get$ref());
					responseOk =
							new ApiResponse()
									.description(EnglishReasonPhraseCatalog.INSTANCE.getReason(200, Locale.ENGLISH))
									.content(new Content().addMediaType("application/json", mediaType));
					this.apiResponses.addApiResponse("200", responseOk);
					break;
				}
		}

		if (this.configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			this.apiResponses
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
	}

	private void setOperationPatch() {
		// TODO: implement
	}

	private void setOperationPost() {
		String requestDescription =
				"Information about the [" + this.schemaName + "](" + this.schemaURI + ") to be created.";

		// Set request
		MediaType mediaType = new MediaType().schema(schema);
		Content content = new Content().addMediaType("application/json", mediaType);
		this.requestBody.setContent(content);
		this.requestBody.setDescription(requestDescription);

		if (this.configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			this.apiResponses
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
	}

	private void setOperationPut() {
		String requestDescription =
				"An old [" + this.schemaName + "](" + this.schemaURI + ") to be updated.";

		// Set request
		MediaType mediaType = new MediaType().schema(schema);
		Content content = new Content().addMediaType("application/json", mediaType);
		this.requestBody.setContent(content);
		this.requestBody.setDescription(requestDescription);

		if (this.configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			this.apiResponses
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
	}

	private void setOperationDelete() {
		if (this.configData.getConfigFlagValue(ConfigFlagType.USE_COMMON_DEFAULT_PATH_RESPONSES)) {
			// Set the response
			this.apiResponses
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
	}
}
