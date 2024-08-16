package edu.isi.oba;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class Serializer {
  String openapi_path;

  public  Serializer(Mapper mapper, java.nio.file.Path dir, OpenAPI openAPI, LinkedHashMap<String, PathItem> custom_paths, Boolean saveAsJSON) throws Exception {
    Map<String, Object> extensions = new HashMap<String, Object>();
    final String openapi_file = Optional.ofNullable(saveAsJSON).orElse(false) ? "openapi.json" : "openapi.yaml";

    //Generate security schema
    Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
    SecurityScheme securityScheme = getSecurityScheme(extensions);
    securitySchemes.put("BearerAuth", securityScheme);

    Components components = new Components();
    mapper.getSchemas().forEach((k, v) -> components.addSchemas(k, v));
    components.securitySchemes(securitySchemes);

    Paths paths = new Paths();
    mapper.getPaths().forEach((k, v) -> {
      paths.addPathItem(k, v);
      
      final var tags = new HashSet<Tag>();

      if (openAPI.getTags() != null) {
        // Copy the List of OpenAPI tags (defined in the configuration file, if at all).
        tags.addAll(openAPI.getTags().stream().collect(Collectors.toSet()));
      }

      // Remove existing Tags so that we make sure everything is in alphabetical order with the "tags" Set<Tag>.
      openAPI.setTags(null);

      // For each operation, grab any Tags that exist and add them to the Set of Tags.
      v.readOperations().forEach((operation) -> {
        operation.getTags().stream().forEach((operationTagName) -> {
          final var tagObj = new Tag();
          tagObj.setName(operationTagName);

          // There should always be one path/endpoint tag which is the schema's name.
          // Getting the tags from the operation only returns a List<String> (where String is the name of the Tag).
          // This appears to be a quirk between the operation tags and the global tags which have a description and externalUrl.
          // This grabs the schema's description by searching for the schema's name.
          final var schemas = components.getSchemas();
          var tagDescription = "";
          if (schemas != null && schemas.get(operationTagName) != null) {
            tagDescription = components.getSchemas().get(operationTagName).getDescription();
          }

          // Use a generic description if one was not found.
          if (tagDescription == null || tagDescription.isBlank()) {
            tagObj.setDescription(operationTagName + " description not set in the ontology.");
          } else {
            tagObj.setDescription(tagDescription);
          }

          tags.add(tagObj);
        });
      });

      // Convert Set to List and sort in alphabetical order (by Tag's name).
      openAPI.setTags(tags.stream().sorted((tag1, tag2) -> tag1.getName().compareToIgnoreCase(tag2.getName())).collect(Collectors.toList()));
    });

    //add custom paths
    Map<String, Object> custom_extensions = new HashMap<String, Object>();
    custom_extensions.put("x-oba-custom", true);

    if (custom_paths != null)
      custom_paths.forEach((k, v) -> {
        System.out.println("inserting custom query " + k);
        v.setExtensions(custom_extensions);
        paths.addPathItem(k, v);
      });

    openAPI.setPaths(paths);
    openAPI.components(components);

    //write the filename
    final var content = Optional.ofNullable(saveAsJSON).orElse(false) ? Json.pretty().writeValueAsString(openAPI) : Yaml.pretty().writeValueAsString(openAPI);
    this.openapi_path = dir + File.separator + openapi_file;
    File file = new File(openapi_path);
    BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
    writer.write(content);
    writer.close();
    this.validate();
  }

  private void validate() throws Exception {
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    SwaggerParseResult result = new OpenAPIV3Parser().readLocation(openapi_path, null, options);
    List<String> messageList = result.getMessages();
    Set<String> errors = new HashSet<>(messageList);
    Set<String> warnings = new HashSet<>();
    errors.forEach((errorMessage) -> {
      if (errorMessage.contains(".$ref target #/components/schemas/") 
          && errorMessage.contains("is not of expected type Examples")) {
        // Ignore.  The validator complains because the reference is to a Schemas item (which is valid) instead of an Examples item.
      } else {
        throw new RuntimeException(errorMessage);
      }
    });
  }

  private SecurityScheme getSecurityScheme(Map<String, Object> extensions) {
    extensions.put("x-bearerInfoFunc", "openapi_server.controllers.user_controller.decode_token");
    SecurityScheme securityScheme = new SecurityScheme();
    securityScheme.setType(SecurityScheme.Type.HTTP);
    securityScheme.bearerFormat("JWT");
    securityScheme.setScheme("bearer");
    securityScheme.setExtensions(extensions);
    return securityScheme;
  }
}
