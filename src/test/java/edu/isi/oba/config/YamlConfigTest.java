package edu.isi.oba.config;

import edu.isi.oba.BaseTest;
import edu.isi.oba.config.flags.GlobalFlags;
import edu.isi.oba.exceptions.ConfigValidationException;
import edu.isi.oba.utils.yaml.YamlUtils;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class YamlConfigTest extends BaseTest {

	@Test
	public void getSelectedClasses() {
		String config_test_file_path = "examples/dbpedia/config_music.yaml";
		YamlConfig config_data = YamlUtils.getYamlData(config_test_file_path);
		Set<String> expected =
				Set.of("http://dbpedia.org/ontology/Genre", "http://dbpedia.org/ontology/Band");
		Set<String> config = config_data.getExtraClassSchemas();
		Assertions.assertEquals(expected, config);
	}

	@Test
	public void testLocalFile() throws Exception {
		final var local_ontology = "src/test/config/mcat_reduced.yaml";
		final var config_data = YamlUtils.getYamlData(local_ontology);
		Assertions.assertFalse(config_data.getOntologies().isEmpty());
	}

	@Test
	public void testSpacesInPath() throws Exception {
		final var local_ontology = "examples/example with spaces/config.yaml";
		final var config_data = YamlUtils.getYamlData(local_ontology);
		Assertions.assertFalse(config_data.getOntologies().isEmpty());
	}

	@Test
	public void testRemoteOntology() throws Exception {
		final var example_remote = "src/test/config/pplan.yaml";
		final var config_data = YamlUtils.getYamlData(example_remote);
		Assertions.assertFalse(config_data.getOntologies().isEmpty());
	}

	@Test
	public void testMissingImportOntology() throws Exception {
		final var example_remote = "src/test/resources/missing_import/config.yaml";
		final var config_data = YamlUtils.getYamlData(example_remote);
		Assertions.assertFalse(config_data.getOntologies().isEmpty());
	}

	@Test
	public void missingFile() throws OWLOntologyCreationException {
		final var missing_file = "src/test/config/missing_file.yaml";
		try {
			final var config_data = YamlUtils.getYamlData(missing_file);
			Assertions.fail("Missing file: If no exception is launched, fail test");
		} catch (Exception e) {
			// pass test if there is an exception
		}
	}

	@Test
	public void testValidationFailsWithMissingFields() {
		YamlConfig config = new YamlConfig();
		config.setOpenapi(null);
		Assertions.assertThrows(ConfigValidationException.class, config::validate);
	}

	@Test
	public void testFlagSettersUpdateGlobalFlags() {
		YamlConfig config = new YamlConfig();
		config.setAlwaysGenerateArrays(false);
		Assertions.assertFalse(GlobalFlags.getFlag(ConfigPropertyNames.ALWAYS_GENERATE_ARRAYS));

		config.setDefaultDescriptions(true);
		Assertions.assertTrue(GlobalFlags.getFlag(ConfigPropertyNames.DEFAULT_DESCRIPTIONS));
	}

	@Test
	public void testCreateOutputDir(@TempDir File tempDir) throws Exception {
		YamlConfig config = new YamlConfig();

		// Use reflection to set private fields
		var outputDirField = YamlConfig.class.getDeclaredField("outputDir");
		outputDirField.setAccessible(true);
		outputDirField.set(config, tempDir.getAbsolutePath());

		var nameField = YamlConfig.class.getDeclaredField("name");
		nameField.setAccessible(true);
		nameField.set(config, "test_project");

		config.createOutputDir();

		File expectedDir = new File(tempDir, "test_project");
		Assertions.assertTrue(expectedDir.exists());
		Assertions.assertTrue(expectedDir.isDirectory());
	}
}
