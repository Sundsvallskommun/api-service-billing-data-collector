package se.sundsvall.billingdatacollector.service.scheduling.billing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

	@Mock
	private BillingJobHandler mockBillingJobHandler;

	@InjectMocks
	private BillingScheduler billingScheduler;

	@Test
	void testRunBillingJob() {
		// Arrange
		doNothing().when(mockBillingJobHandler).performBilling();

		// Act
		billingScheduler.runBillingJob();

		// Assert
		verify(mockBillingJobHandler).performBilling();
		verifyNoMoreInteractions(mockBillingJobHandler);
	}
}
