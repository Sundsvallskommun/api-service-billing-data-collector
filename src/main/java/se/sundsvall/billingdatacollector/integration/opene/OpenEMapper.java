package se.sundsvall.billingdatacollector.integration.opene;

import se.sundsvall.billingdatacollector.model.dto.BillingRecordDto;

public interface OpenEMapper {

    String getSupportedFamilyId();

    BillingRecordDto mapToBillingRecord(byte[] xml);
}
