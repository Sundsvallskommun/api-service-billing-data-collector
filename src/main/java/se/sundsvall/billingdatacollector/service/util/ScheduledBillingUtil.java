package se.sundsvall.billingdatacollector.service.util;

import java.time.LocalDate;
import java.util.Set;

public final class ScheduledBillingUtil {

	private ScheduledBillingUtil() {}

	public static LocalDate calculateNextScheduledBilling(Set<Integer> billingDaysOfMonth, Set<Integer> billingMonths) {
		return calculateNextScheduledBilling(billingDaysOfMonth, billingMonths, LocalDate.now());
	}

	public static LocalDate calculateNextScheduledBilling(Set<Integer> billingDaysOfMonth, Set<Integer> billingMonths, LocalDate startFrom) {
		if (billingDaysOfMonth == null || billingDaysOfMonth.isEmpty()) {
			throw new IllegalArgumentException("billingDaysOfMonth must not be empty");
		}
		if (billingMonths == null || billingMonths.isEmpty()) {
			throw new IllegalArgumentException("billingMonths must not be empty");
		}

		for (int monthOffset = 0; monthOffset <= 12; monthOffset++) {
			LocalDate checkMonth = startFrom.plusMonths(monthOffset);
			int month = checkMonth.getMonthValue();

			if (billingMonths.contains(month)) {
				for (Integer day : billingDaysOfMonth.stream().sorted().toList()) {
					int actualDay = Math.min(day, checkMonth.lengthOfMonth());
					LocalDate potentialDate = LocalDate.of(checkMonth.getYear(), month, actualDay);

					if (!potentialDate.isBefore(startFrom)) {
						return potentialDate;
					}
				}
			}
		}

		// This should never be reached since we check 13 months (covers all 12 calendar months)
		throw new IllegalStateException("Unable to calculate next scheduled billing date");
	}
}
