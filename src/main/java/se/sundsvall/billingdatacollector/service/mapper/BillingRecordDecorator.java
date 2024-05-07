package se.sundsvall.billingdatacollector.service.mapper;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

public interface BillingRecordDecorator {
	String getSupportedFamilyId();
	void decorate(BillingRecordWrapper wrapper);
}
