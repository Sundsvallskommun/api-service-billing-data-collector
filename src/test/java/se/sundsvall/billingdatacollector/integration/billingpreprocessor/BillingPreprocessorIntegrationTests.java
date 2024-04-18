package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.billingdatacollector.model.dto.BillingRecordDto;

@ExtendWith(MockitoExtension.class)
class BillingPreprocessorIntegrationTests {

    @Mock
    private BillingPreprocessorClient mockPartyClient;

    @InjectMocks
    private BillingPreprocessorIntegration bppIntegration;

    @Test
    void createBillingRecord() {
        var billingRecordDto = new BillingRecordDto();

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> bppIntegration.createBillingRecord(billingRecordDto))
            .withMessage("NOT IMPLEMENTED YET");
    }
}
