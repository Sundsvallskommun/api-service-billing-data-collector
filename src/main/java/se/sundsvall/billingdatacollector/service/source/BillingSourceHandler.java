package se.sundsvall.billingdatacollector.service.source;

import java.time.LocalDate;
import java.util.function.Consumer;

public interface BillingSourceHandler {

	void sendBillingRecords(String municipalityId, String externalId, LocalDate scheduledDate, Consumer<String> unhealthyMessageConsumer);
}
