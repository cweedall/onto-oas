package edu.isi.oba.utils.cli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CliUtilsTest {

	@Test
	public void testGetConfigYaml() {
		String[] args = {"-c", "config.yaml"};
		String result = CliUtils.getYamlConfigFileArgument(args);
		assertEquals("config.yaml", result);
	}
}
