package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorIntegration;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.billingdatacollector.service.decorator.BillingRecordDecorator;

@Service
public class CollectorService {

	private static final Logger LOG = LoggerFactory.getLogger(CollectorService.class);

	private final OpenEIntegration openEIntegration;
	private final BillingPreprocessorIntegration preprocessorIntegration;
	private final Map<String, BillingRecordDecorator> decorators;

	public CollectorService(OpenEIntegration openEIntegration, BillingPreprocessorIntegration preprocessorIntegration, List<BillingRecordDecorator> decorators) {
		this.openEIntegration = openEIntegration;
		this.preprocessorIntegration = preprocessorIntegration;

		//Get all Decorators and add them to the map with their corresponding familyId.
		this.decorators = decorators.stream().collect(Collectors.toMap(BillingRecordDecorator::getSupportedFamilyId, Function.identity()));
	}

	public void trigger(String flowInstanceId) {
		LOG.info("Triggering billing for flowInstanceId: {}", flowInstanceId);

		var billingRecordWrapper = openEIntegration.getBillingRecord(flowInstanceId);
		decorate(billingRecordWrapper);
		preprocessorIntegration.createBillingRecord(billingRecordWrapper.getBillingRecord());
	}

	public void triggerBetweenDates(LocalDate startDate, LocalDate endDate) {
		openEIntegration.getSupportedFamilyIds()
			.forEach(familyId -> openEIntegration.getFlowInstanceIds(familyId, startDate.toString(), endDate.toString())
				.forEach(this::trigger));
	}

	private void decorate(BillingRecordWrapper recordWrapper) {
		//Decorate only if we have a decorator for the familyId
		Optional.ofNullable(decorators.get(recordWrapper.getFamilyId()))
			.ifPresent(decorator -> decorator.decorate(recordWrapper));
	}
}
