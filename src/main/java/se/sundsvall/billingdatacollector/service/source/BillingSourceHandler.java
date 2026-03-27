package se.sundsvall.billingdatacollector.service.source;

import java.util.function.Consumer;

public interface BillingSourceHandler {

	void sendBillingRecords(String municipalityId, String externalId, Consumer<String> unhealthyMessageConsumer);
}
