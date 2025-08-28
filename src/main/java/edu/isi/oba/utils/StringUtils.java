package edu.isi.oba.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import rita.RiTa;

public class StringUtils {
	private static final String UPPERCASE_BEFORE_LOWERCASE_BOUNDARY = "\\B([A-Z])(?=[a-z])";
	private static final String PASCAL_TO_KEBAB_UPPERCASE_BEFORE_LOWERCASE_REPLACEMENT = "-$1";
	private static final String PASCAL_WORD_BOUNDARY = "\\B([a-z0-9])([A-Z])";
	private static final String PASCAL_TO_KEBAB_WORD_BOUNDARY_REPLACEMENT = "$1-$2";
	private static final String CAMEL_CASE_BOUNDARY = "-(.)";
	private static final Map<String, String> PASCAL_TO_KEBAB_REGEX_TO_REPLACEMENT =
			new LinkedHashMap<>() {
				{
					// Order of put() calls is important, which is why using LinkedHashMap!
					put(
							UPPERCASE_BEFORE_LOWERCASE_BOUNDARY,
							PASCAL_TO_KEBAB_UPPERCASE_BEFORE_LOWERCASE_REPLACEMENT);
					put(PASCAL_WORD_BOUNDARY, PASCAL_TO_KEBAB_WORD_BOUNDARY_REPLACEMENT);
					put("Ph-D-", "PhD-");
				}
			};

	/**
	 * Convert a PascalCase (or camelCase) string to kebab-case.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String pascalCaseToKebabCase(String str) {
		if (str == null || str.isBlank()) {
			return str;
		}

		for (final var entry : PASCAL_TO_KEBAB_REGEX_TO_REPLACEMENT.entrySet()) {
			final var regex = entry.getKey();
			final var replacement = entry.getValue();
			str = str.replaceAll(regex, replacement);
		}

		return str.toLowerCase();
	}

	/**
	 * Convert a kebab-case string to PascalCase.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String kebabCaseToCamelCase(String str) {
		if (str == null || str.isBlank()) {
			return str;
		}

		return Pattern.compile(CAMEL_CASE_BOUNDARY)
				.matcher(str)
				.replaceAll(mr -> mr.group(1).toUpperCase());
	}

	/**
	 * Convert a kebab-case string to PascalCase.
	 *
	 * @param str a {@link String} which should be formatted in CamelCase.
	 * @return a {@link String} of the original string formatted in kebab-case.
	 */
	public static String kebabCaseToPascalCase(String str) {
		if (str == null || str.isBlank()) {
			return str;
		}

		final var camelCaseStr = StringUtils.kebabCaseToCamelCase(str);
		return camelCaseStr.substring(0, 1).toUpperCase() + camelCaseStr.substring(1);
	}

	public static String getPluralOf(String str) {
		if (str == null || str.isBlank()) {
			return str;
		}

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
		if (str == null || str.isBlank()) {
			return str;
		}

		return StringUtils.getPluralOf(str.toLowerCase());
	}

	public static String getSingularOf(String str) {
		if (str == null || str.isBlank()) {
			return str;
		}

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
		if (str == null || str.isBlank()) {
			return str;
		}

		return StringUtils.getSingularOf(str.toLowerCase());
	}

	public static String insertCharBetweenLowerAndUpper(String text, char charToInsert) {
		if (text == null || text.isBlank()) {
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
