package edu.isi.oba;

import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.utils.cli.CliUtils;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import edu.isi.oba.utils.yaml.YamlUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Oba {
	public static Logger logger = null;

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
		String config_yaml = CliUtils.getYamlConfigFileArgument(args);
		// read the config yaml from command line
		YamlConfig config_data = new YamlConfig();

		try {
			config_data = YamlUtils.getYamlData(config_yaml);
		} catch (Exception e) {
			FatalErrorHandler.fatal(
					"Error parsing the configuration file. Please make sure it is valid \n " + e);
		}

		String destination_dir = config_data.getOutputDir() + File.separator + config_data.getName();
		try {
			Mapper mapper = new Mapper(config_data);
			mapper.createSchemas();

			try {
				MarkdownWriter.writeMarkdownFile(config_data, mapper);
			} catch (Exception e) {
				logger.severe("Error while creating/writing markdown file: " + e.getLocalizedMessage());
				e.printStackTrace();
			}

			final var custom_paths = config_data.getCustomPaths();
			OpenAPI openapi_base = config_data.getOpenapi();

			// get schema and paths
			Oba.generate_openapi_spec(openapi_base, mapper, destination_dir, custom_paths, config_data);
			logger.info("OBA finished successfully. Output can be found at: " + destination_dir);
		} catch (Exception e) {
			FatalErrorHandler.fatal(
					"Error while creating the API specification: " + e.getLocalizedMessage());
		}
	}

	private static void generate_openapi_spec(
			OpenAPI openapi_base,
			Mapper mapper,
			String dir,
			Map<String, PathItem> custom_paths,
			YamlConfig configData)
			throws Exception {
		String destinationProjectDirectory = dir + File.separator;
		Path destinationProject = Paths.get(destinationProjectDirectory);
		new Serializer(mapper, destinationProject, openapi_base, custom_paths, configData);
	}
}
