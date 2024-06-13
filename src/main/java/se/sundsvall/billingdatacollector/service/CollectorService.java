package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorIntegration;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.billingdatacollector.service.decorator.BillingRecordDecorator;

@Service
public class CollectorService {

	private static final Logger LOG = LoggerFactory.getLogger(CollectorService.class);

	private final DbService dbService;
	private final OpenEIntegration openEIntegration;
	private final BillingPreprocessorIntegration preprocessorIntegration;
	private final Map<String, BillingRecordDecorator> decorators;

	public CollectorService(DbService dbService, OpenEIntegration openEIntegration, BillingPreprocessorIntegration preprocessorIntegration, List<BillingRecordDecorator> decorators) {
		this.dbService = dbService;
		this.openEIntegration = openEIntegration;
		this.preprocessorIntegration = preprocessorIntegration;

		//Get all Decorators and add them to the map with their corresponding familyId.
		this.decorators = decorators.stream().collect(Collectors.toMap(BillingRecordDecorator::getSupportedFamilyId, Function.identity()));
	}

	/**
	 * Trigger billing for a specific flowInstanceId.
	 *
	 * @param flowInstanceId The flowInstanceId to trigger billing for
	 */
	public void trigger(String flowInstanceId) {
		LOG.info("Triggering billing for flowInstanceId: {}", flowInstanceId);

		var possibleWrapper = openEIntegration.getBillingRecord(flowInstanceId);

		//If we have a BillingRecordWrapper, decorate it and send it to the preprocessor
		possibleWrapper.ifPresentOrElse(
			wrapper -> {
				decorate(wrapper);
				createBillingRecord(wrapper);
			},
			() -> LOG.warn("No record found for flowInstanceId: {}", flowInstanceId));
	}

	private void createBillingRecord(BillingRecordWrapper billingRecordWrapper) {
		try {
			var response = preprocessorIntegration.createBillingRecord(billingRecordWrapper.getBillingRecord());
			LOG.info("Successfully sent record to preprocessor for flowInstanceId: {}", billingRecordWrapper.getFlowInstanceId());
			dbService.saveToHistory(billingRecordWrapper, response);
		} catch (Exception e) {
			//Save the BillingRecordWrapper if we failed to send it to the preprocessor
			LOG.warn("Failed to create a record for flowInstanceId: {}", billingRecordWrapper.getFlowInstanceId(), e);
			Optional.of(billingRecordWrapper)
				.ifPresent(wrapper -> dbService.saveFailedBillingRecord(wrapper, e.getMessage()));
		}
	}

	/**
	 * Trigger billing for all supported familyIds between the provided dates.
	 * Will check which flowInstanceIds that have already been processed and only trigger billing for the unprocessed ones.
	 *
	 * @param startDate The start date
	 * @param endDate The end date
	 * @param familyIds The familyIds to trigger billing for, may be null/empty
	 * @return A list of flowInstanceIds that have been triggered
	 */
	public List<String> triggerBetweenDates(LocalDate startDate, LocalDate endDate, Set<String> familyIds) {
		var supportedFamilyIds = getSupportedFamilyIds(familyIds);

		LOG.info("Triggering billing for familyIds: {}", supportedFamilyIds);

		List<String> idsToReturn = new ArrayList<>();

		// For each supported familyId, get all flowInstanceIds and trigger billing for them
		supportedFamilyIds
			.forEach(supportedFamilyId -> {
					LOG.info("Getting flowInstanceIds for familyId: {}", supportedFamilyId);
					var receivedFlowInstanceIds = openEIntegration.getFlowInstanceIds(supportedFamilyId, startDate.toString(), endDate.toString());

					// Trigger billing for the unprocessed flowInstanceIds
					receivedFlowInstanceIds.forEach(flowInstanceId -> {
						//Check if it's already been processed, if so skip it
						if(!dbService.hasAlreadyBeenProcessed(supportedFamilyId, flowInstanceId)) {
							try {
								idsToReturn.add(flowInstanceId);
								trigger(flowInstanceId);
							} catch (Exception e) {
								LOG.warn("Failed to trigger billing for familyId: {} and flowInstanceId: {}", supportedFamilyId, flowInstanceId, e);
							}
						} else {
							LOG.info("Billing for familyId: {} and flowInstanceId: {} has already been processed", supportedFamilyId, flowInstanceId);
						}
					});
				}
			);

		return idsToReturn;
	}

	/**
	 * Filter out familyIds that we support.
	 * If no familyIds are provided, return all supported familyIds.
	 * If familyIds are provided, return the intersection of supported familyIds and provided familyIds.
	 * If no supported familyIds are found (and familyIds are provided), throw a Problem.
	 *
	 * @param wantedFamilyIds The familyIds to check
	 * @return A Set of supported familyIds
	 */
	private Set<String> getSupportedFamilyIds(Set<String> wantedFamilyIds) {
		var supportedFamilyIds = openEIntegration.getSupportedFamilyIds();
		var validFamilyIds = new HashSet<>(supportedFamilyIds);
		LOG.info("Wanted familyIds: {}. Supported familyIds: {}", wantedFamilyIds, supportedFamilyIds);

		Optional.ofNullable(wantedFamilyIds)
			.filter(wanted -> !wanted.isEmpty())
			.ifPresent(validFamilyIds::retainAll);

		if (validFamilyIds.isEmpty()) {
			throw Problem.builder()
				.withTitle("No supported familyIds found")
				.withStatus(Status.BAD_REQUEST)
				.withDetail("Supported familyIds: " + supportedFamilyIds)
				.build();
		}

		return validFamilyIds;
	}

	/**
	 * Decorate the BillingRecordWrapper with the corresponding decorator, if any.
	 *
	 * @param recordWrapper The BillingRecordWrapper to decorate
	 */
	private void decorate(BillingRecordWrapper recordWrapper) {
		//Decorate only if we have a decorator for the familyId
		Optional.ofNullable(decorators.get(recordWrapper.getFamilyId()))
			.ifPresent(decorator -> decorator.decorate(recordWrapper));
	}
}
