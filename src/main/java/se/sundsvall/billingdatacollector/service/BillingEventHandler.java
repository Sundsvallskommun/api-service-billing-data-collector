package se.sundsvall.billingdatacollector.service;

import se.sundsvall.billingdatacollector.api.model.EventRequest;

public interface BillingEventHandler {

	void handleEvent(EventRequest request);
}
