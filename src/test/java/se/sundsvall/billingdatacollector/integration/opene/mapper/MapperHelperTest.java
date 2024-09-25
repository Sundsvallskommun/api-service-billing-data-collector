package se.sundsvall.billingdatacollector.integration.opene.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

class MapperHelperTest {

	private final MapperHelper mapper = new MapperHelper();

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
		assertThat(mapper.getLeadingDigitsFromString("no leading digits")).isNull();
	}

	@Test
	void testGetTrailingDigits_shouldReturnNull_whenNoDigitsFound() {
		assertThat(mapper.getTrailingDigitsFromString("no trailing digits")).isNull();
	}

	@Test
	void testGetInternalMotpartNumbers_shouldReturnNull_whenCustomerIdIsNull() {
		assertThat(mapper.getInternalMotpartNumbers(null)).isNull();
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
}
