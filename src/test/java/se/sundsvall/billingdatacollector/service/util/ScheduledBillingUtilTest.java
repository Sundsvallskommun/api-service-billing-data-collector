package se.sundsvall.billingdatacollector.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ScheduledBillingUtilTest {

	@Test
	void testCalculateNextScheduledBilling_currentMonthValidDay() {
		// Arrange
		var today = LocalDate.now();
		var daysOfMonth = Set.of(28);
		var months = Set.of(today.getMonthValue());

		// Calculate expected date
		LocalDate expected;
		if (today.getDayOfMonth() <= 28) {
			expected = LocalDate.of(today.getYear(), today.getMonthValue(), 28);
		} else {
			// Day 28 has passed, next occurrence is next year
			expected = LocalDate.of(today.getYear() + 1, today.getMonthValue(), 28);
		}

		// Act
		var result = ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testCalculateNextScheduledBilling_nextMonth() {
		// Arrange
		var today = LocalDate.now();
		var daysOfMonth = Set.of(1);
		var nextMonthDate = today.plusMonths(1);
		var months = Set.of(nextMonthDate.getMonthValue());

		var expected = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonthValue(), 1);

		// Act
		var result = ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testCalculateNextScheduledBilling_dayAdjustedForShortMonth() {
		// Arrange - February with day 31 should adjust to last day of February
		var today = LocalDate.now();
		var daysOfMonth = Set.of(31);
		var months = Set.of(2); // February

		// Calculate next February
		var nextFeb = today.getMonthValue() <= 2 && (today.getMonthValue() < 2 || today.getDayOfMonth() <= today.withMonth(2).lengthOfMonth())
			? LocalDate.of(today.getYear(), 2, 1)
			: LocalDate.of(today.getYear() + 1, 2, 1);
		var expected = LocalDate.of(nextFeb.getYear(), 2, nextFeb.lengthOfMonth());

		// Act
		var result = ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void testCalculateNextScheduledBilling_invalidDaysOfMonth_throwsException(Set<Integer> daysOfMonth) {
		// Arrange
		var months = Set.of(1, 2, 3);

		// Act & Assert
		assertThatThrownBy(() -> ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billingDaysOfMonth must not be empty");
	}

	@ParameterizedTest
	@NullAndEmptySource
	void testCalculateNextScheduledBilling_invalidMonths_throwsException(Set<Integer> months) {
		// Arrange
		var daysOfMonth = Set.of(1, 15);

		// Act & Assert
		assertThatThrownBy(() -> ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billingMonths must not be empty");
	}

	@Test
	void testCalculateNextScheduledBilling_withStartFrom_skipsEarlierDates() {
		// Arrange - billing configured for day 1 of every month
		var today = LocalDate.now();
		var startFrom = today.plusDays(1); // tomorrow
		var daysOfMonth = Set.of(today.getDayOfMonth()); // today's day
		var months = Set.of(today.getMonthValue()); // current month

		// Act - calculate from tomorrow, so today should be skipped
		var result = ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months, startFrom);

		// Assert - should return next year since today's date is skipped
		assertThat(result).isAfter(today);
		assertThat(result.getYear()).isEqualTo(today.getYear() + 1);
		assertThat(result.getMonthValue()).isEqualTo(today.getMonthValue());
		assertThat(result.getDayOfMonth()).isEqualTo(today.getDayOfMonth());
	}

	@Test
	void testCalculateNextScheduledBilling_withStartFrom_returnsSameDayIfMatches() {
		// Arrange
		var startFrom = LocalDate.of(2025, 6, 15);
		var daysOfMonth = Set.of(15);
		var months = Set.of(6);

		// Act - startFrom matches a valid billing date
		var result = ScheduledBillingUtil.calculateNextScheduledBilling(daysOfMonth, months, startFrom);

		// Assert - should return the same date since it's valid
		assertThat(result).isEqualTo(startFrom);
	}
}
