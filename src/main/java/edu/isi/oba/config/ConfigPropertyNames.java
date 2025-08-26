package edu.isi.oba.config;

/**
 * Configuration properties which project-wide.
 *
 * <p>Some are private because they are only used internally to concatenate with other constants for
 * re-usability.
 */
public class ConfigPropertyNames {
	// ---------------------------------------------------------
	//  Generic constants
	// ---------------------------------------------------------
	private static final String SEPARATOR = "_";

	// ---------------------------------------------------------
	//  Project constants
	// ---------------------------------------------------------
	public static final String NAME = "name";
	public static final String OUTPUT_DIR = "output_dir";
	public static final String ONTOLOGIES = "ontologies";
	public static final String GENERATE_JSON_FILE = "generate_json_file";
	public static final String VALIDATE_GENERATED_OPENAPI_FILE = "validate_generated_openapi_file";

	// ---------------------------------------------------------
	//  Path/endpoint constants
	// ---------------------------------------------------------
	public static final String PATH_CONFIG = "path_config";
	public static final String PATHS_FOR_CLASSES = "paths_for_classes";
	public static final String CLASS_IRI = "class_iri";
	public static final String ALLOW_OPERATIONS = "allow_operations";
	public static final String DENY_OPERATIONS = "deny_operations";
	public static final String DISABLE_ALL_PATHS = "disable_all_paths";
	public static final String PATHS = "paths";
	public static final String ENABLE = "enable";

	// Flags
	public static final String USE_KEBAB_CASE_PATHS = "use_kebab_case_paths";
	public static final String USE_COMMON_DEFAULT_PATH_RESPONSES =
			"use_common_default_path_responses";

	// ---------------------------------------------------------
	//  Generic constants for any operation
	// ---------------------------------------------------------
	private static final String ALL = "all";
	private static final String BY_KEY = "by_key";
	private static final String BULK = "bulk";
	private static final String SINGLE = "single";
	private static final String BY_POST = "by_post";
	public static final String RESPONSE_ARRAY = "response_array";
	public static final String KEY_NAME = "key_name";
	public static final String KEY_NAME_IN_TEXT = KEY_NAME + SEPARATOR + "in_text";
	public static final String KEY_TYPE = "key_type";
	public static final String PATH_SUFFIX = "path_suffix";

	// ---------------------------------------------------------
	//  DELETE operation constants
	// ---------------------------------------------------------
	public static final String DELETE = "delete";
	public static final String DELETE_PATHS = DELETE + SEPARATOR + PATHS;
	public static final String DELETE_BY_KEY = DELETE + SEPARATOR + BY_KEY;
	public static final String DELETE_BY_KEY_ENABLE = DELETE_BY_KEY + SEPARATOR + ENABLE;

	// ---------------------------------------------------------
	//  GET operation constants
	// ---------------------------------------------------------
	public static final String GET = "get";
	public static final String GET_PATHS = GET + SEPARATOR + PATHS;
	public static final String GET_ALL = GET + SEPARATOR + ALL;
	public static final String GET_ALL_ENABLE = GET_ALL + SEPARATOR + ENABLE;
	public static final String GET_BY_KEY = GET + SEPARATOR + BY_KEY;
	public static final String GET_BY_KEY_ENABLE = GET_BY_KEY + SEPARATOR + ENABLE;
	public static final String GET_BY_KEY_RESPONSE_ARRAY_ENABLE =
			GET_BY_KEY + SEPARATOR + RESPONSE_ARRAY + ENABLE;

	// ---------------------------------------------------------
	//  PATCH operation constants
	// ---------------------------------------------------------
	public static final String PATCH = "patch";
	public static final String PATCH_PATHS = PATCH + SEPARATOR + PATHS;

	// ---------------------------------------------------------
	//  POST operation constants
	// ---------------------------------------------------------
	public static final String POST = "post";
	public static final String POST_PATHS = POST + SEPARATOR + PATHS;
	public static final String POST_BULK = POST + SEPARATOR + BULK;
	public static final String POST_BULK_ENABLE = POST_BULK + SEPARATOR + ENABLE;
	public static final String POST_SINGLE = POST + SEPARATOR + SINGLE;
	public static final String POST_SINGLE_ENABLE = POST_SINGLE + SEPARATOR + ENABLE;

	// ---------------------------------------------------------
	//  PUT operation constants
	// ---------------------------------------------------------
	public static final String PUT = "put";
	public static final String PUT_PATHS = PUT + SEPARATOR + PATHS;
	public static final String PUT_BULK = PUT + SEPARATOR + BULK;
	public static final String PUT_BULK_ENABLE = PUT_BULK + SEPARATOR + ENABLE;
	public static final String PUT_BY_KEY = PUT + SEPARATOR + BY_KEY;
	public static final String PUT_BY_KEY_ENABLE = PUT_BY_KEY + SEPARATOR + ENABLE;

	// ---------------------------------------------------------
	//  SEARCH operation constants
	// ---------------------------------------------------------
	public static final String SEARCH = "search";
	public static final String SEARCH_PATHS = SEARCH + SEPARATOR + PATHS;
	public static final String SEARCH_BY_POST = SEARCH + SEPARATOR + BY_POST;
	public static final String SEARCH_BY_POST_ENABLE = SEARCH_BY_POST + SEPARATOR + ENABLE;
	public static final String SEARCH_PROPERTIES = "search_properties";
	public static final String SEARCH_PROPERTY_TYPES = "search_property_types";

	// ---------------------------------------------------------
	//  Ontology annotation constants
	// ---------------------------------------------------------
	public static final String ANNOTATION_CONFIG = "annotation_config";
	public static final String PROPERTY_ANNOTATIONS = "property_annotations";
	public static final String READ_ONLY_FLAG_NAME = "read_only_flag_name";
	public static final String WRITE_ONLY_FLAG_NAME = "write_only_flag_name";
	public static final String EXAMPLE_VALUE_NAME = "example_value_name";

	// ---------------------------------------------------------
	//  Markdown generation constants (some are based on Ontology annotations)
	// ---------------------------------------------------------
	public static final String MARKDOWN_GENERATION_FILENAME = "markdown_generation_filename";
	public static final String MARKDOWN_GENERATION_ANNOTATIONS = "markdown_generation_annotations";
	public static final String ANNOTATION_NAME = "annotation_name";
	public static final String MARKDOWN_HEADING = "markdown_heading";
	public static final String MARKDOWN_DESCRIPTION = "markdown_description";

	// ---------------------------------------------------------
	//  OpenAPI generation flag constants
	// ---------------------------------------------------------
	public static final String FOLLOW_REFERENCES = "follow_references";
	public static final String USE_INHERITANCE_REFERENCES = "use_inheritance_references";
	public static final String DEFAULT_DESCRIPTIONS = "default_descriptions";
	public static final String DEFAULT_PROPERTIES = "default_properties";
	public static final String ALWAYS_GENERATE_ARRAYS = "always_generate_arrays";
	public static final String REQUIRED_PROPERTIES_FROM_CARDINALITY =
			"required_properties_from_cardinality";
	public static final String FIX_SINGULAR_PLURAL_PROPERTY_NAMES =
			"fix_singular_plural_property_names";
}
