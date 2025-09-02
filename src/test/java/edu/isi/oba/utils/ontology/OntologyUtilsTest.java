package edu.isi.oba.utils.ontology;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.config.YamlConfig;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class OntologyUtilsTest {

	@Test
	public void testGetDescription() throws OWLOntologyCreationException {
		String path = "src/test/config/pplan.yaml";
		YamlConfig config = edu.isi.oba.utils.yaml.YamlUtils.getYamlData(path);
		final var planClass =
				config.getManager().getOWLDataFactory().getOWLClass("http://purl.org/net/p-plan#Plan");

		config.getOwlOntologies().stream()
				.forEach(
						ontology -> {
							if (ontology.containsEntityInSignature(planClass)) {
								String desc = OntologyUtils.getDescription(planClass, ontology, true);
								assertNotEquals("", desc);
							}
						});
	}
}
