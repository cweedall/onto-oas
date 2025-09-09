package edu.isi.oba.utils;

import static org.junit.jupiter.api.Assertions.*;

import edu.isi.oba.BaseTest;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class StringUtilsTest extends BaseTest {

	@Test
	void testPrivateConstructor() throws Exception {
		Constructor<StringUtils> constructor = StringUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		assertThrows(InvocationTargetException.class, () -> constructor.newInstance());
	}

	@ParameterizedTest
	@CsvSource({
		"'PascalCaseExample', 'pascal-case-example'",
		"'PhDExample', 'phd-example'",
		"'', ''",
		","
	})
	@DisplayName("Test pascalCaseToKebabCase with various inputs")
	void testPascalCaseToKebabCase(String input, String expected) {
		assertEquals(expected, StringUtils.pascalCaseToKebabCase(input));
	}

	@ParameterizedTest
	@CsvSource({"'kebab-case-example', 'kebabCaseExample'", "'', ''", ","})
	@DisplayName("Test kebabCaseToCamelCase with various inputs")
	void testKebabCaseToCamelCase(String input, String expected) {
		assertEquals(expected, StringUtils.kebabCaseToCamelCase(input));
	}

	@ParameterizedTest
	@CsvSource({"'kebab-case-example', 'KebabCaseExample'", "'', ''", ","})
	@DisplayName("Test kebabCaseToPascalCase with various inputs")
	void testKebabCaseToPascalCase(String input, String expected) {
		assertEquals(expected, StringUtils.kebabCaseToPascalCase(input));
	}

	@ParameterizedTest
	@CsvSource({"'cat', 'cats'", "'bus', 'buses'", "'', ''", ","})
	@DisplayName("Test getPluralOf with various inputs")
	void testGetPluralOf(String input, String expected) {
		assertEquals(expected, StringUtils.getPluralOf(input));
	}

	@ParameterizedTest
	@CsvSource({"'Cat', 'cats'", "'Bus', 'buses'", "'', ''", ","})
	@DisplayName("Test getLowerCasePluralOf with various inputs")
	void testGetLowerCasePluralOf(String input, String expected) {
		assertEquals(expected, StringUtils.getLowerCasePluralOf(input));
	}

	@ParameterizedTest
	@CsvSource({"'cats', 'cat'", "'buses', 'bus'", "'', ''", ","})
	@DisplayName("Test getSingularOf with various inputs")
	void testGetSingularOf(String input, String expected) {
		assertEquals(expected, StringUtils.getSingularOf(input));
	}

	@ParameterizedTest
	@CsvSource({"'Cats', 'cat'", "'Buses', 'bus'", "'', ''", ","})
	@DisplayName("Test getLowerCaseSingularOf with various inputs")
	void testGetLowerCaseSingularOf(String input, String expected) {
		assertEquals(expected, StringUtils.getLowerCaseSingularOf(input));
	}

	@ParameterizedTest
	@CsvSource({
		"'camelCaseText', '-', 'camel-Case-Text'",
		"'PascalCaseText', '_', 'Pascal_Case_Text'",
		"'', '-', ''",
		",'-',"
	})
	@DisplayName("Test insertCharBetweenLowerAndUpper with various inputs")
	void testInsertCharBetweenLowerAndUpper(String input, char insertChar, String expected) {
		assertEquals(expected, StringUtils.insertCharBetweenLowerAndUpper(input, insertChar));
	}
}
