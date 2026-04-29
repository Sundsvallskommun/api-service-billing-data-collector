package se.sundsvall.billingdatacollector.service.scheduling;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
import se.sundsvall.billingdatacollector.service.source.BillingResult;
import se.sundsvall.billingdatacollector.service.source.BillingSourceHandler;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingSchedulerTest {

	private static final String JOB_NAME = "billing-scheduler";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String EXTERNAL_ID = "test-external-id";
	private static final LocalDate NEXT_SCHEDULED_BILLING = LocalDate.of(2026, 6, 1);
	private static final LocalDate NEXT_SLOT = LocalDate.of(2026, 9, 1);

	@Mock
	private Dept44HealthUtility mockDept44HealthUtility;

	@Mock
	private ScheduledBillingService mockScheduledBillingService;

	@Mock
	private BillingSourceHandler mockContractHandler;

	private BillingScheduler billingScheduler;

	@BeforeEach
	void setUp() {
		Map<String, BillingSourceHandler> handlerMap = Map.of("contract", mockContractHandler);
		billingScheduler = new BillingScheduler(mockDept44HealthUtility, mockScheduledBillingService, handlerMap);
		ReflectionTestUtils.setField(billingScheduler, "jobName", JOB_NAME);
	}

	@Test
	void createBillingRecords_whenSentWithNextSlot_advancesEntity() {
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		when(mockContractHandler.sendBillingRecords(entity)).thenReturn(new BillingResult.Sent(NEXT_SLOT));

		billingScheduler.createBillingRecords();

		assertThat(entity.getLastBilled()).isCloseTo(OffsetDateTime.now(), within(2, ChronoUnit.SECONDS));
		assertThat(entity.getNextScheduledBilling()).isEqualTo(NEXT_SLOT);
		verify(mockScheduledBillingService).saveScheduledBillingEntity(entity);
		verify(mockScheduledBillingService, never()).deleteScheduledBillingEntity(any());
		verifyNoInteractions(mockDept44HealthUtility);
	}

	@Test
	void createBillingRecords_whenSentWithNullNextSlot_deletesEntity() {
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		when(mockContractHandler.sendBillingRecords(entity)).thenReturn(new BillingResult.Sent(null));

		billingScheduler.createBillingRecords();

		assertThat(entity.getLastBilled()).isCloseTo(OffsetDateTime.now(), within(2, ChronoUnit.SECONDS));
		verify(mockScheduledBillingService).deleteScheduledBillingEntity(entity);
		verify(mockScheduledBillingService, never()).saveScheduledBillingEntity(any());
		verifyNoInteractions(mockDept44HealthUtility);
	}

	@Test
	void createBillingRecords_whenSkipped_deletesEntityWithoutTouchingLastBilled() {
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);
		var lastBilledBefore = entity.getLastBilled();

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		when(mockContractHandler.sendBillingRecords(entity))
			.thenReturn(new BillingResult.Skipped("period extends past contract end date"));

		billingScheduler.createBillingRecords();

		assertThat(entity.getLastBilled()).isEqualTo(lastBilledBefore);
		verify(mockScheduledBillingService).deleteScheduledBillingEntity(entity);
		verify(mockScheduledBillingService, never()).saveScheduledBillingEntity(any());
		verifyNoInteractions(mockDept44HealthUtility);
	}

	@Test
	void createBillingRecords_whenFailed_marksUnhealthy_andLeavesEntity() {
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		when(mockContractHandler.sendBillingRecords(entity))
			.thenReturn(new BillingResult.Failed("billing-preprocessor unavailable"));

		billingScheduler.createBillingRecords();

		verify(mockDept44HealthUtility).setHealthIndicatorUnhealthy(eq(JOB_NAME),
			contains("billing-preprocessor unavailable"));
		verify(mockScheduledBillingService, never()).deleteScheduledBillingEntity(any());
		verify(mockScheduledBillingService, never()).saveScheduledBillingEntity(any());
	}

	@Test
	void createBillingRecords_shouldSetUnhealthy_whenNoHandlerFound() {
		var entity = createScheduledBillingEntity(BillingSource.OPENE);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));

		billingScheduler.createBillingRecords();

		verify(mockDept44HealthUtility).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verify(mockScheduledBillingService, never()).saveScheduledBillingEntity(any());
		verify(mockScheduledBillingService, never()).deleteScheduledBillingEntity(any());
		verifyNoInteractions(mockContractHandler);
	}

	@Test
	void createBillingRecords_shouldSetUnhealthy_whenHandlerThrowsException() {
		var entity = createScheduledBillingEntity(BillingSource.CONTRACT);

		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(List.of(entity));
		when(mockContractHandler.sendBillingRecords(entity))
			.thenThrow(new RuntimeException("Test exception"));

		billingScheduler.createBillingRecords();

		assertThat(entity.getLastBilled()).isNull();
		verify(mockDept44HealthUtility).setHealthIndicatorUnhealthy(eq(JOB_NAME), any(String.class));
		verify(mockScheduledBillingService, never()).saveScheduledBillingEntity(any());
		verify(mockScheduledBillingService, never()).deleteScheduledBillingEntity(any());
	}

	@Test
	void createBillingRecords_shouldDoNothing_whenNoDueBillings() {
		when(mockScheduledBillingService.getDueScheduledBillings()).thenReturn(Collections.emptyList());

		billingScheduler.createBillingRecords();

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
			.withBillingMonths(Set.of(3, 6, 9, 12))
			.withPaused(false)
			.withNextScheduledBilling(NEXT_SCHEDULED_BILLING)
			.build();
	}
}
