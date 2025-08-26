package edu.isi.oba.config;

import static edu.isi.oba.utils.ObaUtils.get_yaml_data;

import edu.isi.oba.BaseTest;
import edu.isi.oba.utils.ObaUtils;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class YamlConfigTest extends BaseTest {
	@Test
	public void getSelectedClasses() {
		String config_test_file_path = "examples/dbpedia/config_music.yaml";
		YamlConfig config_data = get_yaml_data(config_test_file_path);
		Set<String> expected =
				Set.of("http://dbpedia.org/ontology/Genre", "http://dbpedia.org/ontology/Band");
		Set<String> config = config_data.getClasses();
		Assertions.assertEquals(expected, config);
	}

	/**
	 * This test attempts to load a local ontology.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testLocalFile() throws Exception {
		final var local_ontology = "src/test/config/mcat_reduced.yaml";
		final var config_data = get_yaml_data(local_ontology);
		Assertions.assertEquals(false, config_data.getOntologies().isEmpty());
	}

	/**
	 * This test attempts to load a config in a folder with spaces.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testSpacesInPath() throws Exception {
		final var local_ontology = "examples/example with spaces/config.yaml";
		final var config_data = get_yaml_data(local_ontology);
		Assertions.assertEquals(false, config_data.getOntologies().isEmpty());
	}

	/**
	 * This test attempts to run OBA with an online ontology through a URI. The ontology is hosted in
	 * GitHub, but there is a small risk of the test not passing due to the unavailability of the
	 * ontology.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testRemoteOntology() throws Exception {
		final var example_remote = "src/test/config/pplan.yaml";
		final var config_data = get_yaml_data(example_remote);
		Assertions.assertEquals(false, config_data.getOntologies().isEmpty());
	}

	/** Test an ontology (very simple, two classes) with a missing import */
	@Test
	public void testMissingImportOntology() throws Exception {
		final var example_remote = "src/test/resources/missing_import/config.yaml";
		final var config_data = get_yaml_data(example_remote);
		Assertions.assertEquals(false, config_data.getOntologies().isEmpty());
	}

	/**
	 * This test will try to load a file that does not exits. The exception is captured and reported.
	 * This test will pass IF you see an error on the output terminal
	 *
	 * @throws OWLOntologyCreationException
	 */
	@Test
	public void missingFile() throws OWLOntologyCreationException {
		final var missing_file = "src/test/config/missing_file.yaml";

		try {
			final var config_data = ObaUtils.get_yaml_data(missing_file);
			config_data.processConfig();
			Assertions.fail("Missing file: If no exception is launched, fail test");
		} catch (Exception e) {
			// pass test if there is an exception
		}
	}
}
