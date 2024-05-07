package se.sundsvall.billingdatacollector.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorIntegration;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegration;
import se.sundsvall.billingdatacollector.service.mapper.BillingRecordDecorator;

@Service
public class CollectorService {

	private final OpenEIntegration openEIntegration;
	private final BillingPreprocessorIntegration preprocessorIntegration;
	private final Map<String, BillingRecordDecorator> decorators;

	public CollectorService(OpenEIntegration openEIntegration, BillingPreprocessorIntegration preprocessorIntegration, List<BillingRecordDecorator> decorators) {
		this.openEIntegration = openEIntegration;
		this.preprocessorIntegration = preprocessorIntegration;

		//Get all Decorators and add them to the map with their corresponding familyId.
		this.decorators = decorators.stream().collect(Collectors.toMap(BillingRecordDecorator::getSupportedFamilyId, Function.identity()));
	}

	public void sendBillingData(String flowInstanceId) {
		//Fetch data from OpenE
		var recordWrapper = openEIntegration.getBillingRecord(flowInstanceId);

		//Decorate with correct decorator
		decorators.get(recordWrapper.getFamilyId()).decorate(recordWrapper);

		//Send to BillingPreProcessor
		preprocessorIntegration.createBillingRecord(recordWrapper.getBillingRecord());
	}

}
