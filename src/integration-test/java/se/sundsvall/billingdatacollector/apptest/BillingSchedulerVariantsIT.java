package se.sundsvall.billingdatacollector.apptest;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.db.ScheduledBillingRepository;
import se.sundsvall.billingdatacollector.service.scheduling.BillingScheduler;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IT coverage for the contract-event-driven billing pipeline. Each test
 * pre-loads a single {@code scheduled_billing} row matching the variant
 * under test, fires the scheduler tick, and verifies that:
 *
 * <ul>
 * <li>the BillingPreprocessor was POSTed the expected request body
 * (period description, costPerUnit, accuralKey, contractId etc.) — the
 * fixture's {@code equalToJson} matcher fails the test if any field is
 * wrong;</li>
 * <li>{@code scheduled_billing} ended up in the expected post-tick state
 * (advanced, deleted, or unchanged) so the lifecycle outcome is
 * verified end-to-end;</li>
 * <li>a {@code history} row was persisted iff a billing was actually
 * sent.</li>
 * </ul>
 *
 * <p>
 * Each variant inserts its own {@code scheduled_billing} row (see
 * {@code src/test/resources/db/billing-scheduler/}). The shared
 * {@code testdata.sql} no longer seeds scheduled_billing rows, so per-test
 * files own all schedule state.
 */
