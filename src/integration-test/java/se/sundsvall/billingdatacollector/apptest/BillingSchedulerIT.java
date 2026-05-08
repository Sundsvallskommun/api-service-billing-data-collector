package se.sundsvall.billingdatacollector.apptest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.service.scheduling.BillingScheduler;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/BillingSchedulerIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata.sql",
	"/db/billing-scheduler-seed.sql"
})
class BillingSchedulerIT extends AbstractAppTest {

	@Autowired
	private BillingScheduler billingScheduler;

	@Autowired
	private HistoryRepository historyRepository;

	@Test
	void test1_createBillingRecords() {
		// Setup wiremock
		setupCall();

		// Trigger the "scheduled" job
		billingScheduler.createBillingRecords();

		var historyEntities = historyRepository.findAll();

		//Two existing rows in test-db
		assertThat(historyEntities).hasSize(3);

		var newHistoryEntity = historyEntities.stream()
			.filter(historyEntity -> historyEntity.getContractId() != null &&
				historyEntity.getContractId().startsWith("2026-00001") &&
				historyEntity.getMunicipalityId().equals("2281"))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Expected history entity not found"));

		assertThat(newHistoryEntity.getCreated()).isCloseTo(OffsetDateTime.now(), within(2, ChronoUnit.SECONDS));
		verifyAllStubs();
	}
}
