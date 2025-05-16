package edu.isi.oba.utils;

import java.util.regex.Pattern;
import rita.RiTa;

public class StringUtils {
	/**
	 * Convert a PascalCase (or camelCase) string to kebab-case.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String pascalCaseToKebabCase(String str) {
		return str.replaceAll("\\B([A-Z])(?=[a-z])", "-$1")
				.replaceAll("\\B([a-z0-9])([A-Z])", "$1-$2")
				.replaceAll(
						"Ph-D-",
						"PhD-") // Annoying workaround for "PhD" which usually occurs together as one "word"
				.toLowerCase();
	}

	/**
	 * Convert a kebab-case string to PascalCase.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String kebabCaseToCamelCase(String str) {
		return Pattern.compile("-(.)").matcher(str).replaceAll(mr -> mr.group(1).toUpperCase());
	}

	/**
	 * Convert a kebab-case string to PascalCase.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String kebabCaseToPascalCase(String str) {
		final var camelCaseStr = StringUtils.kebabCaseToCamelCase(str);
		return camelCaseStr.substring(0, 1).toUpperCase() + camelCaseStr.substring(1);
	}

	public static String getPluralOf(String str) {
		// Pluralizing currently only works for English.  Non-English words will be treated as though
		// they are English.
		// TODO: Java support for singularization/pluralization and locale/international support supoort
		// for the process does not have many good options that we could find so far.
		// TODO: If such an option exists or becomes available, this should be updated to support
		// pluralization in other languages.
		// TODO: The language/locale would need to be set as a configuration value and passed into this
		// class somehow.

		return RiTa.pluralize(str);
	}

	public static String getLowerCasePluralOf(String str) {
		return StringUtils.getPluralOf(str.toLowerCase());
	}

	public static String getSingularOf(String str) {
		// Pluralizing currently only works for English.  Non-English words will be treated as though
		// they are English.
		// TODO: Java support for singularization/pluralization and locale/international support supoort
		// for the process does not have many good options that we could find so far.
		// TODO: If such an option exists or becomes available, this should be updated to support
		// pluralization in other languages.
		// TODO: The language/locale would need to be set as a configuration value and passed into this
		// class somehow.

		return RiTa.singularize(str);
	}

	public static String getLowerCaseSingularOf(String str) {
		return StringUtils.getSingularOf(str.toLowerCase());
	}

	public static String insertCharBetweenLowerAndUpper(String text, char charToInsert) {
		if (text == null || text.isEmpty()) {
			return text;
		}

		StringBuilder result = new StringBuilder();
		result.append(text.charAt(0));

		for (int i = 1; i < text.length(); i++) {
			char currentChar = text.charAt(i);
			char previousChar = text.charAt(i - 1);

			if (Character.isLowerCase(previousChar) && Character.isUpperCase(currentChar)) {
				result.append(charToInsert);
			}
			result.append(currentChar);
		}

		return result.toString();
	}
}
