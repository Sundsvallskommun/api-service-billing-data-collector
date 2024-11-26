package se.sundsvall.billingdatacollector.integration.opene.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

class MapperHelperTest {

	private final MapperHelper mapper = new MapperHelper();

	@Test
	void testConvertStringToFloat_shouldThrowException_whenNotParseableToFloat() {
		final var unparseable = "not a float";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.convertStringToFloat(unparseable))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't convert 'not a float' to a float");
			});
	}

	@Test
	void testGetLeadingDigits_shouldThrowException_whenNoDigitsFound() {
		final var noDigits = "no leading digits";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getLeadingDigits(noDigits))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract leading digits from 'no leading digits'");
			});
	}

	@Test
	void testGetTrailingDigits_shouldThrowException_whenNoDigitsFound() {
		final var noDigits = "no trailing digits";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getTrailingDigitsFromString(noDigits))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract trailing digits from 'no trailing digits'");
			});
	}

	@Test
	void testGetInternalMotpartNumbers_shouldThrowException_whenCustomerIdIsNull() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getInternalMotpartNumbers(null))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract motpart numbers from 'null'");
			});
	}

	@Test
	void testGetExternalMotpartNumbers_shouildThrowException_whenCustomerIdIsNull() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getExternalMotpartNumbers(null))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract trailing digits from 'null'");
			});
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
	void testGetLeadingDigits_shouldReturnLeadingDigits() {
		assertThat(mapper.getLeadingDigits("123 - Something 456")).isEqualTo("123");
	}

	@Test
	void testConvertStringToOffsetDateTime() {
		assertThat(mapper.convertStringToOffsetDateTime("2024-09-20T15:28:23"))
			.isEqualTo(OffsetDateTime.of(2024, 9, 20, 15, 28, 23, 0, ZoneOffset.ofHours(2)));
	}

	@Test
	void testConvertNullStringToOffsetDateTime() {
		assertThat(mapper.convertStringToOffsetDateTime(null)).isNull();
	}

	@Test
	void testConvertFaultyDateStringToOffsetDateTime() {
		assertThat(mapper.convertStringToOffsetDateTime("not a date")).isNull();
	}
}
