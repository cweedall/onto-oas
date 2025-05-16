package edu.isi.oba.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class StringUtilsTest {
	/**
	 * This test will try to load a file that does not exits. The exception is captured and reported.
	 * This test will pass IF you see an error on the output terminal
	 *
	 * @throws OWLOntologyCreationException
	 */
	@Test
	public void testKebabCaseConversion() {
		// Test #1
		var expectedStr = "this-is-a-string";
		var originalStr = "thisIsAString";
		var convertedStr = StringUtils.pascalCaseToKebabCase(originalStr);

		Assertions.assertEquals(expectedStr, convertedStr);

		// Test #2
		expectedStr = "this-happy-string";
		originalStr = "thisHAPPYString";
		convertedStr = StringUtils.pascalCaseToKebabCase(originalStr);

		Assertions.assertEquals(expectedStr, convertedStr);

		// Test #3
		expectedStr = "this-phd-string";
		originalStr = "thisPhDString";
		convertedStr = StringUtils.pascalCaseToKebabCase(originalStr);

		Assertions.assertEquals(expectedStr, convertedStr);
	}
}
