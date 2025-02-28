package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.ExternFaktura;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.OrganizationInformation;

final class MapperHelper {

	private static final String DIGITS_AND_DECIMAL_SEPARATORS_REGEX = "[^0-9.,]+";
	private static final String LEADING_DIGITS_REGEX = "^\\d+";
	private static final String ORGANIZATION_INFORMATION_PIPE_REGEX = "(\\d+)\\s\\|\\s(.*)\\s\\|\\s(.*)\\s\\|\\s(.*)\\s\\|\\s(\\d{3}\\s\\d{2})\\s(.*)\\s\\|\\s(\\d+)";
	private static final String ORGANIZATION_INFORMATION_DASH_REGEX = "(\\d+)\\s-\\s(.*)\\s-\\s(.*)\\s-\\s(.*)\\s-\\s(\\d{3}\\s\\d{2})(.*)\\s-\\s(\\d+)";
	private static final String TRAILING_DIGITS_REGEX = "\\d+$";

	private static final Pattern LEADING_DIGITS_PATTERN = Pattern.compile(LEADING_DIGITS_REGEX);
	private static final Pattern ORGANIZATION_INFORMATION_PIPE_PATTERN = Pattern.compile(ORGANIZATION_INFORMATION_PIPE_REGEX);
	private static final Pattern ORGANIZATION_INFORMATION_DASH_PATTERN = Pattern.compile(ORGANIZATION_INFORMATION_DASH_REGEX);
	private static final Pattern TRAILING_DIGITS_PATTERN = Pattern.compile(TRAILING_DIGITS_REGEX);

	private static final String STRING_TO_FLOAT_ERROR = "Couldn't convert '%s' to a float";
	private static final String PARSE_ORGANIZATION_INFORMATION_ERROR = "Could not parse organization information";

	private MapperHelper() {
		// Not meant to be instantiated
	}

