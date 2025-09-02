package edu.isi.oba.utils.yaml;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.config.YamlConfig;
import org.junit.jupiter.api.Test;

public class YamlUtilsTest {

	@Test
	public void testGetYamlData() {
		String path = "src/test/config/pplan.yaml";
		YamlConfig config = YamlUtils.getYamlData(path);
		assertNotNull(config);
		assertNotNull(config.getManager());
	}
}
