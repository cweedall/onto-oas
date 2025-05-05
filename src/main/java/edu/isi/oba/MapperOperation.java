package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.ConfigFlagType;
import io.swagger.models.Method;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
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
	private String summary;
	private String description;
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
		this.path_id_name =
				this.configData.getPath_config().getGet_paths().getGet_by_key().getKey_name();

		this.auth = this.configData.getAuth() == null ? false : this.configData.getAuth().getEnable();
		this.cardinality = cardinality;
		this.schemaName = schemaName;
		this.schemaURI = schemaURI;
		String ref_text = "#/components/schemas/" + this.schemaName;
		schema = new Schema().$ref(ref_text);

		var descriptionText =
				"The "
						+ this.path_id_name
						+ " of the ["
						+ this.schemaName
						+ "]("
						+ this.schemaURI
						+ ") to be ";

		switch (method) {
			case GET:
				setOperationGet();
				descriptionText += "retrieved.";
				break;
			case PATCH:
				setOperationPatch();
				descriptionText += "patched.";
				break;
			case PUT:
				setOperationPut();
				descriptionText += "updated.";
				break;
			case POST:
				setOperationPost();
				descriptionText += "created.";
				break;
			case DELETE:
				setOperationDelete();
				descriptionText += "deleted.";
				break;
			default:
				break;
		}

		if (Cardinality.SINGULAR.equals(cardinality)) {
			this.parameters.add(
					new PathParameter()
							.description(descriptionText)
							.name(this.path_id_name)
							.required(true)
							.schema(new StringSchema()));
		}

		this.operation =
				new Operation()
						.description(this.description)
						.summary(this.summary)
						.addTagsItem(this.schemaName);

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

	private void setOperationGet() {
		String responseDescriptionOk;
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
					summary = "List all instances of " + this.schemaName;
					description =
							"Gets a list of all instances of [" + this.schemaName + "](" + this.schemaURI + ")";
					responseDescriptionOk =
							"Successful response - returns an array with the instances of ["
									+ this.schemaName
									+ "]("
									+ this.schemaURI
									+ ").";

					// Set response
					ArraySchema schema = new ArraySchema();
					schema.setItems(this.schema);

					var mediaType = new MediaType().schema(schema);
					// mediaType.setExampleSetFlag(true);
					var schemaExample = new Example();
					schemaExample.$ref(this.schema.get$ref());
					// mediaType.setExamples(Map.of(this.schemaName, schemaExample));
					Content content = new Content().addMediaType("application/json", mediaType);
					responseOk = new ApiResponse().description(responseDescriptionOk).content(content);
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

					// MediaType mediaType = new MediaType().schema(schema);
					// //mediaType.setExampleSetFlag(true);
					// var schemaExample = new Example();
					// schemaExample.$ref(this.schema.get$ref());
					// mediaType.setExamples(Map.of(this.schemaName, schemaExample));
					// Content content = new Content().addMediaType("application/json", mediaType);
					// requestBody.setContent(content);
					// requestBody.setDescription(requestDescription);
					// Set the response
					// apiResponses.addApiResponse("201", new ApiResponse()
					// .content(content)
					// .description("Created")
				}
			case SINGULAR:
				{
					summary = "Get a single " + this.schemaName + " by its " + this.path_id_name;
					description =
							"Gets the details of a given [" + this.schemaName + "](" + this.schemaURI + ")";
					responseDescriptionOk =
							"Gets the details of a given [" + this.schemaName + "](" + this.schemaURI + ")";

					// Set request
					var mediaType = new MediaType().schema(schema);
					// mediaType.setExampleSetFlag(true);
					var schemaExample = new Example();
					schemaExample.$ref(this.schema.get$ref());
					// mediaType.setExamples(Map.of(this.schemaName, schemaExample));
					responseOk =
							new ApiResponse()
									.description(responseDescriptionOk)
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
				"Information about the [" + this.schemaName + "](" + this.schemaURI + ") to be created";

		// Edit global fields
		summary = "Create one " + this.schemaName;
		description = "Create a new instance of [" + this.schemaName + "](" + this.schemaURI + ")";

		// Set request
		MediaType mediaType = new MediaType().schema(schema);
		// mediaType.setExampleSetFlag(true);
		// var schemaExample = new Example();
		// schemaExample.$ref(this.schema.get$ref());
		// mediaType.setExamples(Map.of(this.schemaName, schemaExample));
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
				"An old [" + this.schemaName + "](" + this.schemaURI + ") to be updated";

		summary = "Update an existing " + this.schemaName;
		description = "Updates an existing [" + this.schemaName + "](" + this.schemaURI + ")";

		// Set request
		MediaType mediaType = new MediaType().schema(schema);
		// mediaType.setExampleSetFlag(true);
		// var schemaExample = new Example();
		// schemaExample.$ref(this.schema.get$ref());
		// mediaType.setExamples(Map.of(this.schemaName, schemaExample));

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
		summary = "Delete an existing " + this.schemaName;
		description = "Delete an existing [" + this.schemaName + "](" + this.schemaURI + ")";

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
