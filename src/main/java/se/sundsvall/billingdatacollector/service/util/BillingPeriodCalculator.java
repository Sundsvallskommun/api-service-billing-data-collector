package se.sundsvall.billingdatacollector.service.util;

import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import java.time.LocalDate;
import java.time.Month;

/**
 * Computes the start and end date of the period an invoice covers, given the
 * scheduled billing date, the invoicing interval (yearly / half-yearly /
 * quarterly / monthly) and the direction of invoicing (advance vs. arrears).
 *
 * <p>
 * Used both to (a) decide whether a contract's end date falls inside the
 * period being billed (so the scheduler can stop emitting billings in time)
 * and (b) format the human-readable period description on the invoice row.
 *
 * <p>
 * Period semantics (inclusive boundaries):
 * <ul>
 * <li><b>YEARLY ADVANCE</b> on a December slot covers Jan&ndash;Dec of the
 * following calendar year. On a June slot (the LAND_LEASE_RESIDENTIAL
 * sub-case where the contract's current period ends 30 June) it covers
 * July of the same year through June of the following year.</li>
 * <li><b>YEARLY ARREARS</b> on a December slot covers Jan&ndash;Dec of the
 * same year. On a June slot it covers July of the previous year through
 * June of the same year.</li>
 * <li><b>QUARTERLY ADVANCE</b> bills for the next quarter; <b>ARREARS</b>
 * bills for the same quarter the slot lies in.</li>
 * <li><b>HALF_YEARLY</b> follows the same advance/arrears shift across H1
 * and H2.</li>
 * <li><b>MONTHLY ADVANCE</b> covers the next month; <b>ARREARS</b> covers
 * the same month the slot lies in.</li>
 * </ul>
 */
public final class BillingPeriodCalculator {

	private BillingPeriodCalculator() { /* utility */ }

	/** Inclusive period boundaries. */
	public record BillingPeriod(LocalDate startDate, LocalDate endDate) {}

	/**
	 * @param  scheduledDate the date the billing is scheduled to fire
	 *                       (i.e. {@code ScheduledBillingEntity.nextScheduledBilling})
	 * @param  interval      yearly, half-yearly, quarterly or monthly
	 * @param  invoicedIn    advance (period after the slot) or arrears (period
	 *                       around / before the slot)
	 * @return               the inclusive start and end of the period covered
	 *                       by the invoice
	 */
	public static BillingPeriod computePeriod(LocalDate scheduledDate, IntervalType interval, InvoicedIn invoicedIn) {
		return switch (interval) {
			case YEARLY -> yearly(scheduledDate, invoicedIn);
			case HALF_YEARLY -> halfYearly(scheduledDate, invoicedIn);
			case QUARTERLY -> quarterly(scheduledDate, invoicedIn);
			case MONTHLY -> monthly(scheduledDate, invoicedIn);
		};
	}

	private static BillingPeriod yearly(LocalDate scheduledDate, InvoicedIn invoicedIn) {
		// LAND_LEASE_RESIDENTIAL with current period ending 30 June uses a
		// June slot; everything else uses a December slot.
		if (scheduledDate.getMonth() == Month.JUNE) {
			int year = scheduledDate.getYear();
			return InvoicedIn.ADVANCE.equals(invoicedIn)
				? period(year, Month.JULY, 1, year + 1, Month.JUNE, 30)
				: period(year - 1, Month.JULY, 1, year, Month.JUNE, 30);
		}
		int billedYear = InvoicedIn.ADVANCE.equals(invoicedIn)
			? scheduledDate.getYear() + 1
			: scheduledDate.getYear();
		return period(billedYear, Month.JANUARY, 1, billedYear, Month.DECEMBER, 31);
	}

	private static BillingPeriod halfYearly(LocalDate scheduledDate, InvoicedIn invoicedIn) {
		int month = scheduledDate.getMonthValue();
		int year = scheduledDate.getYear();
		boolean firstHalfSlot = month <= Month.JUNE.getValue();
		if (InvoicedIn.ADVANCE.equals(invoicedIn)) {
			return firstHalfSlot
				? period(year, Month.JULY, 1, year, Month.DECEMBER, 31)
				: period(year + 1, Month.JANUARY, 1, year + 1, Month.JUNE, 30);
		}
		return firstHalfSlot
			? period(year, Month.JANUARY, 1, year, Month.JUNE, 30)
			: period(year, Month.JULY, 1, year, Month.DECEMBER, 31);
	}

	private static BillingPeriod quarterly(LocalDate scheduledDate, InvoicedIn invoicedIn) {
		int month = scheduledDate.getMonthValue();
		int year = scheduledDate.getYear();
		// Map the slot's month to a quarter index, then shift +1 for ADVANCE.
		int slotQuarter = (month - 1) / 3 + 1;   // 1..4
		int billedQuarter = InvoicedIn.ADVANCE.equals(invoicedIn) ? slotQuarter + 1 : slotQuarter;
		int billedYear = year;
		if (billedQuarter > 4) {
			billedQuarter = 1;
			billedYear = year + 1;
		}
		return quarterPeriod(billedYear, billedQuarter);
	}

	private static BillingPeriod monthly(LocalDate scheduledDate, InvoicedIn invoicedIn) {
		LocalDate billedMonthFirst = InvoicedIn.ADVANCE.equals(invoicedIn)
			? scheduledDate.plusMonths(1).withDayOfMonth(1)
			: scheduledDate.withDayOfMonth(1);
		LocalDate billedMonthLast = billedMonthFirst.withDayOfMonth(billedMonthFirst.lengthOfMonth());
		return new BillingPeriod(billedMonthFirst, billedMonthLast);
	}

	private static BillingPeriod quarterPeriod(int year, int quarter) {
		Month firstMonth = Month.of((quarter - 1) * 3 + 1);
		Month lastMonth = Month.of(quarter * 3);
		LocalDate end = LocalDate.of(year, lastMonth, 1);
		return new BillingPeriod(LocalDate.of(year, firstMonth, 1), end.withDayOfMonth(end.lengthOfMonth()));
	}

	private static BillingPeriod period(int startYear, Month startMonth, int startDay,
		int endYear, Month endMonth, int endDay) {
		return new BillingPeriod(
			LocalDate.of(startYear, startMonth, startDay),
			LocalDate.of(endYear, endMonth, endDay));
	}
}
