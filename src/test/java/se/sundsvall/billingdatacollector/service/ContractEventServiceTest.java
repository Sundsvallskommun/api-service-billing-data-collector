package se.sundsvall.billingdatacollector.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.Invoicing;
import generated.se.sundsvall.contract.Status;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.EventRequest;
import se.sundsvall.billingdatacollector.api.model.EventType;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractEventServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String CONTRACT_ID = "2026-00001";
	// Fixed clock so calculateStartFrom's "today" is deterministic.
	private static final LocalDate TODAY = LocalDate.of(2026, 4, 29);
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-29T10:00:00Z"), ZoneOffset.UTC);

	@Mock
	private ScheduledBillingService mockScheduledBillingService;

	@Mock
	private ContractIntegration mockContractIntegration;

	private ContractEventService service;

	@BeforeEach
	void setUp() {
		service = new ContractEventService(mockScheduledBillingService, mockContractIntegration, FIXED_CLOCK);
	}

	// ========== CREATED — initial nextScheduledBilling matches the spec ==========

	@SuppressWarnings("unchecked")
	@ParameterizedTest(name = "{0}")
	@MethodSource("initialNextScheduledBillingCases")
	void handleEvent_created_initialNextScheduledBilling(String name, IntervalType interval, InvoicedIn invoicedIn,
		Integer yearlyBillingMonth, LocalDate startDate, Set<Integer> expectedMonths, LocalDate expectedNext) {

		var contract = buildContract(Status.ACTIVE, interval, invoicedIn, startDate, null, yearlyBillingMonth);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		var supplierCaptor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(expectedMonths), eq(Set.of(1)),
			eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.valueOf(invoicedIn.name())),
			supplierCaptor.capture());

		assertThat(supplierCaptor.getValue().get())
			.as("startFrom supplier value for %s", name)
			.isEqualTo(expectedNext);
	}

	static Stream<Arguments> initialNextScheduledBillingCases() {
		return Stream.of(
			// A1 — ADVANCE Yearly + future startDate 2026-05-13 → next December slot
			Arguments.of("A1", IntervalType.YEARLY, InvoicedIn.ADVANCE, 12,
				LocalDate.of(2026, 5, 13), Set.of(12), LocalDate.of(2026, 12, 1)),
			// A2 — ADVANCE Quarterly + future startDate 2026-05-13 → next quarterly slot from startDate
			Arguments.of("A2", IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null,
				LocalDate.of(2026, 5, 13), Set.of(3, 6, 9, 12), LocalDate.of(2026, 6, 1)),
			// A3 — ADVANCE Quarterly + startDate in past → clamped to today, then next quarterly slot
			Arguments.of("A3", IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null,
				LocalDate.of(2026, 1, 1), Set.of(3, 6, 9, 12), LocalDate.of(2026, 6, 1)),
			// A4 — ADVANCE Yearly + startDate = today → must return 2026-12-01, NOT today
			// Regression: before the fix, today's date was stored raw and triggered an immediate spurious billing.
			Arguments.of("A4 regression startDate=today", IntervalType.YEARLY, InvoicedIn.ADVANCE, 12,
				TODAY, Set.of(12), LocalDate.of(2026, 12, 1)),
			// A5 — ADVANCE Yearly + startDate on period start 2027-01-01 → 2026-12-01 (bills Jan–Dec 2027), NOT 2027-12-01
			Arguments.of("A5 ADVANCE Yearly startDate on period start", IntervalType.YEARLY, InvoicedIn.ADVANCE, 12,
				LocalDate.of(2027, 1, 1), Set.of(12), LocalDate.of(2026, 12, 1)),
			// A6 — ADVANCE Quarterly + startDate on quarter start 2026-07-01 → 2026-06-01 (bills Q3 2026)
			Arguments.of("A6 ADVANCE Quarterly startDate on period start", IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null,
				LocalDate.of(2026, 7, 1), Set.of(3, 6, 9, 12), LocalDate.of(2026, 6, 1)),
			// A7 — ADVANCE Monthly + startDate on month start 2026-06-01 → 2026-05-01 (bills June 2026)
			Arguments.of("A7 ADVANCE Monthly startDate on period start", IntervalType.MONTHLY, InvoicedIn.ADVANCE, null,
				LocalDate.of(2026, 6, 1), Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), LocalDate.of(2026, 5, 1)),
			// A8 — ADVANCE Yearly + startDate mid-period 2027-03-15 → partial 2027 is not billed, first slot 2027-12-01
			Arguments.of("A8 ADVANCE Yearly startDate mid-period", IntervalType.YEARLY, InvoicedIn.ADVANCE, 12,
				LocalDate.of(2027, 3, 15), Set.of(12), LocalDate.of(2027, 12, 1)),
			// A9 — ADVANCE Monthly + startDate next month start 2026-05-01 → slot 2026-04-01 already passed, clamped to today →
			// 2026-05-01
			Arguments.of("A9 ADVANCE Monthly first slot passed", IntervalType.MONTHLY, InvoicedIn.ADVANCE, null,
				LocalDate.of(2026, 5, 1), Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), LocalDate.of(2026, 5, 1)),
			// AJ1 — ADVANCE Yearly June + startDate on period start 2026-07-01 → 2026-06-01 (bills Jul 2026–Jun 2027)
			Arguments.of("AJ1 ADVANCE Yearly June startDate on period start", IntervalType.YEARLY, InvoicedIn.ADVANCE, 6,
				LocalDate.of(2026, 7, 1), Set.of(6), LocalDate.of(2026, 6, 1)),
			// AJ2 — ADVANCE Yearly June + startDate 2027-01-01 mid-period → partial Jan–Jun 2027 is not billed, first slot
			// 2027-06-01
			Arguments.of("AJ2 ADVANCE Yearly June startDate mid-period", IntervalType.YEARLY, InvoicedIn.ADVANCE, 6,
				LocalDate.of(2027, 1, 1), Set.of(6), LocalDate.of(2027, 6, 1)),
			// AJ3 — ADVANCE Yearly June + startDate 2026-05-13 → partial May–Jun 2026 is not billed, first slot 2026-06-01
			Arguments.of("AJ3 ADVANCE Yearly June startDate just before slot", IntervalType.YEARLY, InvoicedIn.ADVANCE, 6,
				LocalDate.of(2026, 5, 13), Set.of(6), LocalDate.of(2026, 6, 1)),
			// R1 — ARREARS Yearly + 2026-05-13 → partial 2026 is not billed, first slot 2027-12-01
			Arguments.of("R1", IntervalType.YEARLY, InvoicedIn.ARREARS, 12,
				LocalDate.of(2026, 5, 13), Set.of(12), LocalDate.of(2027, 12, 1)),
			// R2 — ARREARS Quarterly + 2026-05-13 → partial Q2 is not billed, first slot 2026-09-01
			Arguments.of("R2", IntervalType.QUARTERLY, InvoicedIn.ARREARS, null,
				LocalDate.of(2026, 5, 13), Set.of(3, 6, 9, 12), LocalDate.of(2026, 9, 1)),
			// R3 — ARREARS Quarterly + startDate 2026-01-01 in past → slot 2026-03-01 already passed, first slot from today
			// 2026-06-01
			Arguments.of("R3", IntervalType.QUARTERLY, InvoicedIn.ARREARS, null,
				LocalDate.of(2026, 1, 1), Set.of(3, 6, 9, 12), LocalDate.of(2026, 6, 1)),
			// R4 — ARREARS Yearly + startDate on period start 2027-01-01 → 2027-12-01 (bills Jan–Dec 2027), NOT 2028-12-01
			Arguments.of("R4 ARREARS Yearly startDate on period start", IntervalType.YEARLY, InvoicedIn.ARREARS, 12,
				LocalDate.of(2027, 1, 1), Set.of(12), LocalDate.of(2027, 12, 1)),
			// R5 — ARREARS Quarterly + startDate on quarter start 2026-07-01 → 2026-09-01 (bills Q3 2026)
			Arguments.of("R5 ARREARS Quarterly startDate on period start", IntervalType.QUARTERLY, InvoicedIn.ARREARS, null,
				LocalDate.of(2026, 7, 1), Set.of(3, 6, 9, 12), LocalDate.of(2026, 9, 1)),
			// R6 — ARREARS Monthly + startDate on month start 2026-06-01 → 2026-06-01 (bills June 2026)
			Arguments.of("R6 ARREARS Monthly startDate on period start", IntervalType.MONTHLY, InvoicedIn.ARREARS, null,
				LocalDate.of(2026, 6, 1), Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), LocalDate.of(2026, 6, 1)),
			// R7 — ARREARS Monthly + startDate mid-month 2026-06-15 → partial June is not billed, first slot 2026-07-01
			Arguments.of("R7 ARREARS Monthly startDate mid-period", IntervalType.MONTHLY, InvoicedIn.ARREARS, null,
				LocalDate.of(2026, 6, 15), Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), LocalDate.of(2026, 7, 1)),
			// R8 — ARREARS Quarterly + startDate long in the past 2025-05-13 → never a passed slot, first slot from today
			// 2026-06-01
			Arguments.of("R8 ARREARS Quarterly startDate long in past", IntervalType.QUARTERLY, InvoicedIn.ARREARS, null,
				LocalDate.of(2025, 5, 13), Set.of(3, 6, 9, 12), LocalDate.of(2026, 6, 1)),
			// RJ1 — ARREARS Yearly June + startDate on period start 2026-07-01 → 2027-06-01 (bills Jul 2026–Jun 2027)
			Arguments.of("RJ1 ARREARS Yearly June startDate on period start", IntervalType.YEARLY, InvoicedIn.ARREARS, 6,
				LocalDate.of(2026, 7, 1), Set.of(6), LocalDate.of(2027, 6, 1)),
			// RJ2 — ARREARS Yearly June + startDate 2027-01-01 mid-period → partial Jan–Jun 2027 is not billed, first slot
			// 2028-06-01
			Arguments.of("RJ2 ARREARS Yearly June startDate mid-period", IntervalType.YEARLY, InvoicedIn.ARREARS, 6,
				LocalDate.of(2027, 1, 1), Set.of(6), LocalDate.of(2028, 6, 1)),
			// RJ3 — ARREARS Yearly June + startDate 2025-07-01 in past → first slot from today 2026-06-01 (bills Jul 2025–Jun 2026)
			Arguments.of("RJ3 ARREARS Yearly June startDate in past", IntervalType.YEARLY, InvoicedIn.ARREARS, 6,
				LocalDate.of(2025, 7, 1), Set.of(6), LocalDate.of(2026, 6, 1)));
	}

	// ========== Passed slots — warning & recompute on UPDATED ==========

	@SuppressWarnings("unchecked")
	@Test
	void handleEvent_created_whenFirstSlotHasPassed_shouldLogMissedPeriod() {
		// ADVANCE Quarterly from 2026-01-01: Q1 and Q2 slots (2025-12-01, 2026-03-01) have passed on 2026-04-29
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, LocalDate.of(2026, 1, 1), null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		var appender = attachLogAppender();
		try {
			service.handleEvent(eventRequest(EventType.CREATED));

			var supplierCaptor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
			verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
				any(), any(), any(), supplierCaptor.capture());

			assertThat(supplierCaptor.getValue().get()).isEqualTo(LocalDate.of(2026, 6, 1));
			assertThat(appender.list)
				.filteredOn(event -> event.getLevel() == Level.WARN)
				.extracting(ILoggingEvent::getFormattedMessage)
				.singleElement()
				.asString()
				.contains("period 2026-01-01 - 2026-06-30 is not billed automatically");
		} finally {
			detachLogAppender(appender);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void handleEvent_created_whenFirstSlotIsInFuture_shouldNotLogMissedPeriod() {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, InvoicedIn.ADVANCE, LocalDate.of(2027, 1, 1), null, 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		var appender = attachLogAppender();
		try {
			service.handleEvent(eventRequest(EventType.CREATED));

			var supplierCaptor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
			verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
				any(), any(), any(), supplierCaptor.capture());

			assertThat(supplierCaptor.getValue().get()).isEqualTo(LocalDate.of(2026, 12, 1));
			assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.WARN);
		} finally {
			detachLogAppender(appender);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	void handleEvent_updated_recomputeForOldContract_shouldNeverScheduleAPassedSlot() {
		// The supplier is what ScheduledBillingService.upsert evaluates when the
		// cadence or direction of an existing row changes. For a contract that
		// started long ago it must start from today, or the scheduler would
		// back-bill every period since the start one tick at a time.
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ARREARS, LocalDate.of(2020, 1, 15), null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		var supplierCaptor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(Set.of(3, 6, 9, 12)), eq(Set.of(1)), eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.ARREARS),
			supplierCaptor.capture());

		// First slot on or after 2026-04-29 is 2026-06-01, billing Q2 2026
		assertThat(supplierCaptor.getValue().get()).isEqualTo(LocalDate.of(2026, 6, 1));
	}

	@SuppressWarnings("unchecked")
	@Test
	void handleEvent_created_whenInvoicedInIsNull_shouldDefaultToArrears() {
		// Yearly from 2027-01-01: ARREARS gives 2027-12-01, ADVANCE would give 2026-12-01
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, null, LocalDate.of(2027, 1, 1), null, 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		var supplierCaptor = ArgumentCaptor.forClass(java.util.function.Supplier.class);
		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(Set.of(12)), eq(Set.of(1)), eq(null), supplierCaptor.capture());

		assertThat(supplierCaptor.getValue().get()).isEqualTo(LocalDate.of(2027, 12, 1));
	}

	private static ListAppender<ILoggingEvent> attachLogAppender() {
		var appender = new ListAppender<ILoggingEvent>();
		appender.start();
		((Logger) LoggerFactory.getLogger(ContractEventService.class)).addAppender(appender);
		return appender;
	}

	private static void detachLogAppender(ListAppender<ILoggingEvent> appender) {
		((Logger) LoggerFactory.getLogger(ContractEventService.class)).detachAppender(appender);
		appender.stop();
	}

	// ========== CREATED — basic flow & non-billable ==========

	@Test
	void handleEvent_created_whenNotActive_shouldDoNothing() {
		var contract = buildContract(Status.TERMINATED, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, LocalDate.of(2026, 5, 13), null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verifyNoInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_created_whenContractNotFound_shouldDoNothing() {
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.CREATED));

		verifyNoInteractions(mockScheduledBillingService);
	}

	// ========== UPDATED ==========

	@Test
	void handleEvent_updated_whenBillable_shouldUpsert() {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, InvoicedIn.ADVANCE, LocalDate.of(2026, 5, 13), null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(Set.of(12)), eq(Set.of(1)), eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.ADVANCE),
			any());
	}

	@Test
	void handleEvent_updated_whenNotBillable_shouldDelete() {
		var contract = buildContract(Status.TERMINATED, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService, never()).upsert(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void handleEvent_updated_whenContractNotFound_shouldDelete() {
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
	}

	// ========== DELETED ==========

	@Test
	void handleEvent_deleted_shouldDeleteWithoutFetchingContract() {
		service.handleEvent(eventRequest(EventType.DELETED));

		verifyNoInteractions(mockContractIntegration);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	// ========== TERMINATED ==========

	@Test
	void handleEvent_terminated_whenContractFoundWithEndDate_shouldApplyEndDateLogic() {
		// Yearly ADVANCE, nextSched=Dec 1 2026, endDate=Mar 14 2027 → period Jan-Dec 2027
		// extends past endDate → DELETE.
		var contract = buildContract(Status.TERMINATED, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), LocalDate.of(2027, 3, 14), null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(LocalDate.of(2026, 12, 1)));

		service.handleEvent(eventRequest(EventType.TERMINATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
	}

	@Test
	void handleEvent_terminated_withoutEndDate_shouldDelete() {
		var contract = buildContract(Status.TERMINATED, IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.TERMINATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
	}

	@Test
	void handleEvent_terminated_whenContractNotFound_shouldDelete() {
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.TERMINATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
	}

	// ========== applyEndDateLogic — spec verification (A4/A5/R4/R5) ==========

	@ParameterizedTest(name = "{0}")
	@MethodSource("applyEndDateLogicCases")
	void handleEvent_applyEndDateLogic(String name, IntervalType interval, InvoicedIn invoicedIn,
		LocalDate nextSched, LocalDate endDate, boolean expectDelete) {

		var contract = buildContract(Status.ACTIVE, interval, invoicedIn, LocalDate.of(2026, 1, 1), endDate, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(nextSched));

		service.handleEvent(eventRequest(EventType.UPDATED));

		if (expectDelete) {
			verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		} else {
			verify(mockScheduledBillingService, never())
				.deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		}
	}

	static Stream<Arguments> applyEndDateLogicCases() {
		return Stream.of(
			// A4 — ADVANCE Yearly + nextSched 2026-12-01 + endDate 2027-03-14:
			// period Jan-Dec 2027 extends past 2027-03-14 → DELETE
			Arguments.of("A4 ADVANCE Yearly", IntervalType.YEARLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 3, 14), true),
			// A5 — ADVANCE Quarterly + nextSched 2026-06-01 + endDate 2027-03-14:
			// period Q3 (Jul-Sep 2026) fits → KEEP. Subsequent slot decisions
			// happen at scheduler time (covered by ContractBillingHandlerTest).
			Arguments.of("A5 ADVANCE Quarterly fits", IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 6, 1), LocalDate.of(2027, 3, 14), false),
			// R4 — ARREARS Yearly + nextSched 2026-12-01 + endDate 2027-03-14:
			// period Jan-Dec 2026 fits → KEEP.
			Arguments.of("R4 ARREARS Yearly fits", IntervalType.YEARLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 12, 1), LocalDate.of(2027, 3, 14), false),
			// R5 — ARREARS Quarterly + nextSched 2026-06-01 + endDate 2026-04-14:
			// period Q2 (Apr-Jun 2026) extends past 2026-04-14 → DELETE
			Arguments.of("R5 ARREARS Quarterly", IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 4, 14), true));
	}

	// ========== applyEndDateLogic — edge cases ==========

	@Test
	void handleEvent_nullEndDate_doesNotDelete() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService, never())
			.deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
	}

	@Test
	void handleEvent_noScheduledBillingFound_doesNothingForEndDate() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), LocalDate.of(2027, 3, 14), null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService, never())
			.deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
	}

	// ========== calculateBillingMonths — yearlyBillingMonth picks the slot ==========

	@ParameterizedTest(name = "{0}")
	@MethodSource("yearlyBillingMonthCases")
	void handleEvent_yearly_yearlyBillingMonth_picksSlot(String name, Integer yearlyBillingMonth, Set<Integer> expectedMonths) {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), null, yearlyBillingMonth);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(expectedMonths), eq(Set.of(1)), eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.ADVANCE), any());
	}

	static Stream<Arguments> yearlyBillingMonthCases() {
		return Stream.of(
			Arguments.of("yearlyBillingMonth=6 uses the June slot", 6, Set.of(6)),
			Arguments.of("yearlyBillingMonth=12 uses the December slot", 12, Set.of(12)),
			Arguments.of("yearlyBillingMonth=null defaults to the December slot", null, Set.of(12)),
			Arguments.of("yearlyBillingMonth=3 (unexpected) defaults to the December slot", 3, Set.of(12)));
	}

	// ========== billingEnabled overrides every other rule ==========

	@Test
	void handleEvent_created_whenBillingDisabled_shouldDoNothing() {
		var contract = buildContract(Status.ACTIVE, Boolean.FALSE, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 5, 13), null, 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verifyNoInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_updated_whenBillingDisabled_shouldDelete() {
		var contract = buildContract(Status.ACTIVE, Boolean.FALSE, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 5, 13), null, 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService, never()).upsert(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void handleEvent_created_whenBillingEnabledMissing_shouldStillUpsert() {
		// Regression: a null billingEnabled (e.g. source data not yet backfilled)
		// must NOT be treated as disabled — it defaults to enabled.
		var contract = buildContract(Status.ACTIVE, null, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 5, 13), null, 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(Set.of(12)), eq(Set.of(1)), eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.ADVANCE), any());
	}

	@Test
	void handleEvent_updated_whenBillingEnabledMissing_shouldNotDelete() {
		// Regression: previously, a missing billingEnabled was treated as "not
		// billable" and the existing schedule got deleted with nothing to
		// recreate it until a new CREATED/UPDATED event arrived.
		var contract = buildContract(Status.ACTIVE, null, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 5, 13), null, 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockScheduledBillingService, never()).deleteByExternalId(any(), any(), any());
		verify(mockScheduledBillingService).upsert(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void handleEvent_terminated_whenBillingDisabled_shouldDeleteWithoutCheckingEndDate() {
		// billingEnabled = false overrides every other rule, including an
		// end date that would otherwise still require a final billing.
		var contract = buildContract(Status.TERMINATED, Boolean.FALSE, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), LocalDate.of(2030, 12, 31), 12);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.TERMINATED));

		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService, never()).getNextScheduledBilling(any(), any(), any());
	}

	// ========== helpers ==========

	private EventRequest eventRequest(EventType eventType) {
		return EventRequest.builder()
			.withId(CONTRACT_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withEventType(eventType)
			.build();
	}

	private Contract buildContract(Status status, IntervalType intervalType, InvoicedIn invoicedIn,
		LocalDate startDate, LocalDate endDate, Integer yearlyBillingMonth) {
		return buildContract(status, Boolean.TRUE, intervalType, invoicedIn, startDate, endDate, yearlyBillingMonth);
	}

	private Contract buildContract(Status status, Boolean billingEnabled, IntervalType intervalType, InvoicedIn invoicedIn,
		LocalDate startDate, LocalDate endDate, Integer yearlyBillingMonth) {
		var contract = new Contract();
		contract.setStatus(status);
		contract.setStartDate(startDate);
		contract.setEndDate(endDate);

		if (intervalType != null || invoicedIn != null) {
			var invoicing = new Invoicing();
			invoicing.setBillingEnabled(billingEnabled);
			invoicing.setInvoiceInterval(intervalType);
			invoicing.setInvoicedIn(invoicedIn);
			invoicing.setYearlyBillingMonth(yearlyBillingMonth);
			contract.setInvoicing(invoicing);
		}

		return contract;
	}
}
