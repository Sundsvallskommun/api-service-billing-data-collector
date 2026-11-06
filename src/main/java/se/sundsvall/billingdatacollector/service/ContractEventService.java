package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.Status;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.EventRequest;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil;
import se.sundsvall.billingdatacollector.service.util.BillingPeriodCalculator;
import se.sundsvall.billingdatacollector.service.util.ScheduledBillingUtil;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

// Bean name is intentionally upper-case to match BillingSource.name(), which
// is the lookup key used by CollectorResource.handleEvent. The convention is
// documented in CLAUDE.md.
@Service("CONTRACT")
public class ContractEventService implements BillingEventHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ContractEventService.class);

	private static final BillingSource SOURCE = BillingSource.CONTRACT;
	private static final Set<Integer> BILLING_DAYS_OF_MONTH = Set.of(1);
	private static final Set<Integer> MONTHLY = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	private static final Set<Integer> QUARTERLY = Set.of(3, 6, 9, 12);
	private static final Set<Integer> HALF_YEARLY = Set.of(6, 12);
	private static final Set<Integer> YEARLY = Set.of(12);
	private static final Set<Integer> YEARLY_JUNE = Set.of(6);

	private final ScheduledBillingService scheduledBillingService;
	private final ContractIntegration contractIntegration;
	private final Clock clock;

	public ContractEventService(ScheduledBillingService scheduledBillingService,
		ContractIntegration contractIntegration, Clock clock) {
		this.scheduledBillingService = scheduledBillingService;
		this.contractIntegration = contractIntegration;
		this.clock = clock;
	}

	@Override
	public void handleEvent(EventRequest request) {
		LOG.info("Handling {} for contractId: {} municipalityId: {}",
			request.getEventType(), sanitizeForLogging(request.getId()), sanitizeForLogging(request.getMunicipalityId()));

		switch (request.getEventType()) {
			case CREATED -> handleCreated(request.getMunicipalityId(), request.getId());
			case UPDATED -> handleUpdated(request.getMunicipalityId(), request.getId());
			case DELETED -> handleDeleted(request.getMunicipalityId(), request.getId());
			case TERMINATED -> handleTerminated(request.getMunicipalityId(), request.getId());
		}
	}

	private void handleCreated(String municipalityId, String contractId) {
		contractIntegration.getContract(municipalityId, contractId)
			.filter(this::isBillable)
			.ifPresent(contract -> upsertAndApplyEndDate(municipalityId, contractId, contract));
	}

	private void handleUpdated(String municipalityId, String contractId) {
		contractIntegration.getContract(municipalityId, contractId)
			.ifPresentOrElse(
				contract -> {
					if (isBillable(contract)) {
						upsertAndApplyEndDate(municipalityId, contractId, contract);
					} else {
						scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE);
					}
				},
				() -> scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE));
	}

	/**
	 * Shared CREATED/UPDATED path: upsert the schedule (preserving
	 * {@code nextScheduledBilling} on existing rows unless the cadence or
	 * invoicing direction actually changed), then re-evaluate the end date.
	 */
	private void upsertAndApplyEndDate(String municipalityId, String contractId, Contract contract) {
		var billingMonths = calculateBillingMonths(contract);
		var invoicedIn = contract.getInvoicing() != null
			? mapInvoicedIn(contract.getInvoicing().getInvoicedIn())
			: null;
		scheduledBillingService.upsert(municipalityId, contractId, SOURCE,
			billingMonths, BILLING_DAYS_OF_MONTH, invoicedIn,
			() -> calculateStartFrom(contract, billingMonths));
		applyEndDateLogic(municipalityId, contractId, contract);
	}

	/**
	 * Maps the generated Contract-service {@code InvoicedIn} to the BDC-owned
	 * persistence enum so the entity layer is independent of the generated
	 * model. Returns {@code null} if the source value is null (treated as
	 * "unknown direction" downstream).
	 */
	private static se.sundsvall.billingdatacollector.api.model.InvoicedIn mapInvoicedIn(InvoicedIn source) {
		return switch (source) {
			case null -> null;
			case ADVANCE -> se.sundsvall.billingdatacollector.api.model.InvoicedIn.ADVANCE;
			case ARREARS -> se.sundsvall.billingdatacollector.api.model.InvoicedIn.ARREARS;
		};
	}

	private void handleDeleted(String municipalityId, String contractId) {
		scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE);
	}

	private void handleTerminated(String municipalityId, String contractId) {
		contractIntegration.getContract(municipalityId, contractId)
			.ifPresentOrElse(
				contract -> {
					if (ContractUtil.isBillingDisabled(contract)) {
						// billingEnabled = false overrides every other rule — drop the
						// schedule now instead of waiting for the next scheduler tick.
						scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE);
					} else if (contract.getEndDate() != null) {
						applyEndDateLogic(municipalityId, contractId, contract);
					} else {
						// Terminated without an end date — no further billings
						// make sense for this contract.
						scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE);
					}
				},
				() -> scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE));
	}

	/**
	 * Cleans up the scheduled billing when a contract's end date is known and
	 * the next scheduled slot already lies inside an out-of-contract period.
	 * The actual decision of "is this billing the last one?" is made on every
	 * scheduler tick by {@code ContractBillingHandler.sendBillingRecords},
	 * which compares the live contract against the period being billed; this
	 * method only handles the early-cleanup case where there is nothing left
	 * to bill at all.
	 *
	 * <p>
	 * No-ops when {@code endDate} or {@code invoicedIn} is null
	 * (insufficient information), or when no scheduled billing exists for
	 * the contract.
	 */
	private void applyEndDateLogic(String municipalityId, String contractId, Contract contract) {
		var endDate = contract.getEndDate();
		var invoicedIn = contract.getInvoicing() != null ? contract.getInvoicing().getInvoicedIn() : null;
		var interval = contract.getInvoicing() != null ? contract.getInvoicing().getInvoiceInterval() : null;
		if (endDate == null || invoicedIn == null || interval == null) {
			return;
		}

		var nextBilling = scheduledBillingService.getNextScheduledBilling(municipalityId, contractId, SOURCE);
		if (nextBilling.isEmpty()) {
			return;
		}

		// Current slot's period extends past end date — no more billings will
		// be valid, drop the schedule. When the period still fits, the
		// scheduler will fire as planned and ContractBillingHandler decides
		// whether the following slot is still within the contract.
		var currentPeriod = BillingPeriodCalculator.computePeriod(nextBilling.get(), interval, invoicedIn);
		if (currentPeriod.endDate().isAfter(endDate)) {
			scheduledBillingService.deleteByExternalId(municipalityId, contractId, SOURCE);
		}
	}

	/**
	 * Returns the very first {@code nextScheduledBilling} for a contract: the
	 * first billing slot on or after today whose billed period (as defined by
	 * {@link BillingPeriodCalculator}) starts on or after the contract's start
	 * date. The same rule applies to both directions; only the period a slot
	 * bills differs.
	 *
	 * <ul>
	 * <li><b>ADVANCE</b>: a slot bills the period that follows it, so a
	 * contract starting 2027-01-01 with yearly billing gets 2026-12-01.</li>
	 * <li><b>ARREARS</b>: a slot bills the period it closes, so the same
	 * contract gets 2027-12-01.</li>
	 * <li><b>Partial periods</b>: a period that starts before the contract is
	 * not billed automatically (a contract starting 2027-03-15 with yearly
	 * billing gets 2027-12-01 for ADVANCE and 2028-12-01 for ARREARS).</li>
	 * <li><b>Passed slots</b>: never scheduled — a period whose slot has
	 * already passed is handled by a manual invoice.</li>
	 * <li><b>Null invoicedIn</b>: defaults to ARREARS behaviour.</li>
	 * </ul>
	 */
	private LocalDate calculateStartFrom(Contract contract, Set<Integer> billingMonths) {
		var today = LocalDate.now(clock);
		var contractStart = contract.getStartDate() != null ? contract.getStartDate() : today;
		var interval = contract.getInvoicing().getInvoiceInterval();
		// Null invoicedIn defaults to ARREARS — made explicit here rather than
		// relying on BillingPeriodCalculator's implicit handling of null.
		var invoicedIn = Optional.ofNullable(contract.getInvoicing().getInvoicedIn())
			.orElse(InvoicedIn.ARREARS);

		// Start from today so a slot that has already passed is never
		// scheduled. The advance/arrears difference (which period a slot
		// bills) lives in BillingPeriodCalculator.
		var slot = firstSlotBillingFrom(today, contractStart, billingMonths, interval, invoicedIn);

		// Without the today-clamp the first slot could lie up to one interval
		// (at most a year) before the contract start. If that slot is earlier
		// than the one scheduled, the periods in between have a passed slot
		// and must be invoiced manually — make that visible.
		var unclampedSlot = firstSlotBillingFrom(contractStart.minusYears(1), contractStart, billingMonths, interval, invoicedIn);
		if (unclampedSlot.isBefore(slot)) {
			LOG.warn("Contract {}: period {} - {} is not billed automatically since its billing slot has passed — issue a manual invoice",
				sanitizeForLogging(contract.getContractId()),
				BillingPeriodCalculator.computePeriod(unclampedSlot, interval, invoicedIn).startDate(),
				BillingPeriodCalculator.computePeriod(slot, interval, invoicedIn).startDate().minusDays(1));
		}
		return slot;
	}

	/**
	 * First billing slot on or after {@code searchFrom} whose billed period
	 * starts on or after {@code contractStart}.
	 */
	private static LocalDate firstSlotBillingFrom(LocalDate searchFrom, LocalDate contractStart,
		Set<Integer> billingMonths, IntervalType interval, InvoicedIn invoicedIn) {
		var slot = ScheduledBillingUtil.calculateNextScheduledBilling(BILLING_DAYS_OF_MONTH, billingMonths, searchFrom);
		while (BillingPeriodCalculator.computePeriod(slot, interval, invoicedIn).startDate().isBefore(contractStart)) {
			slot = ScheduledBillingUtil.calculateNextScheduledBilling(BILLING_DAYS_OF_MONTH, billingMonths, slot.plusDays(1));
		}
		return slot;
	}

	/**
	 * {@code invoicing.billingEnabled = false} overrides every other billing
	 * rule — such a contract never has a scheduled billing, regardless of
	 * status or invoicing interval. See {@link ContractUtil#isBillable} for
	 * the shared definition also used when actually sending a billing.
	 */
	private boolean isBillable(Contract contract) {
		return Status.ACTIVE.equals(contract.getStatus()) && ContractUtil.isBillable(contract);
	}

	private Set<Integer> calculateBillingMonths(Contract contract) {
		return switch (contract.getInvoicing().getInvoiceInterval()) {
			case MONTHLY -> MONTHLY;
			case QUARTERLY -> QUARTERLY;
			case HALF_YEARLY -> HALF_YEARLY;
			case YEARLY -> {
				warnIfYearlyBillingMonthUnexpected(contract);
				yield ContractUtil.isJuneYearlyBilling(contract) ? YEARLY_JUNE : YEARLY;
			}
		};
	}

	private void warnIfYearlyBillingMonthUnexpected(Contract contract) {
		var yearlyBillingMonth = contract.getInvoicing().getYearlyBillingMonth();
		if (yearlyBillingMonth != null && yearlyBillingMonth != 6 && yearlyBillingMonth != 12) {
			LOG.warn("Contract {} has YEARLY invoicing with an unexpected yearlyBillingMonth {} (expected 6 or 12) — defaulting to the December billing slot",
				sanitizeForLogging(contract.getContractId()), yearlyBillingMonth);
		}
	}
}
