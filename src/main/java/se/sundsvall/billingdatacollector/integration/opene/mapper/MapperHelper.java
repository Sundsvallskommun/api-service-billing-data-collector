package se.sundsvall.billingdatacollector.integration.opene.mapper;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

@Component
public class MapperHelper {

	private static final Pattern LEADING_DIGITS_PATTERN = Pattern.compile("^\\d+");
	private static final Pattern TRAILING_DIGITS_PATTERN = Pattern.compile("\\d+(?=\\D*$)");
	private static final String DIGITS_AND_DECIMAL_SEPARATORS_REGEX = "[^0-9.,]+";
	private static final String STRING_TO_FLOAT_ERROR = "Couldn't convert '%s' to a float";

	/**
	 * Converts a string to a float.
	 * e.g. "123,45 SEK" will be converted to Float: 123.45
	 * @param stringToConvert The string to convert
	 * @return The string converted to a float, or 0 if the string is null.
	 */
	public float convertStringToFloat(String stringToConvert) {
		return Optional.ofNullable(stringToConvert)
			.map(this::removeCurrencyFromString)
			.map(this::replaceCommasInCurrencyString)
			.map(string -> parseStringToFloat(string, stringToConvert))
			.orElse(0f);
	}

	float parseStringToFloat(String stringToParse, String originalString) {
		try {
			return Float.parseFloat(stringToParse);
			//Catch a NumberFormatException here to be able to throw a Problem
		} catch (final NumberFormatException e) {
			throw Problem.builder()
				.withTitle(String.format(STRING_TO_FLOAT_ERROR, originalString))
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	/**
	 * Replaces commas with dots in a currency string to be able to parse it to a Float
	 *
	 * @param  stringToParse The string to parse
	 * @return               The string with commas replaced with dots (if any).
	 */
	String replaceCommasInCurrencyString(String stringToParse) {
		return Optional.ofNullable(stringToParse)
			.map(string -> string.replace(",", "."))
			.orElse(null);
	}

	/**
	 * Removes all characters that are not numbers, commas or dots.
	 *
	 * @param  stringToParse The string to parse
	 * @return               The string with all characters that are not numbers, commas or dots removed.
	 */
	String removeCurrencyFromString(String stringToParse) {
		return Optional.ofNullable(stringToParse)
			.map(string -> string.replaceAll(DIGITS_AND_DECIMAL_SEPARATORS_REGEX, ""))
			.orElse(null);
	}

	/**
	 * Extracts leading digits from a string.
	 * e.g. "123 - Something 456" will return "123"
	 *
	 * @param  stringToParse The string to parse
	 * @return               Any leading numbers from the string
	 */
	public String getLeadingDigitsFromString(String stringToParse) {
		return Optional.ofNullable(stringToParse)
			.map(LEADING_DIGITS_PATTERN::matcher)
			.filter(Matcher::find)
			.map(Matcher::group)
			.orElse(null);
	}

	/**
	 * Extracts only trailing digits from a string.
	 * e.g. "123 - Something 456" will return "456"
	 *
	 * @param  stringToParse The string to parse
	 * @return               Any trailing numbers from the string
	 */
	String getTrailingDigitsFromString(String stringToParse) {
		return Optional.ofNullable(stringToParse)
			.map(TRAILING_DIGITS_PATTERN::matcher)
			.filter(Matcher::find)
			.map(Matcher::group)
			.orElse(null);
	}

	/**
	 * Extracts the motpart/counterpart numbers from a string and fill with 0's up to 8 characters
	 *
	 * @param  motpart The string to extract motpart numbers from
	 * @return         The motpart numbers
	 */
	public String getExternalMotpartNumbers(String motpart) {
		return Optional.ofNullable(motpart)
			.map(this::getTrailingDigitsFromString)
			.map(numbers -> StringUtils.rightPad(numbers, 8, "0"))
			.orElse(null);
	}

	/**
	 * Extracts the internal motpart/counterpart numbers from a string and add a "1" in front of it
	 *
	 * @param  customerId The string to extract motpart numbers from
	 * @return            The motpart numbers
	 */
	public String getInternalMotpartNumbers(String customerId) {
		return Optional.ofNullable(customerId)
			.map(id -> "1" + id)
			.orElse(null);
	}
}
