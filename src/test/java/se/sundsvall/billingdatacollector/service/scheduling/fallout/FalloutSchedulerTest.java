package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FalloutSchedulerTest {

	@Mock
	private FalloutJobHandler mockFalloutJobHandler;

	@InjectMocks
	private FalloutScheduler falloutScheduler;

	// Test that the FalloutScheduler runs the FalloutJobHandler
	@Test
	void testRunFalloutJob() {
		// Arrange
		doNothing().when(mockFalloutJobHandler).handleFallout();

		// Act
		falloutScheduler.runFalloutJob();

		// Assert
		verify(mockFalloutJobHandler).handleFallout();
	}
}
