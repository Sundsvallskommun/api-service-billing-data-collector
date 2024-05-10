package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@ExtendWith(MockitoExtension.class)
class BillingPreprocessorIntegrationTest {

    @Mock
    private BillingPreprocessorClient mockPartyClient;

    @InjectMocks
    private BillingPreprocessorIntegration bppIntegration;

    @Test
    void createBillingRecord() {
        var billingRecord = new BillingRecord();

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> bppIntegration.createBillingRecord(billingRecord))
            .withMessage("NOT IMPLEMENTED YET");
    }
}
