package edu.isi.oba;

import edu.isi.oba.utils.ObaUtils;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class ObaUtilsTest extends BaseTest {
	@Test
	public void readJSONFile() throws IOException {
		final var actual = ObaUtils.read_json_file("json_one.json");
		Assertions.assertNotNull(actual.get("@context"));
	}

	@Test
	public void mergeJSONObjects() throws IOException {
		final var one = ObaUtils.read_json_file("json_one.json");
		final var two = ObaUtils.read_json_file("json_two.json");
		final var merge = ObaUtils.mergeJSONObjects(one, two);
		Assertions.assertNotNull(merge.get("@context"));
		Assertions.assertNotNull(merge.get("@context"));
	}

	@Test
	public void concatJSONCommonKey() throws IOException {
		final var one = ObaUtils.read_json_file("json_one.json");
		final var two = ObaUtils.read_json_file("json_two.json");
		final var three = ObaUtils.read_json_file("json_three.json");
		final var jsons = new JSONObject[] {one, two, three};
		final var merge = ObaUtils.concat_json_common_key(jsons, "@context");
		final var o = (JSONObject) merge.get("@context");
		Assertions.assertNotNull(o.get("Entity"));
		Assertions.assertNotNull(o.get("Model"));
		Assertions.assertNotNull(o.get("Setup"));
	}

	@Test
	public void getDescription() throws OWLOntologyCreationException {
		final var example_remote = "src/test/config/pplan.yaml";
		final var config_data = ObaUtils.get_yaml_data(example_remote);

		try {
			final var planClass =
					config_data
							.getManager()
							.getOWLDataFactory()
							.getOWLClass("http://purl.org/net/p-plan#Plan");

			config_data.getOwlOntologies().stream()
					.forEach(
							(ontology) -> {
								if (ontology.containsEntityInSignature(planClass)) {
									String desc = ObaUtils.getDescription(planClass, ontology, true);
									Assertions.assertNotEquals("", desc);
								}
							});
		} catch (Exception e) {
			Assertions.fail("Failed to get description.", e);
		}
	}
}