@WireMockAppTestSuite(files = "classpath:/BillingSchedulerVariantsIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata.sql"
})
// MERGE so per-method @Sql files (which seed only the test's
// scheduled_billing row) run *in addition* to the class-level
// truncate+testdata, instead of overriding it.
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Import(NoOpShedlockTestConfig.class)
class BillingSchedulerVariantsIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private BillingScheduler billingScheduler;

	@Autowired
	private HistoryRepository historyRepository;

	@Autowired
	private ScheduledBillingRepository scheduledBillingRepository;

	private long historyBaselineCount;

	@BeforeEach
	void captureHistoryBaseline() {
		// Captured after @Sql has seeded the database. Tests then assert
		// against this baseline rather than a hard-coded number, so changes
		// to testdata.sql don't silently break unrelated assertions.
		historyBaselineCount = historyRepository.count();
	}

	// ============================================================
	// Happy-path variants — one per (interval × invoicedIn) combo.
	// ============================================================

	@Test
	@Sql("/db/billing-scheduler/test1_advanceMonthly.sql")
	void test1_advanceMonthly_shouldSendBillingAndAdvanceToNextMonth() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00010", LocalDate.of(2020, 3, 1));
		assertHistoryHasContract("2026-00010 (REF-MONTHLY-A)");
		verifyStubs();
	}

	@Test
	@Sql("/db/billing-scheduler/test2_arrearsMonthly.sql")
	void test2_arrearsMonthly_shouldSendBillingAndAdvanceToNextMonth() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00011", LocalDate.of(2020, 3, 1));
		assertHistoryHasContract("2026-00011 (REF-MONTHLY-R)");
		verifyStubs();
	}

	@Test
	@Sql("/db/billing-scheduler/test3_arrearsQuarterly.sql")
	void test3_arrearsQuarterly_shouldSendBillingAndAdvanceToNextQuarter() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00012", LocalDate.of(2020, 6, 1));
		assertHistoryHasContract("2026-00012 (REF-QUARTERLY-R)");
		verifyStubs();
	}

	@Test
	@Sql("/db/billing-scheduler/test4_advanceHalfYearly.sql")
	void test4_advanceHalfYearly_shouldSendBillingAndAdvanceToNextHalfYear() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00013", LocalDate.of(2020, 12, 1));
		assertHistoryHasContract("2026-00013 (REF-HALFYEARLY-A)");
		verifyStubs();
	}

	@Test
	@Sql("/db/billing-scheduler/test5_arrearsHalfYearly.sql")
	void test5_arrearsHalfYearly_shouldSendBillingAndAdvanceToNextHalfYear() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00014", LocalDate.of(2020, 12, 1));
		assertHistoryHasContract("2026-00014 (REF-HALFYEARLY-R)");
		verifyStubs();
	}

	@Test
	@Sql("/db/billing-scheduler/test6_advanceYearly.sql")
	void test6_advanceYearly_shouldSendBillingAndAdvanceToNextYear() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00015", LocalDate.of(2021, 12, 1));
		assertHistoryHasContract("2026-00015 (REF-YEARLY-A)");
		verifyStubs();
	}

	@Test
	@Sql("/db/billing-scheduler/test7_arrearsYearly.sql")
	void test7_arrearsYearly_shouldSendBillingAndAdvanceToNextYear() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00016", LocalDate.of(2021, 12, 1));
		assertHistoryHasContract("2026-00016 (REF-YEARLY-R)");
		verifyStubs();
	}

	/**
	 * LAND_LEASE_RESIDENTIAL with a current-period end date of 30 June uses
	 * the June yearly slot (instead of December). YEARLY ADVANCE on a June
	 * slot covers July of the same year through June of the following year.
	 */
	@Test
	@Sql("/db/billing-scheduler/test8_advanceYearlyJuneLandLeaseResidential.sql")
	void test8_advanceYearlyJune_landLeaseResidential_shouldUseJuneSlot() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertScheduleAdvancedTo("2026-00017", LocalDate.of(2021, 6, 1));
		assertHistoryHasContract("2026-00017 (REF-YEARLY-LLR)");
		verifyStubs();
	}

	// ============================================================
	// Lifecycle outcomes — Skipped, last-billing, paused, errors.
	// ============================================================

	/**
	 * ARREARS YEARLY, slot 2020-12-01 (covers Jan-Dec 2020 — fits), endDate
	 * 2021-06-30. Next slot 2021-12-01 would cover Jan-Dec 2021 which extends
	 * past 2021-06-30 → handler returns Sent(null), scheduler deletes the
	 * row. BillingPreprocessor IS still called for the current (final)
	 * billing.
	 */
	@Test
	@Sql("/db/billing-scheduler/test9_lastBillingDeletesRow.sql")
	void test9_whenNextSlotPeriodExtendsPastEndDate_shouldSendCurrentAndDeleteRow() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, "2026-00018", BillingSource.CONTRACT))
			.as("row deleted because next slot's period extends past contract endDate")
			.isEmpty();
		assertHistoryHasContract("2026-00018 (REF-YEARLY-LAST)");
		verifyStubs();
	}

	/**
	 * ARREARS QUARTERLY, slot 2020-09-01 (covers Q3 Jul-Sep 2020), endDate
	 * 2020-08-15 — current period already extends past endDate. Handler
	 * returns Skipped → scheduler deletes the row, BPP must NOT be called.
	 */
	@Test
	@Sql("/db/billing-scheduler/test10_periodPastEndDateSkipsAndDeletes.sql")
	void test10_whenCurrentPeriodExtendsPastEndDate_shouldSkipWithoutSendingAndDeleteRow() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, "2026-00019", BillingSource.CONTRACT))
			.as("row deleted because current period already extends past endDate")
			.isEmpty();
		assertHistoryUnchanged();
		verifyStubs();
	}

	/**
	 * Paused row must NOT be picked up by the scheduler — no contract fetch,
	 * no BPP call, no history row.
	 */
	@Test
	@Sql("/db/billing-scheduler/test11_pausedRowNotProcessed.sql")
	void test11_pausedRow_shouldNotBeProcessed() {
		// No setupCall — no stubs registered. Any unexpected outbound call
		// would manifest as a connection failure; a successful test asserts
		// only on persistence state.

		billingScheduler.createBillingRecords();

		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, "2026-00020", BillingSource.CONTRACT))
			.as("paused row left intact")
			.isPresent()
			.get()
			.satisfies(entity -> {
				assertThat(entity.isPaused()).isTrue();
				assertThat(entity.getNextScheduledBilling()).isEqualTo(LocalDate.of(2020, 1, 1));
				assertThat(entity.getLastBilled()).isNull();
			});
		assertHistoryUnchanged();
	}

	/**
	 * Contract 404 means an event was dropped — the legitimate cleanup path
	 * is a TERMINATED event. The scheduler must keep the row for retry and
	 * NOT call BPP.
	 */
	@Test
	@Sql("/db/billing-scheduler/test12_contractNotFoundKeepsRow.sql")
	void test12_contractNotFound_shouldKeepRowAndNotCallBpp() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, "2026-00021", BillingSource.CONTRACT))
			.as("row kept after Failed result for retry")
			.isPresent()
			.get()
			.satisfies(entity -> {
				assertThat(entity.getNextScheduledBilling()).isEqualTo(LocalDate.of(2020, 6, 1));
				assertThat(entity.getLastBilled()).isNull();
			});
		assertHistoryUnchanged();
		verifyStubs();
	}

	/**
	 * Contract returns 200 but its invoicing block is incomplete — handler
	 * returns Skipped, scheduler deletes the row, BPP must NOT be called.
	 */
	@Test
	@Sql("/db/billing-scheduler/test13_contractNoLongerBillable.sql")
	void test13_contractNoLongerBillable_shouldDropScheduleWithoutBilling() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, "2026-00022", BillingSource.CONTRACT))
			.as("row deleted because contract is no longer billable")
			.isEmpty();
		assertHistoryUnchanged();
		verifyStubs();
	}

	/**
	 * BPP rejects the request → handler returns Failed, scheduler keeps the
	 * row for retry, no history row is persisted, no relation is created.
	 */
	@Test
	@Sql("/db/billing-scheduler/test14_bppErrorKeepsRow.sql")
	void test14_bppError_shouldKeepRowAndNotPersistHistory() {
		setupCall();

		billingScheduler.createBillingRecords();

		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, "2026-00023", BillingSource.CONTRACT))
			.as("row kept after BPP error for retry on next tick")
			.isPresent()
			.get()
			.satisfies(entity -> {
				assertThat(entity.getNextScheduledBilling()).isEqualTo(LocalDate.of(2020, 2, 1));
				assertThat(entity.getLastBilled()).isNull();
			});
		assertHistoryUnchanged();
		verifyStubs();
	}

	// ============================================================
	// Helpers
	// ============================================================

	/**
	 * @param expectedContractId the {@code contractId (externalReferenceId)}
	 *                           string the mapper produces — verified to be
	 *                           present on the persisted history row, so the
	 *                           billing belongs to the right contract.
	 */
	private void assertHistoryHasContract(String expectedContractId) {
		var entities = historyRepository.findAll();
		assertThat(entities)
			.as("history table after billing scheduler tick — baseline + 1 row from this billing")
			.hasSize((int) historyBaselineCount + 1)
			.anySatisfy(history -> assertThat(history.getContractId()).isEqualTo(expectedContractId));
	}

	private void assertHistoryUnchanged() {
		assertThat(historyRepository.count())
			.as("history table must not change when no billing was sent")
			.isEqualTo(historyBaselineCount);
	}

	private void assertScheduleAdvancedTo(String externalId, LocalDate expectedNextSlot) {
		var entity = scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, externalId, BillingSource.CONTRACT)
			.orElseThrow(() -> new AssertionError("Expected scheduled_billing row for " + externalId));
		assertThat(entity)
			.as("scheduled_billing row after successful billing")
			.satisfies(e -> {
				assertThat(e.getNextScheduledBilling())
					.as("nextScheduledBilling for %s", externalId)
					.isEqualTo(expectedNextSlot);
				assertThat(e.getLastBilled())
					.as("lastBilled for %s — set by scheduler on Sent", externalId)
					.isNotNull();
			});
	}
}
