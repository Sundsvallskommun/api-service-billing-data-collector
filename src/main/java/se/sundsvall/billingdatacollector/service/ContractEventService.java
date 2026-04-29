package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.LeaseType;
import generated.se.sundsvall.contract.Status;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.EventRequest;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
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
		var invoicedIn = mapInvoicedIn(contract.getInvoicing().getInvoicedIn());
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
					if (contract.getEndDate() != null) {
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
	 * Returns the lower bound to feed into
	 * {@link ScheduledBillingUtil#calculateNextScheduledBilling} when computing
	 * the very first {@code nextScheduledBilling} for a contract.
	 *
	 * <ul>
	 * <li><b>ADVANCE</b>: the first valid slot is the next billing slot on
	 * or after the contract's start date — the invoice covers the
	 * period that follows the slot. If the start date lies in the past
	 * we clamp to today so we never schedule a billing for a slot that
	 * has already passed; the gap is handled by a manual invoice.</li>
	 * <li><b>ARREARS</b>: billing follows the period, so the first valid
	 * slot is the slot <em>after</em> the first slot found from
	 * {@code startDate}. We do <em>not</em> clamp to today here — the
	 * skip-one-slot rule already protects against billing for a period
	 * that started before the contract, and clamping would over-skip
	 * when {@code startDate} is in the past.</li>
	 * <li><b>Null invoicedIn</b>: defaults to ADVANCE behaviour.</li>
	 * </ul>
	 */
	private LocalDate calculateStartFrom(Contract contract, Set<Integer> billingMonths) {
		var today = LocalDate.now(clock);
		var contractStart = contract.getStartDate() != null ? contract.getStartDate() : today;
		var invoicedIn = contract.getInvoicing().getInvoicedIn();

		if (InvoicedIn.ARREARS.equals(invoicedIn)) {
			// Skip the first slot whose period starts before the contract.
			// ARREARS bills at the end of the covered period, so the first
			// scheduled billing belongs to the second slot. No today-clamp
			// here — see javadoc.
			var firstAdvanceSlot = ScheduledBillingUtil.calculateNextScheduledBilling(
				BILLING_DAYS_OF_MONTH, billingMonths, contractStart);
			return ScheduledBillingUtil.calculateNextScheduledBilling(
				BILLING_DAYS_OF_MONTH, billingMonths, firstAdvanceSlot.plusDays(1));
		}

		// ADVANCE — clamp to today so we never schedule a billing in the past.
		return contractStart.isBefore(today) ? today : contractStart;
	}

	private boolean isBillable(Contract contract) {
		return Status.ACTIVE.equals(contract.getStatus())
			&& contract.getInvoicing() != null
			&& contract.getInvoicing().getInvoiceInterval() != null;
	}

	private Set<Integer> calculateBillingMonths(Contract contract) {
		return switch (contract.getInvoicing().getInvoiceInterval()) {
			case MONTHLY -> MONTHLY;
			case QUARTERLY -> QUARTERLY;
			case HALF_YEARLY -> HALF_YEARLY;
			case YEARLY -> isLandLeaseResidentialEndOfJune(contract) ? YEARLY_JUNE : YEARLY;
		};
	}

	private boolean isLandLeaseResidentialEndOfJune(Contract contract) {
		var periodEndDate = contract.getCurrentPeriod() != null ? contract.getCurrentPeriod().getEndDate() : null;
		return periodEndDate != null
			&& periodEndDate.getMonth().equals(Month.JUNE)
			&& periodEndDate.getDayOfMonth() == 30
			&& LeaseType.LAND_LEASE_RESIDENTIAL.equals(contract.getLeaseType());
	}
}
