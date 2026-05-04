package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.billingdatacollector.integration.relation.RelationClient;
import se.sundsvall.billingdatacollector.service.EntityMapper;
import se.sundsvall.billingdatacollector.service.source.AbstractHandler;
import se.sundsvall.billingdatacollector.service.source.BillingResult;
import se.sundsvall.billingdatacollector.service.source.BillingResult.Failed;
import se.sundsvall.billingdatacollector.service.source.BillingResult.Sent;
import se.sundsvall.billingdatacollector.service.source.BillingResult.Skipped;
import se.sundsvall.billingdatacollector.service.util.BillingPeriodCalculator;
import se.sundsvall.billingdatacollector.service.util.ScheduledBillingUtil;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.LOCATION;

@Component("contract")
public class ContractBillingHandler extends AbstractHandler {

	private static final String CONTRACT = "contract";
	private static final String BILLING_PREPROCESSOR_SERVICE = "billingpreprocessor";

	private final ContractIntegration contractIntegration;
	private final ContractMapper contractMapper;
	private final BillingPreprocessorClient billingPreprocessorClient;
	private final HistoryRepository historyRepository;
	private final RelationClient relationClient;

	ContractBillingHandler(
		ContractIntegration contractIntegration,
		ContractMapper contractMapper,
		BillingPreprocessorClient billingPreprocessorClient,
		HistoryRepository historyRepository,
		RelationClient relationClient) {

		this.contractIntegration = contractIntegration;
		this.contractMapper = contractMapper;
		this.billingPreprocessorClient = billingPreprocessorClient;
		this.historyRepository = historyRepository;
		this.relationClient = relationClient;
	}

	/**
	 * Sends a billing record for one scheduled-billing entity, after
	 * defensively re-fetching the contract and verifying that the period
	 * being billed still falls inside the contract.
	 *
	 * <p>
	 * Also computes the period of the <em>next</em> slot and returns
	 * {@code Sent(null)} when that period would extend past the contract's
	 * end date — the scheduler then deletes the entity after the last
	 * valid billing without needing access to the contract itself.
	 */
	@Transactional
	@Override
	public BillingResult sendBillingRecords(ScheduledBillingEntity entity) {
		var municipalityId = entity.getMunicipalityId();
		var externalId = entity.getExternalId();
		var scheduledDate = entity.getNextScheduledBilling();
		logInfo("Processing contract with id {} in municipality {}", externalId, municipalityId);

		var contractOpt = contractIntegration.getContract(municipalityId, externalId);
		if (contractOpt.isEmpty()) {
			// 404 is treated as an inconsistency, not a normal cleanup path:
			// the legitimate way to stop billing is a TERMINATED event, which
			// removes the entity before the scheduler runs. A missing contract
			// here means either a dropped event or a transient failure in the
			// contract service — keep the schedule and surface via health.
			logError("No contract found for id {} in municipality {} — expected TERMINATED event, keeping schedule for investigation",
				externalId, municipalityId);
			return new Failed("contract %s not found (HTTP 404) — expected TERMINATED event".formatted(externalId));
		}
		var contract = contractOpt.get();

		if (contract.getInvoicing() == null
			|| contract.getInvoicing().getInvoiceInterval() == null
			|| contract.getInvoicing().getInvoicedIn() == null) {
			// Contract no longer carries the invoicing fields needed to
			// produce a billing — clean up.
			logWarning("Contract {} no longer has complete invoicing data — dropping schedule", externalId);
			return new Skipped("contract no longer billable");
		}

		var interval = contract.getInvoicing().getInvoiceInterval();
		var invoicedIn = contract.getInvoicing().getInvoicedIn();
		var endDate = contract.getEndDate();

		// Defensive: even if applyEndDateLogic ran earlier, the contract may
		// have been updated since. Re-check the current period against the
		// end date.
		var currentPeriod = BillingPeriodCalculator.computePeriod(scheduledDate, interval, invoicedIn);
		if (endDate != null && currentPeriod.endDate().isAfter(endDate)) {
			logInfo("Skipping billing for contract {} — period {} extends past contract end {}",
				externalId, currentPeriod, endDate);
			return new Skipped("period extends past contract end date");
		}

		try {
			sendAndSave(municipalityId, contractMapper.createBillingRecord(municipalityId, contract, scheduledDate), externalId);
		} catch (Exception e) {
			logError("Failed to send billing for contract {}: {}", externalId, e.getMessage());
			return new Failed("Failed to create billing record(s) for source 'CONTRACT'");
		}

		// Compute the slot that would follow this one and decide whether it
		// is still inside the contract — the scheduler uses this to advance
		// vs delete the entity.
		var nextSlot = ScheduledBillingUtil.calculateNextScheduledBilling(
			entity.getBillingDaysOfMonth(), entity.getBillingMonths(), scheduledDate.plusDays(1));
		var nextPeriod = BillingPeriodCalculator.computePeriod(nextSlot, interval, invoicedIn);
		if (endDate != null && nextPeriod.endDate().isAfter(endDate)) {
			return new Sent(null);   // current billing was the last
		}
		return new Sent(nextSlot);
	}

	private void sendAndSave(String municipalityId, BillingRecord billingRecord, String contractId) {
		logInfo("Sending billing record to billing preprocessor");
		final var response = billingPreprocessorClient.createBillingRecord(municipalityId, billingRecord);
		logInfo("Billing record sent successfully with response status: {}", response.getStatusCode());

		// Save to history
		historyRepository.saveAndFlush(EntityMapper.mapToHistoryEntity(municipalityId, billingRecord, getLocation(response)));

		// Relation creation is best-effort — failure does not roll back the
		// billing. Caller (scheduler) reports relation issues via the
		// health indicator on the next tick.
		try {
			relationClient.createRelation(municipalityId, createRelation(contractId, extractIdFromLocationHeader(response)));
		} catch (Exception e) {
			logWarning("Error creating relation for contract {}: {}", contractId, e.getMessage());
		}
	}

	private String extractIdFromLocationHeader(final ResponseEntity<Void> response) {
		final var locationValue = Optional.ofNullable(response.getHeaders().get(LOCATION))
			.orElse(emptyList()).stream().findFirst();
		return locationValue.map(string -> string.substring(string.lastIndexOf('/') + 1)).orElse(EMPTY);
	}

	private Relation createRelation(String contractId, String billingRecordId) {
		var relation = new Relation();
		relation.setType("LINK");
		relation.setSource(createResourceIdentifier(CONTRACT, CONTRACT, contractId));
		relation.setTarget(createResourceIdentifier(BILLING_PREPROCESSOR_SERVICE, "billing-record", billingRecordId));
		return relation;
	}

	private ResourceIdentifier createResourceIdentifier(String service, String type, String id) {
		var resource = new ResourceIdentifier();
		resource.setService(service);
		resource.setType(type);
		resource.setResourceId(id);
		return resource;
	}

	private String getLocation(ResponseEntity<Void> response) {
		return Optional.of(response.getHeaders())
			.map(HttpHeaders::getLocation)
			.map(URI::toString)
			.orElse(null);
	}
}
