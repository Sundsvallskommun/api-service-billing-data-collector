package se.sundsvall.billingdatacollector.service.source;

import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

/**
 * Source-specific implementation that knows how to fetch the underlying
 * record (e.g. a contract or an Open-E flow instance), turn it into a
 * billing record, and POST it to the billing-preprocessor service.
 *
 * <p>
 * Implementations are looked up by lowercase {@code BillingSource} name
 * (e.g. {@code "contract"}) in the bean map injected into
 * {@code BillingScheduler}.
 */
public interface BillingSourceHandler {

	/**
	 * Send the billing for one scheduled entity. The handler is responsible
	 * for re-fetching the source record, deciding whether the period the
	 * billing covers still falls inside the source's active interval, and
	 * computing the next slot if any.
	 *
	 * @param  entity the scheduled-billing row that came due. The handler
	 *                may use {@code billingMonths} / {@code billingDaysOfMonth}
	 *                to compute the next slot; the scheduler uses the
	 *                returned {@link BillingResult} to decide whether to
	 *                delete the row, advance it, or report a failure.
	 * @return        the outcome — see {@link BillingResult}.
	 */
	BillingResult sendBillingRecords(ScheduledBillingEntity entity);
}
