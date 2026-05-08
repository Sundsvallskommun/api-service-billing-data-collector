package se.sundsvall.billingdatacollector.apptest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * Multi-tick lifecycle ITs. Unlike {@link BillingSchedulerVariantsIT}, these
 * tests fire the scheduler more than once per test method and need stub
 * behaviour that changes between ticks (different request bodies sent per
 * tick, or different response status codes per tick). That's awkward to
 * express in static fixture files, so the stubs here are registered
 * programmatically — keeping the file-fixture pattern in
 * {@code BillingSchedulerVariantsIT} intact for the single-tick variants.
 *
 * <p>
 * Both tests rely on {@link NoOpShedlockTestConfig} so successive
 * {@code billingScheduler.createBillingRecords()} calls inside the same test
 * are not blocked by the previous tick's shedlock.
 */
@WireMockAppTestSuite(files = "classpath:/BillingSchedulerLifecycleIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata.sql"
})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Import(NoOpShedlockTestConfig.class)
class BillingSchedulerLifecycleIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "fb2f0290-3820-11ed-a261-0242ac122345";
	private static final String LEGAL_ID = "199001012385";
	private static final String ACCESS_TOKEN_BODY = "{\"access_token\":\"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3\","
		+ "\"not-before-policy\":0,\"session_state\":\"88bbf486\",\"token_type\":\"bearer\"}";

	@Autowired
	private BillingScheduler billingScheduler;

	@Autowired
	private HistoryRepository historyRepository;

	@Autowired
	private ScheduledBillingRepository scheduledBillingRepository;

	private long historyBaselineCount;

	@BeforeEach
	void captureBaseline() {
		historyBaselineCount = historyRepository.count();
		stubAccessToken();
	}

	/**
	 * ARREARS YEARLY contract starting 2017-01-01 with endDate 2023-06-30.
	 * Three successive scheduler ticks should each bill one calendar year
	 * (Jan-Dec 2020, then 2021, then 2022). On the third tick the next
	 * slot's period (Jan-Dec 2023) extends past 2023-06-30, so the handler
	 * returns {@code Sent(null)} and the scheduler deletes the row.
	 *
	 * <p>
	 * Verifies progression of {@code nextScheduledBilling}, the period
	 * description that ends up on each BPP request, and that the row is
	 * removed after the final billing.
	 */
	@Test
	@Sql("/db/billing-scheduler/lifecycle1_multiTickArrearsYearly.sql")
	void lifecycle1_arrearsYearly_billsThreeYearsThenDeletesRow() {
		stubContract("2026-00024", arrearsYearlyContractEndingMid2023());
		stubPartyLegalId();
		stubBppAlwaysAccept();
		stubRelationAlwaysAccept();

		// Tick 1: 2020-12-01 → bill Jan-Dec 2020, advance to 2021-12-01.
		billingScheduler.createBillingRecords();
		assertScheduleAt("2026-00024", LocalDate.of(2021, 12, 1));

		// Tick 2: 2021-12-01 → bill Jan-Dec 2021, advance to 2022-12-01.
		billingScheduler.createBillingRecords();
		assertScheduleAt("2026-00024", LocalDate.of(2022, 12, 1));

		// Tick 3: 2022-12-01 → bill Jan-Dec 2022; next slot 2023-12-01 covers
		// 2023 which extends past endDate 2023-06-30 → Sent(null), delete.
		billingScheduler.createBillingRecords();
		assertScheduleDeleted("2026-00024");

		assertThat(historyRepository.count())
			.as("three successful billings → three new history rows on top of baseline")
			.isEqualTo(historyBaselineCount + 3);

		var bppRequests = wiremock.findAll(postRequestedFor(urlPathEqualTo("/bpp/2281/billingrecords")));
		assertThat(bppRequests)
			.as("BPP got one request per tick")
			.hasSize(3);
		assertThat(bppRequests.get(0).getBodyAsString())
			.as("tick 1 covers 2020").contains("Avser januari-december 2020");
		assertThat(bppRequests.get(1).getBodyAsString())
			.as("tick 2 covers 2021").contains("Avser januari-december 2021");
		assertThat(bppRequests.get(2).getBodyAsString())
			.as("tick 3 covers 2022").contains("Avser januari-december 2022");
	}

	/**
	 * BPP fails the first POST with 500, then accepts the second. The
	 * scheduler should keep the row at its original date after tick 1
	 * (no history added, no relation), and on tick 2 successfully bill
	 * and advance the row.
	 */
	@Test
	@Sql("/db/billing-scheduler/lifecycle2_failedThenRetry.sql")
	void lifecycle2_bppFailsThenSucceeds_secondTickAdvancesRow() {
		stubContract("2026-00025", advanceMonthlyContract());
		stubPartyLegalId();
		stubBppFailingThenSucceeding();
		stubRelationAlwaysAccept();

		// Tick 1: BPP returns 500 → Failed; row stays at 2020-02-01.
		billingScheduler.createBillingRecords();
		assertScheduleStaysAt("2026-00025", LocalDate.of(2020, 2, 1));
		assertThat(historyRepository.count())
			.as("BPP failure must not persist a history row")
			.isEqualTo(historyBaselineCount);

		// Tick 2: BPP returns 201 → Sent; row advances to 2020-03-01.
		billingScheduler.createBillingRecords();
		assertScheduleAt("2026-00025", LocalDate.of(2020, 3, 1));
		assertThat(historyRepository.count())
			.as("retry succeeds → exactly one history row added")
			.isEqualTo(historyBaselineCount + 1);

		assertThat(wiremock.findAll(postRequestedFor(urlPathEqualTo("/bpp/2281/billingrecords"))))
			.as("BPP saw both attempts")
			.hasSize(2);
	}

	// ============================================================
	// Stub helpers — registered programmatically because tests change
	// stub state between scheduler ticks.
	// ============================================================

	private void stubAccessToken() {
		wiremock.stubFor(post(urlEqualTo("/token"))
			.willReturn(aResponse()
				.withHeader("Content-Type", "application/json")
				.withBody(ACCESS_TOKEN_BODY)));
	}

	private void stubContract(String contractId, String contractJson) {
		wiremock.stubFor(get(urlEqualTo("/contract/2281/contracts/" + contractId))
			.withHeader("Authorization", equalTo("Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3"))
			.willReturn(aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "application/json")
				.withBody(contractJson)));
	}

	private void stubPartyLegalId() {
		wiremock.stubFor(get(urlEqualTo("/party/2281/PRIVATE/" + PARTY_ID + "/legalId"))
			.willReturn(aResponse()
				.withStatus(200)
				.withHeader("Content-Type", "text/plain")
				.withBody(LEGAL_ID)));
	}

	private void stubBppAlwaysAccept() {
		wiremock.stubFor(post(urlPathEqualTo("/bpp/2281/billingrecords"))
			.willReturn(aResponse()
				.withStatus(201)
				.withHeader("Location", "/billingrecords/cccccccc-1111-1111-1111-cccccccccccc")
				.withHeader("Content-Type", "*/*")));
	}

	private void stubBppFailingThenSucceeding() {
		final var scenario = "bpp-retry";
		wiremock.stubFor(post(urlPathEqualTo("/bpp/2281/billingrecords"))
			.inScenario(scenario)
			.whenScenarioStateIs(STARTED)
			.willSetStateTo("recovered")
			.willReturn(aResponse()
				.withStatus(500)
				.withHeader("Content-Type", "application/problem+json")
				.withBody("{\"status\":500,\"title\":\"Simulated transient BPP failure\"}")));
		wiremock.stubFor(post(urlPathEqualTo("/bpp/2281/billingrecords"))
			.inScenario(scenario)
			.whenScenarioStateIs("recovered")
			.willReturn(aResponse()
				.withStatus(201)
				.withHeader("Location", "/billingrecords/dddddddd-2222-2222-2222-dddddddddddd")
				.withHeader("Content-Type", "*/*")));
	}

	private void stubRelationAlwaysAccept() {
		wiremock.stubFor(post(urlPathEqualTo("/relation/2281/relations"))
			.willReturn(aResponse()
				.withStatus(201)
				.withHeader("Location", "/2281/relations/eeeeeeee-3333-3333-3333-eeeeeeeeeeee")
				.withHeader("Content-Type", "*/*")));
	}

	// ============================================================
	// Assertion helpers
	// ============================================================

	private void assertScheduleAt(String externalId, LocalDate expectedNextSlot) {
		var entity = scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, externalId, BillingSource.CONTRACT)
			.orElseThrow(() -> new AssertionError("Expected scheduled_billing row for " + externalId));
		assertThat(entity.getNextScheduledBilling())
			.as("nextScheduledBilling for %s after this tick", externalId)
			.isEqualTo(expectedNextSlot);
		assertThat(entity.getLastBilled())
			.as("lastBilled for %s — set by scheduler on Sent", externalId)
			.isNotNull();
	}

	private void assertScheduleStaysAt(String externalId, LocalDate originalDate) {
		var entity = scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, externalId, BillingSource.CONTRACT)
			.orElseThrow(() -> new AssertionError("Expected scheduled_billing row for " + externalId));
		assertThat(entity.getNextScheduledBilling())
			.as("row must not advance after Failed result")
			.isEqualTo(originalDate);
		assertThat(entity.getLastBilled())
			.as("lastBilled stays null when no successful billing happened")
			.isNull();
	}

	private void assertScheduleDeleted(String externalId) {
		assertThat(scheduledBillingRepository.findByMunicipalityIdAndExternalIdAndSource(
			MUNICIPALITY_ID, externalId, BillingSource.CONTRACT))
			.as("row deleted after final billing")
			.isEmpty();
	}

	// ============================================================
	// Inline contract-response bodies — kept here (rather than under
	// __files/) because lifecycle tests don't drive their stubs from
	// fixture folders.
	// ============================================================

	private static String arrearsYearlyContractEndingMid2023() {
		return baseContract("2026-00024", "REF-MULTITICK-AY", "YEARLY", "ARREARS", 1000, "Avgift, årshyra")
			.replace("\"version\": 1", "\"endDate\": \"2023-06-30\",\n\t\"version\": 1");
	}

	private static String advanceMonthlyContract() {
		return baseContract("2026-00025", "REF-RETRY-AM", "MONTHLY", "ADVANCE", 12000, "Avgift, månadshyra");
	}

	private static String baseContract(String contractId, String externalReferenceId,
		String invoiceInterval, String invoicedIn, int yearly, String feeDescription) {
		return """
			{
				"contractId": "%s",
				"externalReferenceId": "%s",
				"municipalityId": "2281",
				"status": "ACTIVE",
				"type": "LEASE_AGREEMENT",
				"leaseType": "LAND_LEASE_MISC",
				"startDate": "2017-01-01",
				"invoicing": {
					"invoiceInterval": "%s",
					"invoicedIn": "%s"
				},
				"fees": {
					"additionalInformation": ["%s"],
					"currency": "SEK",
					"yearly": %d,
					"total": %d
				},
				"propertyDesignations": [
					{ "district": "Ankeborg", "name": "ANKEBORG SÖDRA 12:1" }
				],
				"stakeholders": [
					{
						"address": {
							"country": "SVERIGE",
							"postalCode": "111 22",
							"streetAddress": "STORGATAN 1",
							"town": "ANKEBORG",
							"type": "POSTAL_ADDRESS"
						},
						"firstName": "KALLE",
						"lastName": "ANKA",
						"partyId": "%s",
						"roles": ["LESSEE", "PRIMARY_BILLING_PARTY"],
						"type": "PERSON"
					}
				],
				"version": 1
			}
			""".formatted(contractId, externalReferenceId, invoiceInterval, invoicedIn,
			feeDescription, yearly, yearly, PARTY_ID);
	}
}
