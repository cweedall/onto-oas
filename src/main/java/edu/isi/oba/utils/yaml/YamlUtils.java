package edu.isi.oba.utils.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.isi.oba.config.YamlConfig;
import edu.isi.oba.config.YamlConfigMixin;
import edu.isi.oba.utils.exithandler.FatalErrorHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

public class YamlUtils {

	private static final Logger logger = Logger.getLogger(YamlUtils.class.getName());

	public static YamlConfig getYamlData(String config_yaml) {
		YamlConfig yamlConfig = null;
		try {
			ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

			// Register the mix-in to apply the custom deserialize
			objectMapper.addMixIn(YamlConfig.class, YamlConfigMixin.class);

			// Register other modules (e.g., JavaTimeModule, etc.)
			objectMapper.findAndRegisterModules();

			final var configYamlFile = new File(config_yaml);

			// Deserialize the YAML file
			yamlConfig = objectMapper.readValue(configYamlFile, YamlConfig.class);
		} catch (FileNotFoundException e) {
			FatalErrorHandler.fatal("Configuration file not found: " + config_yaml);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			FatalErrorHandler.fatal(e.getMessage());
		}

		return yamlConfig;
	}
}
