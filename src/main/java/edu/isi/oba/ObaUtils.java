package edu.isi.oba;

import static edu.isi.oba.Oba.logger;

import edu.isi.oba.config.YamlConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import rita.RiTa;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;

public class ObaUtils {
	public static final String DEFAULT_DESCRIPTION = "Description not available";
	public static final String[] POSSIBLE_VOCAB_SERIALIZATIONS = {
		"application/rdf+xml", "text/turtle", "text/n3", "application/ld+json"
	};

	private static final String DCELEMENTS_NS = "http://purl.org/dc/elements/1.1/";
	private static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	private static final String PROV_NS = "http://www.w3.org/ns/prov#";
	private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final String SKOS_NS = "http://www.w3.org/2004/02/skos/core#";

	public static final String DCELEMENTS_DEFINITION = DCELEMENTS_NS + "description";
	public static final String DCTERMS_DEFINITION = DCTERMS_NS + "description";
	public static final String RDFS_COMMENT = RDFS_NS + "comment";
	public static final String SKOS_DEFINITION = SKOS_NS + "definition";
	public static final String PROV_DEFINITION = PROV_NS + "definition";

	public static final List<String> DESCRIPTION_PROPERTIES =
			new ArrayList<>(
					Arrays.asList(
							DCELEMENTS_DEFINITION,
							DCTERMS_DEFINITION,
							RDFS_COMMENT,
							SKOS_DEFINITION,
							PROV_DEFINITION));

