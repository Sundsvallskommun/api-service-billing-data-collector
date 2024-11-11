package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zalando.problem.ThrowableProblem;

class MapperHelperTest {

	private final MapperHelper mapper = new MapperHelper();

	// Most common case, i.e. missing care of
	private static final String ORG_STRING_MISSING_CARE_OF = "5591628136 - Tennisbanan AB - Ankeborgsvägen 22 -   - 123 45 Ankeborg - 789";
	// Care of is present, plus some extra characters to make sure that we can handle special characters
	private static final String ORG_STRING = "5591628136 - Tennisbanan!@#$%^&*()-_=+\\|[]{};:/?.>< AB - Ankeborgsvägen 22 - Some Care of address - 123 45 Ankeborg - 789";

	@Test
	void testConvertStringToFloat_shouldThrowException_whenNotParseableToFloat() {
		var unparseable = "not a float";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.convertStringToFloat(unparseable))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't convert 'not a float' to a float");
			});
	}

	@Test
	void testGetLeadingDigits_shouldReturnNull_whenNoDigitsFound() {
		assertThat(mapper.getLeadingDigitsFromString("no 123 leading digits")).isNull();
	}

	@Test
	void testGetTrailingDigits_shouldReturnNull_whenNoDigitsFound() {
		assertThat(mapper.getTrailingDigitsFromString("no trailing 123 digits")).isNull();
	}

	@Test
	void testGetInternalMotpartNumbers_shouldReturnNull_whenCustomerIdIsNull() {
		assertThat(mapper.getInternalMotpartNumbers(null)).isNull();
	}

	@Test
	void testGetExternalMotpartNumbers() {
		assertThat(mapper.getExternalMotpartNumbers("123 - 456")).isEqualTo("45600000");
	}

	@Test
	void testGetExternalMotpartNumbers_shouldReturnNull_whenCustomerIdIsNull() {
		assertThat(mapper.getExternalMotpartNumbers(null)).isNull();
	}

	@Test
	void testCovertNullValueToFloat_shouldReturnZero() {
		assertThat(mapper.convertStringToFloat(null)).isZero();
	}

	@Test
	void testReplaceCommasInCurrencyString_shouldReplaceCommas() {
		assertThat(mapper.replaceCommasInCurrencyString("123,45")).isEqualTo("123.45");
	}

	@Test
	void testReplaceCommasInCurrencyString_shouldReturnNull_whenInputIsNull() {
		assertThat(mapper.replaceCommasInCurrencyString(null)).isNull();
	}

	@Test
	void testRemoveCurrencyFromString_shouldRemoveCharacters() {
		assertThat(mapper.removeCurrencyFromString("123,45 SEK")).isEqualTo("123,45");
		assertThat(mapper.removeCurrencyFromString("123.45 SEK")).isEqualTo("123.45");
	}

	@Test
	void testRemoveCurrencyFromString_shouldReturnNull_whenInputIsNull() {
		assertThat(mapper.removeCurrencyFromString(null)).isNull();
	}

	@Test
	void testGetLeadingDigitsFromString_shouldReturnLeadingDigits() {
		assertThat(mapper.getLeadingDigitsFromString("123 - Something 456")).isEqualTo("123");
	}

	@Test
	void testGetOrganizationInformation_withoutCareOf() {
		var organizationInformation = mapper.getOrganizationInformation(ORG_STRING_MISSING_CARE_OF);
		assertThat(organizationInformation.getOrganizationNumber()).isEqualTo("5591628136");
		assertThat(organizationInformation.getName()).isEqualTo("Tennisbanan AB");
		assertThat(organizationInformation.getStreetAddress()).isEqualTo("Ankeborgsvägen 22");
		assertThat(organizationInformation.getZipCode()).isEqualTo("123 45");
		assertThat(organizationInformation.getCity()).isEqualTo("Ankeborg");
		assertThat(organizationInformation.getCareOf()).isEmpty();
	}

	@Test
	void testGetOrganizationInformation_withCareOf() {
		var organizationInformation = mapper.getOrganizationInformation(ORG_STRING);
		assertThat(organizationInformation.getOrganizationNumber()).isEqualTo("5591628136");
		assertThat(organizationInformation.getName()).isEqualTo("Tennisbanan!@#$%^&*()-_=+\\|[]{};:/?.>< AB");
		assertThat(organizationInformation.getStreetAddress()).isEqualTo("Ankeborgsvägen 22");
		assertThat(organizationInformation.getZipCode()).isEqualTo("123 45");
		assertThat(organizationInformation.getCity()).isEqualTo("Ankeborg");
		assertThat(organizationInformation.getCareOf()).isEqualTo("Some Care of address");
	}

	@Test
	void testGetOrganizationInformation_cannotMatch_shouldThrowException() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getOrganizationInformation("not matching"))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Could not parse organization information");
				assertThat(throwableProblem.getDetail()).isEqualTo("Could not parse organization information from string: not matching");
			});
	}

	@MethodSource("provideStringsForTruncation")
	@ParameterizedTest
	void testTruncateString(String input, String wanted, int maxLength) {
		assertThat(mapper.truncateString(input, maxLength)).isEqualTo(wanted);
	}

	private static Stream<Arguments> provideStringsForTruncation() {
		return Stream.of(
			// (input, wanted, maxLength)
			Arguments.of(null, null, 5),
			Arguments.of("", "", 5),
			Arguments.of(" ", " ", 5),
			Arguments.of("abcd", "abcd", 5),
			Arguments.of("abcde", "abcde", 5),
			Arguments.of("abcdef", "abcde", 5));
	}
}
