package edu.isi.oba;

import edu.isi.oba.config.AuthConfig;
import edu.isi.oba.config.CONFIG_FLAG;
import edu.isi.oba.config.FirebaseConfig;
import edu.isi.oba.config.Provider;
import edu.isi.oba.config.YamlConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

class Oba {
	static Logger logger = null;

	public static void main(String[] args) throws Exception {
		/*
		TODO: we are supporting one language. Issue #42
		*/

		InputStream stream = Oba.class.getClassLoader().getResourceAsStream("logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(stream);
			logger = Logger.getLogger(Oba.class.getName());
			logger.setUseParentHandlers(false); // remove double logging

		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler());

		// parse command line
		String config_yaml = ObaUtils.get_config_yaml(args);
		// read the config yaml from command line
		YamlConfig config_data = new YamlConfig();

		try {
			config_data = ObaUtils.get_yaml_data(config_yaml);
		} catch (Exception e) {
			logger.severe("Error parsing the configuration file. Please make sure it is valid \n " + e);
			System.exit(1);
		}

		String destination_dir = config_data.getOutput_dir() + File.separator + config_data.getName();
		FirebaseConfig firebase_data = config_data.getFirebase();
		AuthConfig authConfig = config_data.getAuth();
		if (authConfig != null) {

			Provider provider = authConfig.getProvider_obj();
			if (provider.equals(Provider.FIREBASE) && firebase_data.getKey() == null) {
				logger.severe("You must set up the firebase key");
				System.exit(1);
			}
		} else {
			config_data.setAuth(new AuthConfig());
		}

		try {
			Mapper mapper = new Mapper(config_data);
			mapper.createSchemas();

			try {
				MarkdownWriter.writeMarkdownFile(config_data, mapper);
			} catch (Exception e) {
				logger.severe("Error while creating/writing markdown file: " + e.getLocalizedMessage());
				e.printStackTrace();
			}

			LinkedHashMap<String, PathItem> custom_paths = config_data.getCustom_paths();
			OpenAPI openapi_base = config_data.getOpenapi();

			// get schema and paths
			generate_openapi_spec(
					openapi_base,
					mapper,
					destination_dir,
					custom_paths,
					config_data.getConfigFlagValue(CONFIG_FLAG.GENERATE_JSON_FILE));
			logger.info("OBA finished successfully. Output can be found at: " + destination_dir);
		} catch (Exception e) {
			logger.severe("Error while creating the API specification: " + e.getLocalizedMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void generate_openapi_spec(
			OpenAPI openapi_base,
			Mapper mapper,
			String dir,
			LinkedHashMap<String, PathItem> custom_paths,
			Boolean saveAsJSON)
			throws Exception {
		String destinationProjectDirectory = dir + File.separator;
		Path destinationProject = Paths.get(destinationProjectDirectory);
		new Serializer(mapper, destinationProject, openapi_base, custom_paths, saveAsJSON);
	}
}
