package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.Invoicing;
import java.net.URI;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.billingdatacollector.integration.relation.RelationClient;
import se.sundsvall.billingdatacollector.service.source.BillingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractBillingHandlerTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String CONTRACT_ID = "2026-00001";

	@Mock
	private ContractIntegration contractIntegrationMock;

	@Mock
	private ContractMapper contractMapperMock;

	@Mock
	private BillingPreprocessorClient billingPreprocessorClientMock;

	@Mock
	private HistoryRepository historyRepositoryMock;

	@Mock
	private RelationClient relationClientMock;

	@Mock
	private BillingRecord billingRecordMock;

	@InjectMocks
	private ContractBillingHandler handler;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(
			contractIntegrationMock,
			contractMapperMock,
			billingPreprocessorClientMock,
			historyRepositoryMock,
			relationClientMock);
	}

	/**
	 * A5 from the spec — Quarterly ADVANCE on 1 Jun 2026, contract ends
	 * 14 Mar 2027. Period Q3 (Jul-Sep 2026) fits, next slot Q4 (Oct-Dec 2026)
	 * also fits → handler returns Sent(2026-09-01).
	 */
	@Test
	void sendBillingRecords_advanceQuarterlyWithFutureEndDate_returnsSentWithNextSlot() {
		var entity = quarterlyEntity(LocalDate.of(2026, 6, 1));
		var contract = contract(IntervalType.QUARTERLY, InvoicedIn.ADVANCE, LocalDate.of(2027, 3, 14));

		stubSuccessfulPipeline(entity, contract);

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Sent.class);
		assertThat(((BillingResult.Sent) result).nextSlot()).isEqualTo(LocalDate.of(2026, 9, 1));
		verify(billingPreprocessorClientMock).createBillingRecord(MUNICIPALITY_ID, billingRecordMock);
		verify(historyRepositoryMock).saveAndFlush(any());
		verify(relationClientMock).createRelation(eq(MUNICIPALITY_ID), any());
	}

	/**
	 * Last leg of A5 — Quarterly ADVANCE slot 1 Sep 2026 with endDate
	 * 14 Mar 2027. Q4 still fits, but the *next* slot (Dec 2026) would
	 * cover Q1 2027 which extends past 14 Mar 2027 → Sent(null).
	 */
	@Test
	void sendBillingRecords_whenNextSlotPeriodExtendsPastEndDate_returnsSentWithNullNextSlot() {
		var entity = quarterlyEntity(LocalDate.of(2026, 9, 1));
		var contract = contract(IntervalType.QUARTERLY, InvoicedIn.ADVANCE, LocalDate.of(2027, 3, 14));

		stubSuccessfulPipeline(entity, contract);

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Sent.class);
		assertThat(((BillingResult.Sent) result).nextSlot()).isNull();
		verify(billingPreprocessorClientMock).createBillingRecord(MUNICIPALITY_ID, billingRecordMock);
		verify(historyRepositoryMock).saveAndFlush(any());
		verify(relationClientMock).createRelation(eq(MUNICIPALITY_ID), any());
	}

	/**
	 * R5 — Quarterly ARREARS slot 1 Jun 2026, contract ends 14 Apr 2026.
	 * Period Q2 (Apr-Jun 2026) extends past the end date → Skipped, no
	 * invoice sent.
	 */
	@Test
	void sendBillingRecords_whenCurrentPeriodExtendsPastEndDate_returnsSkippedWithoutSending() {
		var entity = quarterlyEntity(LocalDate.of(2026, 6, 1));
		var contract = contract(IntervalType.QUARTERLY, InvoicedIn.ARREARS, LocalDate.of(2026, 4, 14));

		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Skipped.class);
		verifyNoInteractions(billingPreprocessorClientMock, historyRepositoryMock);
		verify(contractMapperMock, never()).createBillingRecord(any(), any(), any());
	}

	/**
	 * R4 — Yearly ARREARS slot 1 Dec 2026, endDate 14 Mar 2027. Period
	 * Jan-Dec 2026 fits, next slot Dec 2027 would cover Jan-Dec 2027 which
	 * extends past 14 Mar 2027 → Sent(null).
	 */
	@Test
	void sendBillingRecords_yearlyArrearsLastBilling_returnsSentWithNullNextSlot() {
		var entity = ScheduledBillingEntity.builder()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withExternalId(CONTRACT_ID)
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1))
			.withBillingMonths(Set.of(12))
			.withNextScheduledBilling(LocalDate.of(2026, 12, 1))
			.build();
		var contract = contract(IntervalType.YEARLY, InvoicedIn.ARREARS, LocalDate.of(2027, 3, 14));

		stubSuccessfulPipeline(entity, contract);

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Sent.class);
		assertThat(((BillingResult.Sent) result).nextSlot()).isNull();
		verify(billingPreprocessorClientMock).createBillingRecord(MUNICIPALITY_ID, billingRecordMock);
		verify(historyRepositoryMock).saveAndFlush(any());
		verify(relationClientMock).createRelation(eq(MUNICIPALITY_ID), any());
	}

	@Test
	void sendBillingRecords_whenContractNotFound_returnsFailed() {
		var entity = quarterlyEntity(LocalDate.of(2026, 6, 1));
		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Failed.class);
		assertThat(((BillingResult.Failed) result).reason())
			.contains(CONTRACT_ID)
			.contains("404")
			.contains("TERMINATED");
		verifyNoInteractions(billingPreprocessorClientMock, historyRepositoryMock);
	}

	@Test
	void sendBillingRecords_whenContractMissingInvoicing_returnsSkipped() {
		var entity = quarterlyEntity(LocalDate.of(2026, 6, 1));
		var contract = new Contract();   // no invoicing set
		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Skipped.class);
		verifyNoInteractions(billingPreprocessorClientMock, historyRepositoryMock);
	}

	@Test
	void sendBillingRecords_whenContractHasNoEndDate_alwaysAdvances() {
		var entity = quarterlyEntity(LocalDate.of(2026, 6, 1));
		var contract = contract(IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null);

		stubSuccessfulPipeline(entity, contract);

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Sent.class);
		assertThat(((BillingResult.Sent) result).nextSlot()).isEqualTo(LocalDate.of(2026, 9, 1));
		verify(billingPreprocessorClientMock).createBillingRecord(MUNICIPALITY_ID, billingRecordMock);
		verify(historyRepositoryMock).saveAndFlush(any());
		verify(relationClientMock).createRelation(eq(MUNICIPALITY_ID), any());
	}

	@Test
	void sendBillingRecords_whenBillingPreprocessorFails_returnsFailed() {
		var entity = quarterlyEntity(LocalDate.of(2026, 6, 1));
		var contract = contract(IntervalType.QUARTERLY, InvoicedIn.ADVANCE, null);

		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(contractMapperMock.createBillingRecord(MUNICIPALITY_ID, contract, entity.getNextScheduledBilling()))
			.thenReturn(billingRecordMock);
		when(billingPreprocessorClientMock.createBillingRecord(MUNICIPALITY_ID, billingRecordMock))
			.thenThrow(new RuntimeException("preprocessor down"));

		var result = handler.sendBillingRecords(entity);

		assertThat(result).isInstanceOf(BillingResult.Failed.class);
		verifyNoInteractions(historyRepositoryMock, relationClientMock);
	}

	private void stubSuccessfulPipeline(ScheduledBillingEntity entity, Contract contract) {
		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contract));
		when(contractMapperMock.createBillingRecord(MUNICIPALITY_ID, contract, entity.getNextScheduledBilling()))
			.thenReturn(billingRecordMock);
		var headers = new HttpHeaders();
		headers.setLocation(URI.create("http://billing-preprocessor/billing-records/abc-123"));
		when(billingPreprocessorClientMock.createBillingRecord(eq(MUNICIPALITY_ID), eq(billingRecordMock)))
			.thenReturn(ResponseEntity.status(HttpStatus.CREATED).headers(headers).build());
	}

	private static ScheduledBillingEntity quarterlyEntity(LocalDate nextScheduledBilling) {
		return ScheduledBillingEntity.builder()
			.withMunicipalityId(MUNICIPALITY_ID)
			.withExternalId(CONTRACT_ID)
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1))
			.withBillingMonths(Set.of(3, 6, 9, 12))
			.withNextScheduledBilling(nextScheduledBilling)
			.build();
	}

	private static Contract contract(IntervalType interval, InvoicedIn invoicedIn, LocalDate endDate) {
		var contract = new Contract();
		var invoicing = new Invoicing();
		invoicing.setInvoiceInterval(interval);
		invoicing.setInvoicedIn(invoicedIn);
		contract.setInvoicing(invoicing);
		contract.setEndDate(endDate);
		return contract;
	}
}