	public static void write_file(String file_path, String content) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file_path));
			writer.write(content);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Code to unzip a file. Inspired from
	 * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/ Taken from
	 *
	 * @param resourceName
	 * @param outputFolder
	 */
	public static void unZipIt(String resourceName, String outputFolder) {

		byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(ObaUtils.class.getResourceAsStream(resourceName));
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(outputFolder + File.separator + fileName);
				// System.out.println("file unzip : "+ newFile.getAbsoluteFile());
				if (ze.isDirectory()) {
					String temp = newFile.getAbsolutePath();
					new File(temp).mkdirs();
				} else {
					String directory = newFile.getParent();
					if (directory != null) {
						File d = new File(directory);
						if (!d.exists()) {
							d.mkdirs();
						}
					}
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Exception while copying resource. " + ex.getMessage());
		}
	}

	public static void copy(InputStream is, File dest) throws Exception {
		OutputStream os = null;
		try {
			os = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length;
			while ((length = is.read(buffer)) > 0) {
				os.write(buffer, 0, length);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error while extracting the reosurces: " + e.getMessage());
			throw e;
		} finally {
			if (is != null) is.close();
			if (os != null) os.close();
		}
	}

	/**
	 * This function recursively copy all the sub folder and files from sourceFolder to
	 * destinationFolder
	 */
	public static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
		// Check if sourceFolder is a directory or file
		// If sourceFolder is file; then copy the file directly to new location
		if (sourceFolder.isDirectory()) {
			// Verify if destinationFolder is already present; If not then create it
			if (!destinationFolder.exists()) {
				destinationFolder.mkdir();
				System.out.println("Directory created :: " + destinationFolder);
			}

			// Get all files from source directory
			String files[] = sourceFolder.list();

			// Iterate over all files and copy them to destinationFolder one by one
			for (String file : files) {
				File srcFile = new File(sourceFolder, file);
				File destFile = new File(destinationFolder, file);

				// Recursive function call
				copyFolder(srcFile, destFile);
			}
		} else {
			// Copy the file content from one place to another
			Files.copy(
					sourceFolder.toPath(), destinationFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("File copied :: " + destinationFolder);
		}
	}

	public static String get_config_yaml(String[] args) {
		// obtain the options to pass configuration
		Options options = new Options();
		Option input = new Option("c", "config", true, "configuration file path");
		input.setRequired(true);
		options.addOption(input);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		String config_yaml = null;

		try {
			cmd = parser.parse(options, args);
			config_yaml = cmd.getOptionValue("config");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			formatter.printHelp("utiConfiguration filelity-name", options);
			System.exit(1);
		}
		return config_yaml;
	}

	public static YamlConfig get_yaml_data(String config_yaml) {
		Constructor constructor = new Constructor(YamlConfig.class, new LoaderOptions());
		Yaml yaml = new Yaml(constructor);

		InputStream config_input = null;
		try {
			config_input = new FileInputStream(new File(config_yaml));
		} catch (FileNotFoundException e) {
			System.err.println("Configuration file not found: " + config_yaml);
			System.exit(1);
		}
		// Yaml config parse
		return yaml.loadAs(config_input, YamlConfig.class);
	}

	public static JSONObject concat_json_common_key(JSONObject[] objects, String common_key) {
		JSONObject mergeJSON = (JSONObject) objects[0].get(common_key);
		for (int i = 1; i < objects.length; i++) {
			mergeJSON = mergeJSONObjects(mergeJSON, (JSONObject) objects[i].get(common_key));
		}
		return new JSONObject().put(common_key, mergeJSON);
	}

	public static JSONObject concat_json(JSONObject[] objects) {
		JSONObject mergeJSON = objects[0];
		for (int i = 1; i < objects.length; i++) {
			mergeJSON = mergeJSONObjects(mergeJSON, objects[i]);
		}
		return mergeJSON;
	}

	public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) {

		JSONObject mergedJSON = new JSONObject();
		try {
			mergedJSON = new JSONObject(json1, JSONObject.getNames(json1));
			for (String crunchifyKey : JSONObject.getNames(json2)) {
				mergedJSON.put(crunchifyKey, json2.get(crunchifyKey));
			}

		} catch (JSONException e) {
			throw new RuntimeException("JSON Exception" + e);
		}
		return mergedJSON;
	}

	/**
	 * @param file_name
	 * @return
	 * @throws IOException
	 */
	public static JSONObject read_json_file(String file_name) throws IOException {
		InputStream stream = Oba.class.getClassLoader().getResourceAsStream(file_name);
		byte b[] = new byte[stream.available()];
		JSONObject jsonObject = null;
		if (stream.read(b) == b.length) {
			jsonObject = new JSONObject(new String(b));
		}
		return jsonObject;
	}

	public static String check_trailing_slash(String string) {
		return string.endsWith("/") ? string : string + "/";
	}

	/**
	 * Method that will download an ontology given its URI, doing content negotiation The ontology
	 * will be downloaded in the first serialization available (see
	 * Constants.POSSIBLE_VOCAB_SERIALIZATIONS)
	 *
	 * @param uri the URI of the ontology
	 * @param downloadPath path where the ontology will be saved locally.
	 */
	public static void downloadOntology(String uri, String downloadPath) {
		for (String serialization : POSSIBLE_VOCAB_SERIALIZATIONS) {
			System.out.println("Attempting to download vocabulary in " + serialization);
			// logger is not initialized correctly and fails in tests (to fix)
			// logger.info("Attempting to download vocabulary in " + serialization);
			try {
				URL url = new URL(uri);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setInstanceFollowRedirects(true);
				connection.setRequestProperty("Accept", serialization);
				int status = connection.getResponseCode();
				boolean redirect = false;
				if (status != HttpURLConnection.HTTP_OK) {
					if (status == HttpURLConnection.HTTP_MOVED_TEMP
							|| status == HttpURLConnection.HTTP_MOVED_PERM
							|| status == HttpURLConnection.HTTP_SEE_OTHER) redirect = true;
				}
				// there are some vocabularies with multiple redirections:
				// 301 -> 303 -> owl
				while (redirect) {
					String newUrl = connection.getHeaderField("Location");
					connection = (HttpURLConnection) new URL(newUrl).openConnection();
					connection.setRequestProperty("Accept", serialization);
					status = connection.getResponseCode();
					if (status != HttpURLConnection.HTTP_MOVED_TEMP
							&& status != HttpURLConnection.HTTP_MOVED_PERM
							&& status != HttpURLConnection.HTTP_SEE_OTHER) redirect = false;
				}
				InputStream in = (InputStream) connection.getInputStream();
				Files.copy(in, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING);
				in.close();
				break; // if the vocabulary is downloaded, then we don't download it for the other
				// serializations
			} catch (Exception e) {
				final String message =
						"Failed to download vocabulary in RDF format [" + serialization + "]: ";
				logger.severe(message + e.toString());
				throw new RuntimeException(message, e);
			}
		}
	}

	/**
	 * Method that given a class, property or data property, searches for the best description.
	 *
	 * @param entity entity to search.
	 * @param ontology ontology to be used to search descriptions.
	 * @param hasDefaultDescriptions flag indicating whether default descriptions should or should not
	 *     be included.
	 * @return Description string
	 */
	public static String getDescription(
			OWLEntity entity, OWLOntology ontology, Boolean hasDefaultDescriptions, String languageTag) {
		// Default to English (i.e. "en"), if null or empty/blank string
		if (languageTag == null || languageTag.isBlank()) {
			languageTag = "en";
		}

		var descriptionCount = 0;
		if (entity instanceof OWLObjectProperty) {
			for (final var description : ObaUtils.DESCRIPTION_PROPERTIES) {
				descriptionCount +=
						EntitySearcher.getAnnotationObjects(
										entity,
										Set.of(ontology).stream(),
										new OWLAnnotationPropertyImpl(new IRI(description) {}))
								.count();
			}

			if (descriptionCount == 0) {
				for (final var objPropRange :
						ontology.getObjectPropertyRangeAxioms(
								((OWLObjectProperty) entity).asObjectPropertyExpression())) {
					if (objPropRange.getRange() instanceof OWLClass) {
						entity = objPropRange.getRange().asOWLClass();
					}
				}
			}
		}

		// Use a map to keep track of all the description annotations.
		final var langDescMap = new HashMap<String, String>();

		for (final var description : ObaUtils.DESCRIPTION_PROPERTIES) {
			final var annotationObjectsStream =
					EntitySearcher.getAnnotationObjects(
							entity,
							Set.of(ontology).stream(),
							new OWLAnnotationPropertyImpl(new IRI(description) {}));
			final var annotationObjects = annotationObjectsStream.collect(Collectors.toSet());

			for (final var annotationObj : annotationObjects) {
				final var optionalDescriptionLiteral =
						((OWLAnnotation) annotationObj).getValue().asLiteral();

				if (optionalDescriptionLiteral.isPresent()) {
					final var descriptionLiteral = optionalDescriptionLiteral.get();

					// A description is present, but if it has no language tag, use empty string "".
					// If there are multiple descriptions annotations with no language tag, only the last one
					// encountered will be used.
					langDescMap.put(
							descriptionLiteral.getLang() == null ? "" : descriptionLiteral.getLang(),
							descriptionLiteral.getLiteral());
				}
			}
		}

		// Return description for the specified language tag, if it exists.  Otherwise, return
		// description with no language tag, if it exists.
		if (langDescMap.containsKey(languageTag)) {
			return langDescMap.get(languageTag);
		} else if (langDescMap.containsKey("")) {
			return langDescMap.get("");
		}

		// If no description was found, then return default description (if configured to do so) or
		// null, otherwise.
		return !Optional.ofNullable(hasDefaultDescriptions).orElse(false)
				? null
				: ObaUtils.DEFAULT_DESCRIPTION;
	}

	/**
	 * Convenience overloaded method for getDescription() that searches for the best English
	 * description of class, property, or data property.
	 *
	 * @param entity entity to search.
	 * @param ontology ontology to be used to search descriptions.
	 * @param hasDefaultDescriptions flag indicating whether default descriptions should or should not
	 *     be included.
	 * @return A string of the entity's English description.
	 */
	public static String getDescription(
			OWLEntity entity, OWLOntology ontology, Boolean hasDefaultDescriptions) {
		return ObaUtils.getDescription(entity, ontology, hasDefaultDescriptions, "en");
	}

	/**
	 * Convert a PascalCase (or camelCase) string to kebab-case.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String pascalCaseToKebabCase(String str) {
		return str.replaceAll("\\B([A-Z])(?=[a-z])", "-$1")
				.replaceAll("\\B([a-z0-9])([A-Z])", "$1-$2")
				.replaceAll(
						"Ph-D-",
						"PhD-") // Annoying workaround for "PhD" which usually occurs together as one "word"
				.toLowerCase();
	}

	/**
	 * Convert a kebab-case string to PascalCase.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String kebabCaseToCamelCase(String str) {
		return Pattern.compile("-(.)").matcher(str).replaceAll(mr -> mr.group(1).toUpperCase());
	}

	/**
	 * Convert a kebab-case string to PascalCase.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String kebabCaseToPascalCase(String str) {
		final var camelCaseStr = ObaUtils.kebabCaseToCamelCase(str);
		return camelCaseStr.substring(0, 1).toUpperCase() + camelCaseStr.substring(1);
	}

	public static String getPluralOf(String str) {
		// Pluralizing currently only works for English.  Non-English words will be treated as though
		// they are English.
		// TODO: Java support for singularization/pluralization and locale/international support supoort
		// for the process does not have many good options that we could find so far.
		// TODO: If such an option exists or becomes available, this should be updated to support
		// pluralization in other languages.
		// TODO: The language/locale would need to be set as a configuration value and passed into this
		// class somehow.

		return RiTa.pluralize(str);
	}

	public static String getLowerCasePluralOf(String str) {
		return ObaUtils.getPluralOf(str.toLowerCase());
	}

	public static String getSingularOf(String str) {
		// Pluralizing currently only works for English.  Non-English words will be treated as though
		// they are English.
		// TODO: Java support for singularization/pluralization and locale/international support supoort
		// for the process does not have many good options that we could find so far.
		// TODO: If such an option exists or becomes available, this should be updated to support
		// pluralization in other languages.
		// TODO: The language/locale would need to be set as a configuration value and passed into this
		// class somehow.

		return RiTa.singularize(str);
	}

	public static String getLowerCaseSingularOf(String str) {
		return ObaUtils.getSingularOf(str.toLowerCase());
	}
}
