package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.Invoicing;
import generated.se.sundsvall.contract.LeaseType;
import generated.se.sundsvall.contract.Period;
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
		LocalDate startDate, Set<Integer> expectedMonths, LocalDate expectedNext) {

		var contract = buildContract(Status.ACTIVE, interval, invoicedIn, startDate, null, null);
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
			// A1 — ADVANCE Yearly + 2026-05-13 → 2026-12-01 (covers Jan-Dec 2027)
			Arguments.of("A1", IntervalType.YEARLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 5, 13), Set.of(12), LocalDate.of(2026, 5, 13)),
			// A2 — ADVANCE Quarterly + 2026-05-13 → first slot from startDate
			Arguments.of("A2", IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 5, 13), Set.of(3, 6, 9, 12), LocalDate.of(2026, 5, 13)),
			// A3 — ADVANCE Quarterly + startDate in past → clamped to today (2026-04-29)
			Arguments.of("A3", IntervalType.QUARTERLY, InvoicedIn.ADVANCE,
				LocalDate.of(2026, 1, 1), Set.of(3, 6, 9, 12), TODAY),
			// R1 — ARREARS Yearly + 2026-05-13 → skip the first slot from startDate
			Arguments.of("R1", IntervalType.YEARLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 5, 13), Set.of(12), LocalDate.of(2027, 12, 1)),
			// R2 — ARREARS Quarterly + 2026-05-13 → skip-one-from-startDate
			Arguments.of("R2", IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 5, 13), Set.of(3, 6, 9, 12), LocalDate.of(2026, 9, 1)),
			// R3 — ARREARS Quarterly + startDate in past → still skip-one-from-startDate (not from today)
			Arguments.of("R3", IntervalType.QUARTERLY, InvoicedIn.ARREARS,
				LocalDate.of(2026, 1, 1), Set.of(3, 6, 9, 12), LocalDate.of(2026, 6, 1)));
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

	// ========== calculateBillingMonths ==========

	@Test
	void handleEvent_yearly_landLeaseResidentialEndOfJune_usesJuneSlot() {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 6, 30));
		contract.setLeaseType(LeaseType.LAND_LEASE_RESIDENTIAL);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(Set.of(6)), eq(Set.of(1)), eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.ADVANCE), any());
	}

	@Test
	void handleEvent_yearly_landLeaseMisc_usesDecemberSlot() {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, InvoicedIn.ADVANCE,
			LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 6, 30));
		contract.setLeaseType(LeaseType.LAND_LEASE_MISC);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(eq(MUNICIPALITY_ID), eq(CONTRACT_ID), eq(BillingSource.CONTRACT),
			eq(Set.of(12)), eq(Set.of(1)), eq(se.sundsvall.billingdatacollector.api.model.InvoicedIn.ADVANCE), any());
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
		LocalDate startDate, LocalDate endDate, LocalDate currentPeriodEndDate) {
		var contract = new Contract();
		contract.setStatus(status);
		contract.setStartDate(startDate);
		contract.setEndDate(endDate);

		if (intervalType != null || invoicedIn != null) {
			var invoicing = new Invoicing();
			invoicing.setInvoiceInterval(intervalType);
			invoicing.setInvoicedIn(invoicedIn);
			contract.setInvoicing(invoicing);
		}

		if (currentPeriodEndDate != null) {
			var period = new Period();
			period.setEndDate(currentPeriodEndDate);
			contract.setCurrentPeriod(period);
		}

		return contract;
	}
}
