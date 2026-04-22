package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.LeaseType;
import generated.se.sundsvall.contract.Status;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.billingdatacollector.api.model.ContractEventRequest;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Service
public class ContractEventService {

	private static final Logger LOG = LoggerFactory.getLogger(ContractEventService.class);

	private static final Set<Integer> BILLING_DAYS_OF_MONTH = Set.of(1);
	private static final Set<Integer> MONTHLY = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
	private static final Set<Integer> QUARTERLY = Set.of(3, 6, 9, 12);
	private static final Set<Integer> HALF_YEARLY = Set.of(6, 12);
	private static final Set<Integer> YEARLY = Set.of(12);
	private static final Set<Integer> YEARLY_JUNE = Set.of(6);

	private final ScheduledBillingService scheduledBillingService;
	private final ContractIntegration contractIntegration;

	public ContractEventService(ScheduledBillingService scheduledBillingService, ContractIntegration contractIntegration) {
		this.scheduledBillingService = scheduledBillingService;
		this.contractIntegration = contractIntegration;
	}

	public void handleEvent(String municipalityId, ContractEventRequest request) {
		LOG.info("Handling {} for contractId: {} municipalityId: {}",
			request.getEventType(), sanitizeForLogging(request.getId()), sanitizeForLogging(municipalityId));

		switch (request.getEventType()) {
			case CONTRACT_CREATED -> handleCreated(municipalityId, request.getId());
			case CONTRACT_UPDATED -> handleUpdated(municipalityId, request.getId());
			case CONTRACT_DELETED -> handleDeleted(municipalityId, request.getId());
			case CONTRACT_TERMINATED -> handleTerminated(municipalityId, request.getId());
		}
	}

	private void handleCreated(String municipalityId, String contractId) {
		contractIntegration.getContract(municipalityId, contractId)
			.ifPresent(contract -> {
				if (isBillable(contract)) {
					scheduledBillingService.upsertByContractId(municipalityId, contractId,
						calculateBillingMonths(contract), BILLING_DAYS_OF_MONTH);
					applyEndDateLogic(municipalityId, contractId, contract);
				}
			});
	}

	private void handleUpdated(String municipalityId, String contractId) {
		contractIntegration.getContract(municipalityId, contractId)
			.ifPresentOrElse(
				contract -> {
					if (isBillable(contract)) {
						scheduledBillingService.upsertByContractId(municipalityId, contractId,
							calculateBillingMonths(contract), BILLING_DAYS_OF_MONTH);
						applyEndDateLogic(municipalityId, contractId, contract);
					} else {
						scheduledBillingService.deleteByContractId(municipalityId, contractId);
					}
				},
				() -> scheduledBillingService.deleteByContractId(municipalityId, contractId));
	}

	private void handleDeleted(String municipalityId, String contractId) {
		scheduledBillingService.deleteByContractId(municipalityId, contractId);
	}

	private void handleTerminated(String municipalityId, String contractId) {
		contractIntegration.getContract(municipalityId, contractId)
			.ifPresentOrElse(
				contract -> applyEndDateLogic(municipalityId, contractId, contract),
				() -> scheduledBillingService.deleteByContractId(municipalityId, contractId));
	}

	/**
	 * Evaluates whether the scheduled billing for a contract should be kept (and marked as final)
	 * or removed, based on the contract's endDate and invoicing direction.
	 *
	 * <ul>
	 * <li>ADVANCE + endDate after nextScheduledBilling → keep, mark as final</li>
	 * <li>ADVANCE + endDate on/before nextScheduledBilling → delete</li>
	 * <li>ARREARS + endDate on/before nextScheduledBilling → keep, mark as final</li>
	 * <li>ARREARS + endDate after nextScheduledBilling → delete</li>
	 * <li>endDate null or invoicedIn null → no action (insufficient information)</li>
	 * </ul>
	 */
	private void applyEndDateLogic(String municipalityId, String contractId, Contract contract) {
		LocalDate endDate = contract.getEndDate();
		InvoicedIn invoicedIn = contract.getInvoicing() != null ? contract.getInvoicing().getInvoicedIn() : null;

		if (endDate == null || invoicedIn == null) {
			return;
		}

		var nextBilling = scheduledBillingService.getNextScheduledBillingByContractId(municipalityId, contractId);
		if (nextBilling.isEmpty()) {
			return;
		}

		boolean advance = InvoicedIn.ADVANCE.equals(invoicedIn);
		boolean keepAsFinal = advance
			? endDate.isAfter(nextBilling.get())
			: !endDate.isAfter(nextBilling.get());

		if (keepAsFinal) {
			scheduledBillingService.updateFinalBillingDate(municipalityId, contractId, endDate);
		} else {
			scheduledBillingService.deleteByContractId(municipalityId, contractId);
		}
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
