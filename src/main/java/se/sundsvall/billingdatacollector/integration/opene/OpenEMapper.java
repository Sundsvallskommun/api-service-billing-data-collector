package se.sundsvall.billingdatacollector.integration.opene;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

public interface OpenEMapper {

	String getSupportedFamilyId();

	BillingRecordWrapper mapToBillingRecordWrapper(byte[] xml);
}
