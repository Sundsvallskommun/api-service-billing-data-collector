package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.model.dto.BillingRecordDto;

@Component
public class BillingPreprocessorIntegration {

    private final BillingPreprocessorClient client;

    BillingPreprocessorIntegration(final BillingPreprocessorClient client) {
        this.client = client;
    }

    public void createBillingRecord(final BillingRecordDto billingRecordDto) {
        // ...c'mon...do stuff...
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
}