	/**
	 * Converts a string to a BigDecimal.
	 * 
	 * @param  stringToConvert The string to convert to a BigDecimal
	 * @return                 The string converted to a BigDecimal, or 0 if the string is blank.
	 */
	static BigDecimal convertStringToBigDecimal(String stringToConvert) {
		return Optional.ofNullable(stringToConvert)
			.filter(StringUtils::isNotBlank)
			.map(MapperHelper::removeCurrencyFromString)
			.map(MapperHelper::replaceCommasInCurrencyString)
			.map(BigDecimal::new)
			.orElse(BigDecimal.ZERO);
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
	 * Extracts the customer id from a counterpart string.
	 * 
	 * @param  counterPart The counterpart string to extract the customer id from, e.g. "Motpart privatperson 860
	 *                     from which we'll extract the last digits.
	 * @return             The customer id, e.g. 860
	 */
	static String getCustomerIdFromCounterPart(String counterPart) {
		return ofNullable(counterPart)
			.map(MapperHelper::getTrailingDigitsFromString)
			.orElse(null);
	}

	/**
	 * Extracts the counterpart (motpart) numbers from a string and fill with 0's up to 8 characters
	 * 
	 * @param  counterPart The string to extract counterpart numbers from
	 * @return             The counterpart numbers
	 */
	static String getExternalCounterPartNumbers(String counterPart) {
		return ofNullable(counterPart)
			.map(MapperHelper::getTrailingDigitsFromString)
			.map(numbers -> StringUtils.rightPad(numbers, 8, "0"))
			.orElse(null);
	}

	/**
	 * Extracts the internal counterpart (motpart) numbers from a string and add a "1" in front of it
	 * 
	 * @param  customerId The string to extract counterpart numbers from
	 * @return            The counterpart numbers
	 */
	static String getInternalCounterPartNumbers(String customerId) {
		return ofNullable(customerId)
			.map(id -> "1" + id)
			.orElse(null);
	}

	/**
	 * Extracts organization information. If it has been generated from the organization number the
	 * "organizationInformation" field will be present.
	 * If it has been entered manually the "organizationInformation" field will be empty and we will have to look at
	 * KundensOrgUppgExterntForetag to extract all information.
	 *
	 * @param  externFaktura Pbject to extract organization information from
	 * @return               The organization information
	 */
	static OrganizationInformation getOrganizationInformation(ExternFaktura externFaktura) {
		// Check if information has been entered manually or "automatically".
		if (StringUtils.isNotBlank(externFaktura.manualOrgInfoOrganizationNumber())) {
			return getOrganizationInformationFromManualEntry(externFaktura);
		}

		return getOrganizationInformationFromAutomaticEntry(externFaktura);
	}

	/**
	 * There exists two variants for parsing. One legacy that uses dashes to separate the fields and one new that uses
	 * pipes.
	 * If it's an automatic entry the organization information will be in the "organizationInformation" field.
	 * If the organization information has been added manually, the information will be in the "manualOrgInfo*" fields.
	 * Should none of the regexes match, an exception will be thrown.
	 * 
	 * @param  externFaktura The external invoice to parse from
	 * @return               The organization information
	 */
	private static OrganizationInformation getOrganizationInformationFromAutomaticEntry(ExternFaktura externFaktura) {
		var pipeMatcher = ORGANIZATION_INFORMATION_PIPE_PATTERN.matcher(externFaktura.organizationInformation());
		var dashMatcher = ORGANIZATION_INFORMATION_DASH_PATTERN.matcher(externFaktura.organizationInformation());

		if (pipeMatcher.matches()) {
			return extractOrganizationInformationFromMatcher(pipeMatcher);
		} else if (dashMatcher.matches()) {
			return extractOrganizationInformationFromMatcher(dashMatcher);
		}

		throw Problem.builder()
			.withTitle(PARSE_ORGANIZATION_INFORMATION_ERROR)
			.withStatus(INTERNAL_SERVER_ERROR)
			.withDetail("Could not parse organization information from string: " + externFaktura.organizationInformation())
			.build();
	}

	private static OrganizationInformation getOrganizationInformationFromManualEntry(ExternFaktura externFaktura) {
		return OrganizationInformation.builder()
			.withOrganizationNumber(cleanOrganizationNumber(externFaktura.manualOrgInfoOrganizationNumber()))
			.withName(ofNullable(externFaktura.manualOrgInfoName()).map(String::trim).orElse(null))
			.withStreetAddress(ofNullable(externFaktura.manualOrgInfoAddress()).map(String::trim).orElse(null))
			.withCareOf(ofNullable(externFaktura.manualOrgInfoCo()).map(String::trim).orElse(null))
			.withZipCode(ofNullable(externFaktura.manualOrgInfoZipCode()).map(String::trim).orElse(null))
			.withCity(ofNullable(externFaktura.manualOrgInfoCity()).map(String::trim).orElse(null))
			.withCounterPart(ofNullable(getLeadingDigitsFromString(externFaktura.manualOrgInfoCounterPart())).map(String::trim).orElse(null))  // Set counterPart here
			.build();
	}

	private static OrganizationInformation extractOrganizationInformationFromMatcher(Matcher matcher) {
		return OrganizationInformation.builder()
			.withOrganizationNumber(ofNullable(matcher.group(1)).map(String::trim).orElse(null))
			.withName(ofNullable(matcher.group(2)).map(String::trim).orElse(null))
			.withStreetAddress(ofNullable(matcher.group(3)).map(String::trim).orElse(null))
			.withCareOf(ofNullable(matcher.group(4)).map(String::trim).orElse(null))
			.withZipCode(ofNullable(matcher.group(5)).map(String::trim).orElse(null))
			.withCity(ofNullable(matcher.group(6)).map(String::trim).orElse(null))
			.withCounterPart(ofNullable(matcher.group(7)).map(String::trim).orElse(null))
			.build();
	}

	private static String cleanOrganizationNumber(String organizationNumber) {
		return ofNullable(organizationNumber)
			.map(number -> number.replace("-", "")) // Remove dashes in case of "123456-1234"
			.map(number -> number.replace(" ", "")) // Remove spaces in case of "123456 1234"
			.orElse(organizationNumber);
	}

	/**
	 * Limit the length of a string, if it's more than the maxLength it will be truncated.
	 * 
	 * @param  string    the string to potentially limit
	 * @param  maxLength the maximum length of the string
	 * @return           the string if it's less than maxLength, otherwise the string truncated to maxLength
	 */
	static String truncateString(String string, int maxLength) {
		if (isNotBlank(string) && string.length() > maxLength) {
			return string.substring(0, maxLength).trim();
		}
		return string;
	}
}
