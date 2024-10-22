package se.sundsvall.billingdatacollector.integration.opene.mapper;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import se.sundsvall.billingdatacollector.integration.opene.mapper.model.OrganizationInformation;

@Component
public class MapperHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(MapperHelper.class);

	private static final Pattern LEADING_DIGITS_PATTERN = Pattern.compile("^\\d+");
	private static final Pattern TRAILING_DIGITS_PATTERN = Pattern.compile("\\d+(?=\\D*$)");

	private static final String ORGANIZATION_INFORMATION_REGEX = "(\\d+)\\s-\\s([a-zA-ZåäöÅÄÖ\\s]+)\\s-\\s(.*)\\s-\\s(.*)\\s-\\s(\\d{3}\\s\\d{2})([a-zA-ZåäöÅÄÖ\\s]+)\\s-\\s(\\d+)";
	private static final Pattern ORGANIZATION_INFORMATION_PATTERN = Pattern.compile(ORGANIZATION_INFORMATION_REGEX);

	private static final String DIGITS_AND_DECIMAL_SEPARATORS_REGEX = "[^0-9.,]+";
	private static final String STRING_TO_FLOAT_ERROR = "Couldn't convert '%s' to a float";

	/**
	 * Converts a string to a float.
	 * e.g. "123,45 SEK" will be converted to Float: 123.45
	 * @param stringToConvert The string to convert
	 * @return The string converted to a float, or 0 if the string is null.
	 */
	public float convertStringToFloat(String stringToConvert) {
		return ofNullable(stringToConvert)
			.map(this::removeCurrencyFromString)
			.map(this::replaceCommasInCurrencyString)
			.map(string -> parseStringToFloat(string, stringToConvert))
			.orElse(0f);
	}

	float parseStringToFloat(String stringToParse, String originalString) {
		try {
			return Float.parseFloat(stringToParse);
			//Catch a NumberFormatException here to be able to throw a Problem
		} catch (NumberFormatException e) {
			throw Problem.builder()
				.withTitle(String.format(STRING_TO_FLOAT_ERROR, originalString))
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	/**
	 * Replaces commas with dots in a currency string to be able to parse it to a Float
	 * @param stringToParse The string to parse
	 * @return The string with commas replaced with dots (if any).
	 */
	String replaceCommasInCurrencyString(String stringToParse) {
		return ofNullable(stringToParse)
			.map(string -> string.replace(",", "."))
			.orElse(null);
	}

	/**
	 * Removes all characters that are not numbers, commas or dots.
	 * @param stringToParse The string to parse
	 * @return The string with all characters that are not numbers, commas or dots removed.
	 */
	String removeCurrencyFromString(String stringToParse) {
		return ofNullable(stringToParse)
			.map(string -> string.replaceAll(DIGITS_AND_DECIMAL_SEPARATORS_REGEX, ""))
			.orElse(null);
	}

	/**
	 * Extracts leading digits from a string.
	 * e.g. "123 - Something 456" will return "123"
	 * @param stringToParse The string to parse
	 * @return Any leading numbers from the string
	 */
	public String getLeadingDigitsFromString(String stringToParse) {
		return ofNullable(stringToParse)
			.map(LEADING_DIGITS_PATTERN::matcher)
			.filter(Matcher::find)
			.map(Matcher::group)
			.orElse(null);
	}

	/**
	 * Extracts only trailing digits from a string.
	 * e.g. "123 - Something 456" will return "456"
	 * @param stringToParse The string to parse
	 * @return Any trailing numbers from the string
	 */
	String getTrailingDigitsFromString(String stringToParse) {
		return ofNullable(stringToParse)
			.map(TRAILING_DIGITS_PATTERN::matcher)
			.filter(Matcher::find)
			.map(Matcher::group)
			.orElse(null);
	}

	/**
	 * Extracts the motpart/counterpart numbers from a string and fill with 0's up to 8 characters
	 * @param motpart The string to extract motpart numbers from
	 * @return The motpart numbers
	 */
	public String getExternalMotpartNumbers(String motpart) {
		return ofNullable(motpart)
			.map(this::getTrailingDigitsFromString)
			.map(numbers -> StringUtils.rightPad(numbers, 8, "0"))
			.orElse(null);
	}

	/**
	 * Extracts the internal motpart/counterpart numbers from a string and add a "1" in front of it
	 * @param customerId The string to extract motpart numbers from
	 * @return The motpart numbers
	 */
	public String getInternalMotpartNumbers(String customerId) {
		return ofNullable(customerId)
			.map(id -> "1" + id)
			.orElse(null);
	}

	public OrganizationInformation getOrganizationInformation(String value) {
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
		} else {
			LOGGER.error("Could not parse organization information: {}", value);
			throw Problem.builder()
				.withTitle("Could not parse organization information")
				.withStatus(INTERNAL_SERVER_ERROR)
				.withDetail("Could not parse organization information from string: " + value)
				.build();
		}
	}
}
