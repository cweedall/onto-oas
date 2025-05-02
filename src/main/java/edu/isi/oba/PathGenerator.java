package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.flags.CONFIG_FLAG;
import io.swagger.models.Method;
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

class PathGenerator {
	private final YamlConfig configData;

	public PathGenerator(YamlConfig configData) {
		this.configData = configData;
	}

	public PathItem generate_singular(String schemaName, String schemaURI) {
		PathItem path_item = new PathItem();
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.PATH_GET)) {
			path_item.get(
					new MapperOperation(
									schemaName, schemaURI, Method.GET, Cardinality.SINGULAR, this.configData)
							.getOperation());
		}

		if (this.configData.getConfigFlagValue(CONFIG_FLAG.PATH_DELETE)) {
			path_item.delete(
					new MapperOperation(
									schemaName, schemaURI, Method.DELETE, Cardinality.SINGULAR, this.configData)
							.getOperation());
		}

		if (this.configData.getConfigFlagValue(CONFIG_FLAG.PATH_POST)) {
			path_item.put(
					new MapperOperation(
									schemaName, schemaURI, Method.POST, Cardinality.SINGULAR, this.configData)
							.getOperation());
		}

		if (this.configData.getConfigFlagValue(CONFIG_FLAG.PATH_PUT)) {
			path_item.put(
					new MapperOperation(
									schemaName, schemaURI, Method.PUT, Cardinality.SINGULAR, this.configData)
							.getOperation());
		}

		return path_item;
	}

	public PathItem generate_plural(String schemaName, String schemaURI) {
		PathItem path_item = new PathItem();
		if (this.configData.getConfigFlagValue(CONFIG_FLAG.PATH_GET)) {
			path_item.get(
					new MapperOperation(
									schemaName, schemaURI, Method.GET, Cardinality.PLURAL, this.configData)
							.getOperation());
		}

		if (this.configData.getConfigFlagValue(CONFIG_FLAG.PATH_POST)) {
			path_item.post(
					new MapperOperation(
									schemaName, schemaURI, Method.POST, Cardinality.PLURAL, this.configData)
							.getOperation());
		}

		return path_item;
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
