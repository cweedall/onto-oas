package edu.isi.oba;

import edu.isi.oba.utils.yaml.YamlUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class MapperTest extends BaseTest {
	String outputPath = null;

	private Mapper setupMapper(String configFilePath) throws Exception {
		Mapper mapper = null;

		try {
			final var configData = YamlUtils.getYamlData(configFilePath);
			this.outputPath = configData.getOutputDir();

			mapper = new Mapper(configData);
			// Use temporary directory for unit testing
			mapper.createSchemas();

			// If no schemas are returned from the mapper, something is wrong.  Probably with the
			// ontology(?).
			final var schemas = mapper.getSchemas();
			Assertions.assertNotNull(schemas);
		} catch (OWLOntologyCreationException e) {
			Assertions.fail("Error in ontology creation: ", e);
		}

		return mapper;
	}

	/**
	 * This test loads a local ontology and verifies the generated OpenAPI spec schema only generates
	 * models/endpoints for classes allowed by the configuration file.
	 *
	 * @throws java.lang.Exception
	 */
	@Test
	public void testFilteredClasses() throws Exception {
		// Expected values
		final var expectedResult1 = "Genre";
		final var expectedResult2 = "Band";
		final var expectedResultSize = 2;

		// The YAML config restricts allowed classes to:
		// - http://dbpedia.org/ontology/Genre
		// - http://dbpedia.org/ontology/Band
		final var configFilePath = "src/test/config/dbpedia.yaml";
		final var mapper = this.setupMapper(configFilePath);

		// The Genre and Band model schemas must exist.
		final var keys = mapper.getSchemas().keySet();
		Assertions.assertTrue(keys.contains(expectedResult1));
		Assertions.assertTrue(keys.contains(expectedResult2));
		Assertions.assertEquals(expectedResultSize, keys.size());

		// Delete temporary directory now
		if (this.outputPath != null && !this.outputPath.isBlank()) {
			Files.walk(Paths.get(this.outputPath))
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

	/** Test an ontology (very simple, two classes) with a missing import */
	@Test
	public void testComplexOntology() throws Exception {
		final var configFilePath = "src/test/resources/complex_expr/config.yaml";
		final var mapper = this.setupMapper(configFilePath);

		// The person model schema must exist.
		final var keys = mapper.getSchemas().keySet();
		Assertions.assertTrue(keys.contains("Person"));

		// Delete temporary directory now
		if (this.outputPath != null && !this.outputPath.isBlank()) {
			Files.walk(Paths.get(this.outputPath))
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}
}
