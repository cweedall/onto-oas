package edu.isi.oba;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.oas.inflector.examples.ExampleBuilder.RequestType;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContext;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.jena.iri.IRIException0;
import org.apache.jena.iri.IRIFactory;

public class ExamplesGenerator {
	public static Map<String, Example> generateExamples(OpenAPI openAPI) throws Exception {
		final var componentsWithExamples = new HashMap<String, Example>();

		// Don't use .sortedOutput(true) because we are using SortedSchemaMixin to alphabetically sort
		// the desired entries.  Sorting _everything_ alphabetically messes up the YAML file by moving
		// the info section away from the top of the document, for example.
		final var openApiConfiguration = new SwaggerConfiguration().openAPI(openAPI);

		final var ctx = new JaxrsOpenApiContext<>().openApiConfiguration(openApiConfiguration).init();

		final var content =
				ctx.getOutputJsonMapper().writer(new DefaultPrettyPrinter()).writeValueAsString(openAPI);

		final var parseOptions = new ParseOptions();
		parseOptions.setResolve(true);
		parseOptions.setResolveFully(true);

		final var parseResult = new OpenAPIV3Parser().readContents(content, null, parseOptions);
		final var openAPITest = parseResult.getOpenAPI();

		final var openAPIschemas =
				ctx.getOutputJsonMapper()
						.writer(new DefaultPrettyPrinter())
						.writeValueAsString(openAPITest.getComponents().getSchemas());

		final var objMapper = new ObjectMapper();
		try {
			final var objs = objMapper.readTree(openAPIschemas);
			final var examplesReadOnlyMap = ExamplesGenerator.iterateJsonNode(objs, RequestType.READ);
			final var examplesWriteOnlyMap = ExamplesGenerator.iterateJsonNode(objs, RequestType.WRITE);

			final var examplesMap =
					ExamplesGenerator.getMergedReadWriteExampleMaps(
							examplesReadOnlyMap, examplesWriteOnlyMap);

			examplesMap.forEach(
					(exampleKey, examplesObj) -> {
						final var tempex = new Example();
						tempex.value(examplesObj);
						componentsWithExamples.put(exampleKey, tempex);
					});
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new TreeMap<>(componentsWithExamples);
	}

	private static Map<String, Object> iterateJsonNode(JsonNode node) {
		return ExamplesGenerator.iterateJsonNode(node, null);
	}

	private static Map<String, Object> iterateJsonNode(JsonNode node, RequestType requestType) {
		final var newmap = new HashMap<String, Object>();

		final var fields = node.fields();

		while (fields.hasNext()) {
			final var field = fields.next();
			final var key = field.getKey();
			final var value = field.getValue();

			if (key != null) {
				if (value == null) {
					newmap.put(key, null);
				} else {
					final var propertiesNode = value.get("properties");

					if (propertiesNode == null) {
						final var readOnlyNode = value.get("readOnly");
						final var writeOnlyNode = value.get("writeOnly");

						final var isAllowedForReadOnly =
								(writeOnlyNode == null ? true : !Boolean.parseBoolean(writeOnlyNode.asText()))
										&& (readOnlyNode == null ? true : Boolean.parseBoolean(readOnlyNode.asText()));
						final var isAllowedForWriteOnly =
								(readOnlyNode == null ? true : !Boolean.parseBoolean(readOnlyNode.asText()))
										&& (writeOnlyNode == null
												? true
												: Boolean.parseBoolean(writeOnlyNode.asText()));

						if (requestType == null
								|| (requestType == RequestType.READ && isAllowedForReadOnly)
								|| (requestType == RequestType.WRITE && isAllowedForWriteOnly)) {
							final var defaultNode = value.get("default");
							if (defaultNode == null) {
								final var exampleNode = value.get("example");
								if (exampleNode == null) {
									final var enumNode = value.get("enum");
									if (enumNode == null) {
										final var typeNode = value.get("type");
										if (typeNode == null) {
											// If no type, treat as a default string.
											// This should never happen!?
											newmap.put(key, null);
										} else {
											if ("array".equals(typeNode.asText())) {
												final var itemsNode = value.get("items");
												if (itemsNode == null) {
													newmap.put(key, null);
												} else {
													final var minItemsNode = value.get("minItems");
													final var maxItemsNode = value.get("maxItems");
													final var arrayNode = new ObjectMapper().createArrayNode();
													final var arrayNodePOJOItem =
															ExamplesGenerator.getObjectFromTypeAndFormat(itemsNode);
													if (minItemsNode == null && maxItemsNode == null) {
														arrayNode.addPOJO(arrayNodePOJOItem);
													} else if (maxItemsNode != null) {
														final var maxItemsNumber = Integer.parseInt(maxItemsNode.asText());
														for (int i = 0; i < maxItemsNumber; i++) {
															arrayNode.addPOJO(arrayNodePOJOItem);
														}
													} else {
														final var minItemsNumber = Integer.parseInt(minItemsNode.asText());
														for (int i = 0; i < minItemsNumber; i++) {
															arrayNode.addPOJO(arrayNodePOJOItem);
														}
													}
													newmap.put(key, arrayNode);
												}
											} else {
												newmap.put(key, ExamplesGenerator.getObjectFromTypeAndFormat(value));
											}
										}
									} else {
										// If there is an enum list, it should always have at least one value.
										// This is a safety check to avoid problems.
										if (enumNode.size() > 0) {
											newmap.put(key, enumNode.get(0));
										} else {
											newmap.put(key, String.valueOf(""));
										}
									}
								} else {
									newmap.put(key, exampleNode);
								}
							} else {
								newmap.put(key, defaultNode);
							}
						}
					} else {
						final var readOnlyNode = value.get("readOnly");
						final var writeOnlyNode = value.get("writeOnly");

						final var isAllowedForReadOnly =
								(writeOnlyNode == null ? true : !Boolean.parseBoolean(writeOnlyNode.asText()))
										&& (readOnlyNode == null ? true : Boolean.parseBoolean(readOnlyNode.asText()));
						final var isAllowedForWriteOnly =
								(readOnlyNode == null ? true : !Boolean.parseBoolean(readOnlyNode.asText()))
										&& (writeOnlyNode == null
												? true
												: Boolean.parseBoolean(writeOnlyNode.asText()));

						if (requestType == null
								|| (requestType == RequestType.READ && isAllowedForReadOnly)
								|| (requestType == RequestType.WRITE && isAllowedForWriteOnly)) {
							newmap.put(key, ExamplesGenerator.iterateJsonNode(propertiesNode, requestType));
						}
					}
				}
			}
		}

		return new TreeMap<>(newmap);
	}

	private static Object getObjectFromTypeAndFormat(JsonNode valueNode) {
		final var typeNode = valueNode.get("type");
		if (typeNode == null) {
			return null;
		} else {
			if ("boolean".equals(typeNode.asText())) {
				return Boolean.valueOf(false);
			} else if ("integer".equals(typeNode.asText())) {
				return Integer.valueOf(2);
			} else if ("number".equals(typeNode.asText())) {
				final var formatNode = valueNode.get("format");
				if (formatNode == null) {
					// If no format, treat as a default number.
					return (Number) 5;
				} else {
					if ("double".equals(formatNode.asText())) {
						return Double.valueOf("8.123456789101112");
					} else if ("float".equals(formatNode.asText())) {
						return Float.valueOf("9.1234567");
					} else {
						return (Number) 5;
					}
				}
			} else {
				final var formatNode = valueNode.get("format");
				if (formatNode == null) {
					// If no format, treat as a default string.
					return typeNode;
				} else {
					if ("binary".equals(formatNode.asText())) {
						// Represents binary data, often used for file uploads.
						return Integer.parseInt("This is some binary data", 2);
					} else if ("byte".equals(formatNode.asText())) {
						// Represents base64 encoded data / file contents
						return String.valueOf("These are some bytes").getBytes();
					} else if ("date".equals(formatNode.asText())) {
						// Represents a date in the format YYYY-MM-DD.
						return LocalDate.parse("2025-01-02");
					} else if ("date-time".equals(formatNode.asText())) {
						// Represents a date and time in the format YYYY-MM-DDTHH:mm:ssZ.
						return ZonedDateTime.parse("2025-01-02T07:00:00.000Z");
					} else if ("hostname".equals(formatNode.asText())) {
						// Represents a host name as defined by RFC1123
						final var hostname = "localhost";
						try {
							return InetAddress.getByName(hostname).getCanonicalHostName();
						} catch (UnknownHostException e) {
							return String.valueOf(hostname);
						}
					} else if ("ipv4".equals(formatNode.asText())) {
						// Represents an IPv4 address.
						final var ipv4Addr = "192.168.1.1";
						try {
							return (Inet4Address) InetAddress.getByName(ipv4Addr);
						} catch (UnknownHostException e) {
							return String.valueOf(ipv4Addr);
						}
					} else if ("ipv6".equals(formatNode.asText())) {
						// Represents an IPv6 address.
						final var ipv6Addr = "2001:db8:3333:4444:5555:6666:7777:8888";
						try {
							return (Inet6Address) InetAddress.getByName(ipv6Addr);
						} catch (UnknownHostException e) {
							return String.valueOf(ipv6Addr);
						}
					} else if ("iri".equals(formatNode.asText())) {
						// Represents an Internationalized Resource Identifier (IRI).
						final var iri = "http://www.example.org/red%09ros&#xE9;#red";
						try {
							return IRIFactory.iriImplementation().construct(iri);
						} catch (IRIException0 e) {
							return String.valueOf(iri);
						}
					} else if ("password".equals(formatNode.asText())) {
						// Indicates that the string contains sensitive information.
						return String.valueOf("\u25CF\u25CF\u25CF\u25CF\u25CF\u25CF");
					} else if ("uri".equals(formatNode.asText())) {
						// Represents a Uniform Resource Identifier (URI).
						final var uri = "mailto:user@example.com";
						try {
							return new URI(uri);
						} catch (URISyntaxException e) {
							return String.valueOf(uri);
						}
					} else if ("uri-reference".equals(formatNode.asText())) {
						// Represents a URI reference.
						final var uriReference = "http://www.example.com/page.html#section1";
						try {
							return new URI(uriReference);
						} catch (URISyntaxException e) {
							return String.valueOf(uriReference);
						}
					} else {
						// generic string without a format.
						return typeNode;
					}
				}
			}
		}
	}

	private static Map<String, Object> getMergedReadWriteExampleMaps(
			Map<String, Object> readExamplesMap, Map<String, Object> writeExamplesMap) {
		final var mergedExamplesMap = new HashMap<String, Object>();

		final var underscore = "_";
		final var arraySuffix = "Array";
		final var exampleInName = "Example";

		final var exampleNameSuffix = underscore + exampleInName;
		final var exampleArrayNameSuffix = underscore + arraySuffix + exampleInName;

		final var readPrefix = underscore + "Response";
		final var writePrefix = underscore + "Request";

		final var readNameSuffix = readPrefix + exampleInName;
		final var readArrayNameSuffix = readPrefix + arraySuffix + exampleInName;

		final var writeNameSuffix = writePrefix + exampleInName;
		final var writeArrayNameSuffix = writePrefix + arraySuffix + exampleInName;

		readExamplesMap.forEach(
				(key, value) -> {
					if (writeExamplesMap.containsKey(key)) {
						if (value != null && value.equals(writeExamplesMap.get(key))) {
							mergedExamplesMap.put(key + exampleNameSuffix, value);
							mergedExamplesMap.put(key + exampleArrayNameSuffix, List.of(value));
						} else {
							mergedExamplesMap.put(key + readNameSuffix, value);
							mergedExamplesMap.put(key + readArrayNameSuffix, List.of(value));
							mergedExamplesMap.put(key + writeNameSuffix, writeExamplesMap.get(key));
							mergedExamplesMap.put(key + writeArrayNameSuffix, List.of(writeExamplesMap.get(key)));
						}
					} else {
						mergedExamplesMap.put(key + exampleNameSuffix, value);
						mergedExamplesMap.put(key + exampleArrayNameSuffix, List.of(value));
					}
				});

		writeExamplesMap.forEach(
				(key, value) -> {
					if (readExamplesMap.containsKey(key)) {
						if (value != null && value.equals(readExamplesMap.get(key))) {
							mergedExamplesMap.put(key + exampleNameSuffix, value);
							mergedExamplesMap.put(key + exampleArrayNameSuffix, List.of(value));
						} else {
							mergedExamplesMap.put(key + writeNameSuffix, value);
							mergedExamplesMap.put(key + writeArrayNameSuffix, List.of(value));
							mergedExamplesMap.put(key + readNameSuffix, readExamplesMap.get(key));
							mergedExamplesMap.put(key + readArrayNameSuffix, List.of(readExamplesMap.get(key)));
						}
					} else {
						mergedExamplesMap.put(key + exampleNameSuffix, value);
						mergedExamplesMap.put(key + exampleArrayNameSuffix, List.of(value));
					}
				});

		return mergedExamplesMap;
	}

	public static Paths generatePathExamples(Paths paths, Map<String, Example> examples) {
		final var updatedPaths = (Paths) paths.clone();
		updatedPaths.forEach(
				(k, v) -> {
					v.readOperationsMap()
							.forEach(
									(httpMethod, operation) -> {
										if (HttpMethod.GET.equals(httpMethod)) {
											final var responses = operation.getResponses();
											if (responses != null) {
												responses.forEach(
														(responseName, apiResponse) -> {
															if (apiResponse != null) {
																final var content = apiResponse.getContent();
																if (content != null) {
																	content.forEach(
																			(contentName, mediaType) -> {
																				// Map of the schema reference and flag indicating whether
																				// it is an array.
																				final var schemaRefMap = new HashMap<String, Boolean>();

																				final var mediaTypeSchema = mediaType.getSchema();
																				final var isArrayRef =
																						"array".equals(mediaTypeSchema.getType())
																								&& mediaTypeSchema.getItems() != null
																								&& mediaTypeSchema.getItems().get$ref() != null;
																				String schemaRef = null;
																				if (isArrayRef) {
																					schemaRef = mediaTypeSchema.getItems().get$ref();
																				} else {
																					schemaRef = mediaTypeSchema.get$ref();
																				}

																				if (schemaRef != null) {
																					schemaRefMap.put(schemaRef, isArrayRef);
																				} else {
																					final var composedSchemaItemsList = new HashSet<Schema>();
																					if (mediaTypeSchema.getAllOf() != null) {
																						composedSchemaItemsList.addAll(
																								mediaTypeSchema.getAllOf());
																					}
																					if (mediaTypeSchema.getAnyOf() != null) {
																						composedSchemaItemsList.addAll(
																								mediaTypeSchema.getAnyOf());
																					}
																					if (mediaTypeSchema.getOneOf() != null) {
																						composedSchemaItemsList.addAll(
																								mediaTypeSchema.getOneOf());
																					}

																					composedSchemaItemsList.forEach(
																							(schema) -> {
																								final var composedSchemaRef =
																										((Schema) schema).get$ref();
																								if (composedSchemaRef != null) {
																									schemaRefMap.put(composedSchemaRef, isArrayRef);
																								}
																							});
																				}

																				// Now add all the examples to the content
																				schemaRefMap.forEach(
																						(schemaReference, isReferenceAnArray) -> {
																							final var exampleReference =
																									schemaReference.replace(
																											"/schemas/", "/examples/");
																							final var refParts = schemaReference.split("/");
																							final var exampleBaseName =
																									refParts[refParts.length - 1];
																							final var mediaTypeExampleName =
																									StringUtils.insertCharBetweenLowerAndUpper(
																													exampleBaseName, '_')
																											+ "_details";
																							final var exampleSuffix =
																									isReferenceAnArray ? "_ArrayExample" : "_Example";
																							final var readExampleSuffix =
																									isReferenceAnArray
																											? "_ResponseArrayExample"
																											: "_ResponseExample";
																							if (examples.containsKey(
																									exampleBaseName + exampleSuffix)) {
																								mediaType.addExamples(
																										mediaTypeExampleName,
																										new Example()
																												.$ref(exampleReference + exampleSuffix));
																							} else if (examples.containsKey(
																									exampleBaseName + readExampleSuffix)) {
																								mediaType.addExamples(
																										mediaTypeExampleName,
																										new Example()
																												.$ref(
																														exampleReference + readExampleSuffix));
																							} else {
																								// invalid??
																							}
																						});
																			});
																}
															}
														});
											}
										} else if (HttpMethod.POST.equals(httpMethod)
												|| HttpMethod.PUT.equals(httpMethod)) {
											final var requestBody = operation.getRequestBody();
											if (requestBody != null) {
												final var content = requestBody.getContent();
												if (content != null) {
													content.forEach(
															(contentName, mediaType) -> {
																// Map of the schema reference and flag indicating whether
																// it is an array.
																final var schemaRefMap = new HashMap<String, Boolean>();

																final var mediaTypeSchema = mediaType.getSchema();
																final var isArrayRef =
																		"array".equals(mediaTypeSchema.getType())
																				&& mediaTypeSchema.getItems() != null
																				&& mediaTypeSchema.getItems().get$ref() != null;
																String schemaRef = null;
																if (isArrayRef) {
																	schemaRef = mediaTypeSchema.getItems().get$ref();
																} else {
																	schemaRef = mediaTypeSchema.get$ref();
																}

																if (schemaRef != null) {
																	schemaRefMap.put(schemaRef, isArrayRef);
																} else {
																	final var composedSchemaItemsList = new HashSet<Schema>();
																	if (mediaTypeSchema.getAllOf() != null) {
																		composedSchemaItemsList.addAll(mediaTypeSchema.getAllOf());
																	}
																	if (mediaTypeSchema.getAnyOf() != null) {
																		composedSchemaItemsList.addAll(mediaTypeSchema.getAnyOf());
																	}
																	if (mediaTypeSchema.getOneOf() != null) {
																		composedSchemaItemsList.addAll(mediaTypeSchema.getOneOf());
																	}

																	composedSchemaItemsList.forEach(
																			(schema) -> {
																				final var composedSchemaRef = ((Schema) schema).get$ref();
																				if (composedSchemaRef != null) {
																					schemaRefMap.put(composedSchemaRef, isArrayRef);
																				}
																			});
																}

																// Now add all the examples to the content
																schemaRefMap.forEach(
																		(schemaReference, isReferenceAnArray) -> {
																			final var exampleReference =
																					schemaReference.replace("/schemas/", "/examples/");
																			final var refParts = schemaReference.split("/");
																			final var exampleBaseName = refParts[refParts.length - 1];
																			final var mediaTypeExampleName =
																					StringUtils.insertCharBetweenLowerAndUpper(
																									exampleBaseName, '_')
																							+ "_details";
																			final var exampleSuffix =
																					isReferenceAnArray ? "_ArrayExample" : "_Example";
																			final var writeExampleSuffix =
																					isReferenceAnArray
																							? "_RequestArrayExample"
																							: "_RequestExample";
																			if (examples.containsKey(exampleBaseName + exampleSuffix)) {
																				mediaType.addExamples(
																						mediaTypeExampleName,
																						new Example().$ref(exampleReference + exampleSuffix));
																			} else if (examples.containsKey(
																					exampleBaseName + writeExampleSuffix)) {
																				mediaType.addExamples(
																						mediaTypeExampleName,
																						new Example()
																								.$ref(exampleReference + writeExampleSuffix));
																			} else {
																				// invalid??
																			}
																		});
															});
												}
											}
										}
									});
				});
		return updatedPaths;
	}
}
