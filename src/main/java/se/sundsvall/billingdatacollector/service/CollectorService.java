package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
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

	private final FalloutService falloutService;
	private final OpenEIntegration openEIntegration;
	private final BillingPreprocessorIntegration preprocessorIntegration;
	private final Map<String, BillingRecordDecorator> decorators;

	public CollectorService(FalloutService falloutService, OpenEIntegration openEIntegration, BillingPreprocessorIntegration preprocessorIntegration, List<BillingRecordDecorator> decorators) {
		this.falloutService = falloutService;
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
				createBillingRecord(wrapper, flowInstanceId);
			},
			() -> LOG.warn("No record found for flowInstanceId: {}", flowInstanceId));
	}

	private void createBillingRecord(BillingRecordWrapper billingRecordWrapper, String flowInstanceId) {
		try {
			preprocessorIntegration.createBillingRecord(billingRecordWrapper.getBillingRecord());
			LOG.info("Successfully sent record to preprocessor for flowInstanceId: {}", flowInstanceId);
		} catch (Exception e) {
			//Save the BillingRecordWrapper if we failed to send it to the preprocessor
			LOG.warn("Failed to create a record for flowInstanceId: {}", flowInstanceId, e);
			Optional.of(billingRecordWrapper)
				.ifPresent(wrapper ->  falloutService.saveFailedBillingRecord(wrapper, e.getMessage()));
		}
	}

	/**
	 * Trigger billing for all supported familyIds between the provided dates.
	 * @param startDate The start date
	 * @param endDate 	The end date
	 * @param familyIds The familyIds to trigger billing for, may be null/empty
	 */
	public void triggerBetweenDates(LocalDate startDate, LocalDate endDate, Set<String> familyIds) {
		var supportedFamilyIds = getSupportedFamilyIdsFromRequest(familyIds);

		LOG.info("Triggering billing for familyIds: {}", supportedFamilyIds);

		// For each supported familyId, get all flowInstanceIds and trigger billing for them
		supportedFamilyIds
			.forEach(supportedFamilyId -> {
					LOG.info("Getting flowInstanceIds for familyId: {}", supportedFamilyId);
					openEIntegration.getFlowInstanceIds(supportedFamilyId, startDate.toString(), endDate.toString())
						.forEach(flowInstanceId -> {
							LOG.info("Triggering billing for familyId: {} and flowInstanceId: {}", supportedFamilyId, flowInstanceId);
							try {
								trigger(flowInstanceId);
							} catch (Exception e) {
								LOG.warn("Failed to trigger billing for familyId: {} and flowInstanceId: {}", supportedFamilyId, flowInstanceId, e);
							}
						});
				}
			);
	}

	/**
	 * Filter out familyIds that we do not support.
	 * If no familyIds are provided, return all supported familyIds.
	 * If familyIds are provided, return the intersection of supported familyIds and provided familyIds.
	 * If no supported familyIds are found (and familyIds are provided), throw a Problem.
	 * @param wantedFamilyIds The familyIds that we want to check if we support
	 * @return A Set of supported familyIds
	 */
	private Set<String> getSupportedFamilyIdsFromRequest(Set<String> wantedFamilyIds) {
		var supportedFamilyIds = openEIntegration.getSupportedFamilyIds();
		var resultingFamilyIds = new HashSet<>(supportedFamilyIds);
		LOG.info("Wanted familyIds: {}. Supported familyIds: {}", wantedFamilyIds, supportedFamilyIds);

		Optional.ofNullable(wantedFamilyIds)
			.filter(wanted -> !wanted.isEmpty())
			.ifPresent(resultingFamilyIds::retainAll);

		if(resultingFamilyIds.isEmpty()) {
			throw Problem.builder()
				.withTitle("No supported familyIds found")
				.withStatus(Status.BAD_REQUEST)
				.withDetail("Supported familyIds: " + supportedFamilyIds)
				.build();
		}

		return resultingFamilyIds;
	}

	/**
	 * Decorate the BillingRecordWrapper with the corresponding decorator, if any.
	 * @param recordWrapper The BillingRecordWrapper to decorate
	 */
	private void decorate(BillingRecordWrapper recordWrapper) {
		//Decorate only if we have a decorator for the familyId
		Optional.ofNullable(decorators.get(recordWrapper.getFamilyId()))
			.ifPresent(decorator -> decorator.decorate(recordWrapper));
	}
}
