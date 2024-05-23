package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import reactor.core.publisher.Sinks;

@ExtendWith(MockitoExtension.class)
class BillingPreprocessorIntegrationTest {

	@Mock
	private BillingPreprocessorClient mockClient;

    @InjectMocks
    private BillingPreprocessorIntegration bppIntegration;

    @Test
    void createBillingRecord() {
		//Arrange
		doNothing().when(mockClient).createBillingRecord(any(BillingRecord.class));
        var billingRecord = new BillingRecord();

		//Act
		bppIntegration.createBillingRecord(billingRecord);

		//Assert
		verify(mockClient).createBillingRecord(billingRecord);
		verifyNoMoreInteractions(mockClient);
    }
}
