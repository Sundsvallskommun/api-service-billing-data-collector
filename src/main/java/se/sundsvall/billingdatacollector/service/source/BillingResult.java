package se.sundsvall.billingdatacollector.service.source;

import java.time.LocalDate;

/**
 * Outcome of a single scheduled-billing run for one entity. The handler knows
 * whether the contract is still active for this period and whether another
 * billing should follow; it tells the scheduler what to do with the entity
 * via this return value.
 */
public sealed interface BillingResult {

	/**
	 * The invoice was sent. {@code nextSlot == null} means this was the last
	 * billing for the contract (its end date falls inside the next slot's
	 * period) and the entity should be deleted. A non-null value tells the
	 * scheduler to advance {@code nextScheduledBilling} to that date.
	 */
	record Sent(LocalDate nextSlot)
		implements
		BillingResult {}

	/**
	 * No invoice was sent because the period that would have been billed
	 * extends past the contract's end date (or the contract was missing or
	 * no longer billable). The scheduler should delete the entity — there
	 * will be no further billings.
	 */
	record Skipped(String reason)
		implements
		BillingResult {}

	/**
	 * Sending failed; the scheduler should mark the health indicator
	 * unhealthy and leave the entity untouched so the next run retries.
	 */
	record Failed(String reason)
		implements
		BillingResult {}
}
