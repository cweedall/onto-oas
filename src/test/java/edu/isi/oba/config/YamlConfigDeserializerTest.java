package edu.isi.oba.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link YamlConfigDeserializer}. */
public class YamlConfigDeserializerTest {

	/** Mix-in class to associate {@link YamlConfigDeserializer} with {@link YamlConfig}. */
	public abstract static class YamlConfigMixin {
		// No implementation needed; used only for annotation mapping
	}

	/** Tests that a YAML configuration file is correctly deserialized and processed. */
	@Test
	public void testDeserializeAndProcessConfig() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.addMixIn(YamlConfig.class, YamlConfigMixin.class);

		try {
			File yamlFile = new File("src/test/config/dbpedia.yaml");
			YamlConfig config = mapper.readValue(yamlFile, YamlConfig.class);

			assertNotNull(config);
			assertNotNull(config.getOntologies());
			assertNotNull(config.getAllowedOwlClasses());
			assertTrue(config.getAllowedOwlClasses().size() > 0 || config.getOntologies().size() > 0);

		} catch (IOException e) {
			fail("Deserialization or processing failed: " + e.getMessage());
		}
	}
}
