package se.sundsvall.billingdatacollector.apptest;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.db.ScheduledJobRepository;
import se.sundsvall.billingdatacollector.service.scheduling.billing.BillingJobHandler;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/BillingCollectorIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql"
})
class BillingCollectorIT extends AbstractAppTest {

	private static final String FAMILY_ID = "358";
	private static final List<String> FLOW_INSTANCE_IDS = List.of("185375", "185376");

	@BeforeEach
	public void setup() {
		CommonStubs.stubAccessToken();
	}

	@Autowired
	private BillingJobHandler billingJobHandler;

	@Autowired
	private FalloutRepository falloutRepository;

	@Autowired
	private HistoryRepository historyRepository;

	@Autowired
	private ScheduledJobRepository scheduledJobRepository;

	@Test
	void test1_fetchAndCreateBillingRecordsForPrivatePerson() {
		// Setup wiremock
		setupCall();

		// Trigger the "scheduled" job
		billingJobHandler.performBilling();
		await()
			.atMost(5, SECONDS)
			.until(() -> historyRepository.count() > 0);

		var historyEntities = historyRepository.findAll();

		//Check that we have two records in the database and that they're the ones we want.
		// We won't assert everything, that's done in the unit tests.
		assertThat(historyEntities).hasSize(2);
		historyEntities.forEach(entity -> {
			assertThat(entity.getFamilyId()).isEqualTo(FAMILY_ID);
			assertThat(entity.getFlowInstanceId()).isIn(FLOW_INSTANCE_IDS);
		});

		// Check that the scheduled job has been saved in the database
		var jobEntity = scheduledJobRepository.findAll();
		assertThat(jobEntity).hasSize(1);
		assertThat(jobEntity.getFirst().getFetchedEndDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(jobEntity.getFirst().getFetchedStartDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(jobEntity.getFirst().getProcessed()).isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));

		// And check that we have no fallouts
		assertThat(falloutRepository.count()).isZero();
	}

	@Test
	void test2_fetchAndCreateBillingRecords_shouldGiveFallout_whenErrorFromBillingPreProcessor() {
		// Setup wiremock
		setupCall();

		// Trigger the "scheduled" job
		billingJobHandler.performBilling();

		await()
			.atMost(5, SECONDS)
			.until(() -> falloutRepository.count() > 0);

		var historyEntities = historyRepository.findAll();

		//Check that we have no history
		assertThat(historyEntities).isEmpty();

		// And check that we have a fallout, not verifying mapping, only that we have the correct one.
		var fallouts = falloutRepository.findAll();
		assertThat(fallouts).hasSize(1);
		assertThat(fallouts.getFirst().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(fallouts.getFirst().getFlowInstanceId()).isEqualTo(FLOW_INSTANCE_IDS.getFirst());
		assertThat(fallouts.getFirst().getBillingRecordWrapper()).isNotNull();
		assertThat(fallouts.getFirst().getBillingRecordWrapper().getBillingRecord()).isNotNull();
		assertThat(fallouts.getFirst().getOpenEInstance()).isNull();

		// Check that the scheduled job has been saved in the database
		var jobEntity = scheduledJobRepository.findAll();
		assertThat(jobEntity).hasSize(1);
		assertThat(jobEntity.getFirst().getFetchedEndDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(jobEntity.getFirst().getFetchedStartDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(jobEntity.getFirst().getProcessed()).isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
	}

	@Test
	void test3_fetchAndCreateBillingRecords_shouldGiveFallout_whenErrorFromOpenE() {
		// Setup wiremock
		setupCall();

		// Trigger the "scheduled" job
		billingJobHandler.performBilling();
		await()
			.atMost(5, SECONDS)
			.until(() -> falloutRepository.count() > 0);

		var historyEntities = historyRepository.findAll();

		//Check that we have no history
		assertThat(historyEntities).isEmpty();

		// And check that we have a fallout, not verifying mapping, only that we have the correct one.
		var fallouts = falloutRepository.findAll();
		assertThat(fallouts).hasSize(1);
		assertThat(fallouts.getFirst().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(fallouts.getFirst().getFlowInstanceId()).isEqualTo(FLOW_INSTANCE_IDS.getFirst());
		assertThat(fallouts.getFirst().getOpenEInstance()).isNotNull();
		assertThat(fallouts.getFirst().getBillingRecordWrapper()).isNull();

		// Check that the scheduled job has been saved in the database
		var jobEntity = scheduledJobRepository.findAll();
		assertThat(jobEntity).hasSize(1);
		assertThat(jobEntity.getFirst().getFetchedEndDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(jobEntity.getFirst().getFetchedStartDate()).isEqualTo(LocalDate.now().minusDays(1));
		assertThat(jobEntity.getFirst().getProcessed()).isCloseTo(OffsetDateTime.now(), within(5, ChronoUnit.SECONDS));
	}
}
