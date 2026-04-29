package se.sundsvall.billingdatacollector.service.util;

import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPeriodCalculatorTest {

	@ParameterizedTest(name = "{0} {1} on {2} → {3} – {4}")
	@MethodSource("periodCases")
	void computePeriod(IntervalType interval, InvoicedIn invoicedIn, LocalDate scheduledDate,
		LocalDate expectedStart, LocalDate expectedEnd) {

		var period = BillingPeriodCalculator.computePeriod(scheduledDate, interval, invoicedIn);

		assertThat(period.startDate()).isEqualTo(expectedStart);
		assertThat(period.endDate()).isEqualTo(expectedEnd);
	}

	static Stream<Arguments> periodCases() {
		return Stream.of(
			// YEARLY — December slot covers full calendar year
			Arguments.of(IntervalType.YEARLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)),
			Arguments.of(IntervalType.YEARLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
			// YEARLY — June slot (LAND_LEASE_RESIDENTIAL with period ending 30 Jun)
			Arguments.of(IntervalType.YEARLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30)),
			Arguments.of(IntervalType.YEARLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 6, 1), LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30)),

			// QUARTERLY — Mar slot
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)),
			// QUARTERLY — Jun slot
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)),
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)),
			// QUARTERLY — Sep slot
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)),
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)),
			// QUARTERLY — Dec slot (ADVANCE rolls to next year's Q1)
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 31)),
			Arguments.of(IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 12, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)),

			// HALF_YEARLY — Jun slot
			Arguments.of(IntervalType.HALF_YEARLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)),
			Arguments.of(IntervalType.HALF_YEARLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)),
			// HALF_YEARLY — Dec slot (ADVANCE rolls to next year's H1)
			Arguments.of(IntervalType.HALF_YEARLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30)),
			Arguments.of(IntervalType.HALF_YEARLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 12, 1), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)),

			// MONTHLY — ADVANCE covers next month, ARREARS the same month
			Arguments.of(IntervalType.MONTHLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
			Arguments.of(IntervalType.MONTHLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
			// MONTHLY — December ADVANCE rolls into next year
			Arguments.of(IntervalType.MONTHLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)));
	}
}
