package se.sundsvall.billingdatacollector.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/BillingCollectorAPIIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
})
class BillingCollectorAPIIT extends AbstractAppTest {

	private static final String SERVICE_PATH = "/2281/trigger";
	private static final String RESPONSE_FILE = "response.json";

	private static final String FAMILY_ID = "358";
	private static final List<String> FLOW_INSTANCE_IDS = List.of("185375", "185376");

	private static final LocalDate START_DATE = LocalDate.of(2024, 1, 1);
	private static final LocalDate END_DATE = LocalDate.of(2024, 1, 31);

	@Autowired
	private FalloutRepository falloutRepository;

	@Autowired
	private HistoryRepository historyRepository;

	@Test
	void test1_triggerBillingWithFamilyIds_shouldOnlyTriggerSupported() {
		CommonStubs.stubAccessToken();
		setupCall()
			.withServicePath(SERVICE_PATH + "?startDate=" + START_DATE + "&endDate=" + END_DATE + "&familyIds=358&familyIds=unsupported-family-id")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();

		final var historyEntities = historyRepository.findAll();

		// Not verifying the content, only that we have saved the entities.
		assertThat(historyEntities).hasSize(2);
		historyEntities.forEach(entity -> {
			assertThat(entity.getFamilyId()).isEqualTo(FAMILY_ID);
			assertThat(entity.getFlowInstanceId()).isIn(FLOW_INSTANCE_IDS);
		});

		assertThat(falloutRepository.count()).isZero();
	}

	@Test
	void test2_triggerBillingWithUnsupportedFamilyId() {
		setupCall()
			.withServicePath(SERVICE_PATH + "?startDate=" + START_DATE + "&endDate=" + END_DATE + "&familyIds=unsupported-family-id")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();    // No stubs used

		assertThat(historyRepository.count()).isZero();
		assertThat(falloutRepository.count()).isZero();
	}

	@Test
	void test3_triggerBillingForAFlowInstanceId() {
		CommonStubs.stubAccessToken();
		setupCall()
			.withServicePath(SERVICE_PATH + "/185375")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequestAndVerifyResponse()
			.verifyAllStubs();

		final var historyEntities = historyRepository.findAll();

		// Not verifying the content, only that we have saved the entities.
		assertThat(historyEntities).hasSize(1);

		assertThat(historyEntities.getFirst().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(historyEntities.getFirst().getFlowInstanceId()).isIn(FLOW_INSTANCE_IDS.getFirst());

		assertThat(falloutRepository.count()).isZero();
	}
}
