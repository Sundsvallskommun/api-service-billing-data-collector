package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import org.springframework.stereotype.Component;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@Component
public class BillingPreprocessorIntegration {

	private final BillingPreprocessorClient client;

	BillingPreprocessorIntegration(final BillingPreprocessorClient client) {
		this.client = client;
	}

	public void createBillingRecord(final BillingRecord billingRecord) {
		client.createBillingRecord(billingRecord);
	}
}
