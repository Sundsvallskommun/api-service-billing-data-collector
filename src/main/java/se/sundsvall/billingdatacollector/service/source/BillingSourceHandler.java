package se.sundsvall.billingdatacollector.service.source;

public interface BillingSourceHandler {

	void sendBillingRecords(String municipalityId, String externalId);
}
