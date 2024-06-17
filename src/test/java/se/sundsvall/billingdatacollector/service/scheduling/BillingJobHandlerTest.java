package se.sundsvall.billingdatacollector.service.scheduling;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.billingdatacollector.TestDataFactory.createHistoryEntity;
import static se.sundsvall.billingdatacollector.TestDataFactory.createScheduledJobEntity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.billingdatacollector.service.DbService;

@ExtendWith(MockitoExtension.class)
class BillingJobHandlerTest {

	@Mock
	private CollectorService mockCollectorService;

	@Mock
	private DbService mockDbService;

	@InjectMocks
	private BillingJobHandler billingJobHandler;

	@Test
	void testHandleBilling() {
		// Arrange
		var scheduledJobEntity = createScheduledJobEntity();
		when(mockDbService.getLatestJob()).thenReturn(Optional.of(scheduledJobEntity));
		doNothing().when(mockDbService).saveScheduledJob(Mockito.any(), Mockito.any());
		when(mockCollectorService.triggerBillingBetweenDates(Mockito.any(), Mockito.any(), anySet())).thenReturn(List.of("1", "2"));
		when(mockDbService.getHistory(anyList())).thenReturn(List.of(createHistoryEntity("1"), createHistoryEntity("2")));
		when(mockDbService.getFallouts(anyList())).thenReturn(Collections.emptyList());

		// Act
		billingJobHandler.handleBilling();

		// Assert
		verify(mockDbService).getLatestJob();
		//We want to make sure that the job fetches with a startDate == 3 days ago, and endDate == yesterday
		verify(mockDbService).saveScheduledJob(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
		verify(mockCollectorService).triggerBillingBetweenDates(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), Collections.emptySet());
		verify(mockDbService).getHistory(List.of("1", "2"));
		verify(mockDbService).getFallouts(List.of("1", "2"));
		verifyNoMoreInteractions(mockDbService, mockCollectorService);
	}

	@Test
	void testHandleBilling_noJobsProcessed() {
		// Arrange
		var scheduledJobEntity = createScheduledJobEntity();
		when(mockDbService.getLatestJob()).thenReturn(Optional.of(scheduledJobEntity));
		doNothing().when(mockDbService).saveScheduledJob(Mockito.any(), Mockito.any());
		when(mockCollectorService.triggerBillingBetweenDates(Mockito.any(), Mockito.any(), anySet())).thenReturn(Collections.emptyList());

		// Act
		billingJobHandler.handleBilling();

		// Assert
		verify(mockDbService).getLatestJob();
		verify(mockDbService).saveScheduledJob(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
		verify(mockCollectorService).triggerBillingBetweenDates(LocalDate.now().minusDays(3), LocalDate.now().minusDays(1), Collections.emptySet());
		verify(mockDbService, Mockito.never()).getHistory(Mockito.any());
		verify(mockDbService, Mockito.never()).getFallouts(Mockito.any());
		verifyNoMoreInteractions(mockDbService, mockCollectorService);
	}
}
