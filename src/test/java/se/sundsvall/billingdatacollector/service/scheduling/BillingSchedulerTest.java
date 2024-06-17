package se.sundsvall.billingdatacollector.service.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

	@Mock
	private BillingJobHandler mockBillingJobHandler;

	@InjectMocks
	private BillingScheduler billingScheduler;

	@Test
	void testRunBillingJob() {
		// Arrange
		Mockito.doNothing().when(mockBillingJobHandler).handleBilling();

		// Act
		billingScheduler.runBillingJob();

		// Assert
		verify(mockBillingJobHandler).handleBilling();
		verifyNoMoreInteractions(mockBillingJobHandler);
	}
}
