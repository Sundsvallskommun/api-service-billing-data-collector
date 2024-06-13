package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@ExtendWith(MockitoExtension.class)
class BillingPreprocessorIntegrationTest {

	@Mock
	private BillingPreprocessorClient mockClient;

	@Mock
	private ResponseEntity<Void> mockResponseEntity;

	@InjectMocks
	private BillingPreprocessorIntegration bppIntegration;

	@Test
	void createBillingRecord() {
		//Arrange
		when(mockClient.createBillingRecord(any(BillingRecord.class))).thenReturn(mockResponseEntity);
		var billingRecord = new BillingRecord();

		//Act
		bppIntegration.createBillingRecord(billingRecord);

		//Assert
		verify(mockClient).createBillingRecord(billingRecord);
		verifyNoMoreInteractions(mockClient);
	}
}
