package se.sundsvall.billingdatacollector.service.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.billingdatacollector.service.ScheduledBillingService;
import se.sundsvall.billingdatacollector.service.source.BillingSourceHandler;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

	private static final String JOB_NAME = "billing-scheduler";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXTERNAL_ID = "test-external-id";

	@Mock
	private Dept44HealthUtility mockDept44HealthUtility;

	@Mock
	private ScheduledBillingService mockScheduledBillingService;

	@Mock
	private BillingSourceHandler mockContractHandler;

	private BillingScheduler billingScheduler;

	@BeforeEach
	void setUp() {
		Map<String, BillingSourceHandler> handlerMap = Map.of("CONTRACT", mockContractHandler);
		billingScheduler = new BillingScheduler(mockDept44HealthUtility, mockScheduledBillingService, handlerMap);
		ReflectionTestUtils.setField(billingScheduler, "jobName", JOB_NAME);
	}

	@Test
	void createBillingRecords_shouldProcessDueBillings_whenHandlerExists() {
		// Arrange
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		doNothing().when(mockContractHandler).sendBillingRecords(MUNICIPALITY_ID, EXTERNAL_ID);
		doNothing().when(mockScheduledBillingService).updateNextScheduledBilling(entity);

		// Act
		billingScheduler.createBillingRecords();

		// Assert
		verify(mockScheduledBillingService).getDueScheduledBillings();
		verify(mockContractHandler).sendBillingRecords(MUNICIPALITY_ID, EXTERNAL_ID);
		verify(mockScheduledBillingService).updateNextScheduledBilling(entity);
		verifyNoInteractions(mockDept44HealthUtility);
		verifyNoMoreInteractions(mockScheduledBillingService, mockContractHandler);
	}

	@Test
	void createBillingRecords_shouldSetUnhealthy_whenNoHandlerFound() {
		// Arrange
		var entity = createScheduledBillingEntity(BillingSource.OPENE);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));

		// Act
		billingScheduler.createBillingRecords();

		// Assert
		verify(mockScheduledBillingService).getDueScheduledBillings();
		verify(mockDept44HealthUtility).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verify(mockScheduledBillingService, never()).updateNextScheduledBilling(any());
		verifyNoInteractions(mockContractHandler);
		verifyNoMoreInteractions(mockScheduledBillingService, mockDept44HealthUtility);
	}

	@Test
	void createBillingRecords_shouldSetUnhealthy_whenHandlerThrowsException() {
		// Arrange
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		doThrow(new RuntimeException("Test exception")).when(mockContractHandler).sendBillingRecords(MUNICIPALITY_ID, EXTERNAL_ID);

		// Act
		billingScheduler.createBillingRecords();

		// Assert
		verify(mockScheduledBillingService).getDueScheduledBillings();
		verify(mockContractHandler).sendBillingRecords(MUNICIPALITY_ID, EXTERNAL_ID);
		verify(mockDept44HealthUtility).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verify(mockScheduledBillingService, never()).updateNextScheduledBilling(any());
		verifyNoMoreInteractions(mockScheduledBillingService, mockContractHandler, mockDept44HealthUtility);
	}

	@Test
	void createBillingRecords_shouldDoNothing_whenNoDueBillings() {
		// Arrange
		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(Collections.emptyList());

		// Act
		billingScheduler.createBillingRecords();

		// Assert
		verify(mockScheduledBillingService).getDueScheduledBillings();
		verifyNoInteractions(mockDept44HealthUtility, mockContractHandler);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	private ScheduledBillingEntity createScheduledBillingEntity(BillingSource source) {
		return ScheduledBillingEntity.builder()
			.withId("test-id")
			.withMunicipalityId(MUNICIPALITY_ID)
			.withExternalId(EXTERNAL_ID)
			.withSource(source)
			.withBillingDaysOfMonth(Set.of(1))
			.withBillingMonths(Set.of(1))
			.withPaused(false)
			.build();
	}
}
