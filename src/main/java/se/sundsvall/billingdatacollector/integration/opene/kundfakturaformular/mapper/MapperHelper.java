package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.OrganizationInformation;

final class MapperHelper {

	private static final String DIGITS_AND_DECIMAL_SEPARATORS_REGEX = "[^0-9.,]+";
	private static final String LEADING_DIGITS_REGEX = "^\\d+";
	private static final String ORGANIZATION_INFORMATION_REGEX = "(\\d+)\\s\\|\\s(.*)\\s\\|\\s(.*)\\s\\|\\s(.*)\\s\\|\\s(\\d{3}\\s\\d{2})\\s(.*)\\s\\|\\s(\\d+)";
	private static final String TRAILING_DIGITS_REGEX = "\\d+$";

	private static final Pattern LEADING_DIGITS_PATTERN = Pattern.compile(LEADING_DIGITS_REGEX);
	private static final Pattern ORGANIZATION_INFORMATION_PATTERN = Pattern.compile(ORGANIZATION_INFORMATION_REGEX);
	private static final Pattern TRAILING_DIGITS_PATTERN = Pattern.compile(TRAILING_DIGITS_REGEX);

	private static final String STRING_TO_FLOAT_ERROR = "Couldn't convert '%s' to a float";
	private static final String PARSE_ORGANIZATION_INFORMATION_ERROR = "Could not parse organization information";

	private MapperHelper() {
		// Not meant to be instantiated
	}

	/**
	 * Converts a string to a float.
	 * e.g. "123,45 SEK" will be converted to Float: 123.45
	 * 
	 * @param  stringToConvert The string to convert to a float
	 * @return                 The string converted to a float, or 0 if the string is null.
	 */
	static float convertStringToFloat(String stringToConvert) {
		return ofNullable(stringToConvert)
			.map(MapperHelper::removeCurrencyFromString)
			.map(MapperHelper::replaceCommasInCurrencyString)
			.map(string -> parseStringToFloat(string, stringToConvert))
			.orElse(0f);
	}

	static float parseStringToFloat(String stringToParse, String originalString) {
		try {
			return Float.parseFloat(stringToParse);
		} catch (NumberFormatException e) {
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
	static String replaceCommasInCurrencyString(String stringToParse) {
		return ofNullable(stringToParse)
			.map(string -> string.replace(",", "."))
			.orElse(null);
	}

	/**
	 * Removes all characters that are not numbers, commas or dots.
	 * 
	 * @param  stringToParse The string to parse
	 * @return               The string with all characters that are not numbers, commas or dots removed.
	 */
	static String removeCurrencyFromString(String stringToParse) {
		return ofNullable(stringToParse)
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
	static String getLeadingDigitsFromString(String stringToParse) {
		return ofNullable(stringToParse)
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
	static String getTrailingDigitsFromString(String stringToParse) {
		return ofNullable(stringToParse)
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
	static String getExternalMotpartNumbers(String motpart) {
		return ofNullable(motpart)
			.map(MapperHelper::getTrailingDigitsFromString)
			.map(numbers -> StringUtils.rightPad(numbers, 8, "0"))
			.orElse(null);
	}

	/**
	 * Extracts the internal motpart/counterpart numbers from a string and add a "1" in front of it
	 * 
	 * @param  customerId The string to extract motpart numbers from
	 * @return            The motpart numbers
	 */
	static String getInternalMotpartNumbers(String customerId) {
		return ofNullable(customerId)
			.map(id -> "1" + id)
			.orElse(null);
	}

	static OrganizationInformation getOrganizationInformation(String value) {
		var matcher = ORGANIZATION_INFORMATION_PATTERN.matcher(value);

		if (matcher.matches()) {
			return OrganizationInformation.builder()
				.withOrganizationNumber(ofNullable(matcher.group(1)).map(String::trim).orElse(null))
				.withName(ofNullable(matcher.group(2)).map(String::trim).orElse(null))
				.withStreetAddress(ofNullable(matcher.group(3)).map(String::trim).orElse(null))
				.withCareOf(ofNullable(matcher.group(4)).map(String::trim).orElse(null))
				.withZipCode(ofNullable(matcher.group(5)).map(String::trim).orElse(null))
				.withCity(ofNullable(matcher.group(6)).map(String::trim).orElse(null))
				.build();
		}

		throw Problem.builder()
			.withTitle(PARSE_ORGANIZATION_INFORMATION_ERROR)
			.withStatus(INTERNAL_SERVER_ERROR)
			.withDetail("Could not parse organization information from string: " + value)
			.build();
	}

	/**
	 * Limit the length of a string, if it's more than the maxLength it will be truncated.
	 * 
	 * @param  string    the string to potentially limit
	 * @param  maxLength the maximum length of the string
	 * @return           the string if it's less than maxLength, otherwise the string truncated to maxLength
	 */
	static String truncateString(String string, int maxLength) {
		if (isNotBlank(string) && string.length() >= maxLength) {
			return string.substring(0, maxLength);
		}
		return string;
	}
}
