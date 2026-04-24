package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.Invoicing;
import generated.se.sundsvall.contract.LeaseType;
import generated.se.sundsvall.contract.Period;
import generated.se.sundsvall.contract.Status;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.EventRequest;
import se.sundsvall.billingdatacollector.api.model.EventType;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractEventServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String CONTRACT_ID = "2026-00001";
	private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
	private static final LocalDate ARREARS_START_FROM = LocalDate.of(2025, 6, 1); // first slot after first full quarterly period from START_DATE
	private static final LocalDate NEXT_BILLING = LocalDate.of(2026, 3, 1);
	private static final LocalDate END_DATE_AFTER_NEXT = LocalDate.of(2026, 6, 30);
	private static final LocalDate END_DATE_BEFORE_NEXT = LocalDate.of(2026, 1, 31);
	private static final LocalDate END_DATE_ON_NEXT = LocalDate.of(2026, 3, 1);

	@Mock
	private ScheduledBillingService mockScheduledBillingService;

	@Mock
	private ContractIntegration mockContractIntegration;

	@InjectMocks
	private ContractEventService service;

	// ========== CREATED ==========

	@Test
	void handleEvent_created_whenBillable_shouldUpsert() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_created_whenNotActive_shouldDoNothing() {
		var contract = buildContract(Status.TERMINATED, IntervalType.QUARTERLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verifyNoInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_created_whenContractNotFound_shouldDoNothing() {
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verifyNoInteractions(mockScheduledBillingService);
	}

	// ========== UPDATED ==========

	@Test
	void handleEvent_updated_whenBillable_shouldUpsert() {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(12), Set.of(1), LocalDate.now());
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_updated_whenNotBillable_shouldDelete() {
		var contract = buildContract(Status.TERMINATED, IntervalType.QUARTERLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_updated_whenContractNotFound_shouldDelete() {
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.UPDATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
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
	void handleEvent_terminated_whenContractFound_shouldApplyEndDateLogic() {
		var contract = buildContract(Status.TERMINATED, null, InvoicedIn.ADVANCE, END_DATE_AFTER_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(NEXT_BILLING));

		service.handleEvent(eventRequest(EventType.TERMINATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService).updateFinalBillingDate(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, END_DATE_AFTER_NEXT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_terminated_whenContractNotFound_shouldDelete() {
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.TERMINATED));

		verify(mockContractIntegration).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	// ========== calculateBillingMonths ==========

	@Test
	void handleEvent_monthly_shouldUseAllMonths() {
		var contract = buildContract(Status.ACTIVE, IntervalType.MONTHLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(
			MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT,
			Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
			Set.of(1), START_DATE);
	}

	@Test
	void handleEvent_quarterly_shouldUseQuarterlyMonths() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(
			MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT,
			Set.of(3, 6, 9, 12),
			Set.of(1), START_DATE);
	}

	@Test
	void handleEvent_halfYearly_shouldUseHalfYearlyMonths() {
		var contract = buildContract(Status.ACTIVE, IntervalType.HALF_YEARLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(
			MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT,
			Set.of(6, 12),
			Set.of(1), START_DATE);
	}

	@Test
	void handleEvent_yearly_shouldUseDecember() {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, null, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(
			MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT,
			Set.of(12),
			Set.of(1), START_DATE);
	}

	@ParameterizedTest
	@MethodSource("yearlyBillingMonthsProvider")
	void handleEvent_yearly_landLease_shouldCalculateBillingMonths(LeaseType leaseType, LocalDate periodEndDate, Set<Integer> expectedMonths) {
		var contract = buildContract(Status.ACTIVE, IntervalType.YEARLY, null, null, periodEndDate);
		contract.setLeaseType(leaseType);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, expectedMonths, Set.of(1), START_DATE);
	}

	private static Stream<Arguments> yearlyBillingMonthsProvider() {
		return Stream.of(
			Arguments.of(LeaseType.LAND_LEASE_RESIDENTIAL, LocalDate.of(2026, 6, 30), Set.of(6)),
			Arguments.of(LeaseType.LAND_LEASE_MISC, LocalDate.of(2026, 6, 30), Set.of(12)),
			Arguments.of(LeaseType.LAND_LEASE_RESIDENTIAL, LocalDate.of(2026, 12, 31), Set.of(12)),
			Arguments.of(LeaseType.LAND_LEASE_RESIDENTIAL, LocalDate.of(2026, 6, 29), Set.of(12)));
	}

	// ========== applyEndDateLogic ==========

	@Test
	void handleEvent_advance_endDateAfterNextBilling_shouldSetFinalBillingDate() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, END_DATE_AFTER_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(NEXT_BILLING));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService).updateFinalBillingDate(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, END_DATE_AFTER_NEXT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_advance_endDateBeforeNextBilling_shouldDelete() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, END_DATE_BEFORE_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(NEXT_BILLING));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_advance_endDateOnNextBilling_shouldDelete() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, END_DATE_ON_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(NEXT_BILLING));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_arrears_endDateBeforeNextBilling_shouldSetFinalBillingDate() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ARREARS, END_DATE_BEFORE_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(NEXT_BILLING));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), ARREARS_START_FROM);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService).updateFinalBillingDate(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, END_DATE_BEFORE_NEXT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_arrears_endDateAfterNextBilling_shouldDelete() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ARREARS, END_DATE_AFTER_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(NEXT_BILLING));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), ARREARS_START_FROM);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verify(mockScheduledBillingService).deleteByExternalId(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_nullEndDate_shouldNotTouchFinalBillingDate() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_nullInvoicedIn_shouldNotTouchFinalBillingDate() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, null, END_DATE_AFTER_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void handleEvent_noScheduledBillingFound_shouldDoNothing() {
		var contract = buildContract(Status.ACTIVE, IntervalType.QUARTERLY, InvoicedIn.ADVANCE, END_DATE_AFTER_NEXT, null);
		when(mockContractIntegration.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(mockScheduledBillingService.getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());

		service.handleEvent(eventRequest(EventType.CREATED));

		verify(mockScheduledBillingService).upsert(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT, Set.of(3, 6, 9, 12), Set.of(1), START_DATE);
		verify(mockScheduledBillingService).getNextScheduledBilling(MUNICIPALITY_ID, CONTRACT_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	// ========== helpers ==========

	private EventRequest eventRequest(EventType eventType) {
		return EventRequest.builder()
			.withId(CONTRACT_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withEventType(eventType)
			.build();
	}

	private Contract buildContract(Status status, IntervalType intervalType, InvoicedIn invoicedIn, LocalDate endDate, LocalDate currentPeriodEndDate) {
		var contract = new Contract();
		contract.setStatus(status);
		contract.setStartDate(START_DATE);
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
