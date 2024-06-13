package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@Component
public class BillingPreprocessorIntegration {

	private final BillingPreprocessorClient client;

	BillingPreprocessorIntegration(final BillingPreprocessorClient client) {
		this.client = client;
	}

	public ResponseEntity<Void> createBillingRecord(final BillingRecord billingRecord) {
		return client.createBillingRecord(billingRecord);
	}
}
