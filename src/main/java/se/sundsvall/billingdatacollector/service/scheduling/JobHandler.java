package se.sundsvall.billingdatacollector.service.scheduling;

import static java.util.Collections.emptySet;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.billingdatacollector.service.DbService;

@Component
public class JobHandler {

	private static final Logger LOG = LoggerFactory.getLogger(JobHandler.class);

	private final CollectorService collectorService;
	private final DbService dbService;

	public JobHandler(CollectorService collectorService, DbService dbService) {
		this.collectorService = collectorService;
		this.dbService = dbService;
	}

	/**
	 * Fetch billing data
	 */
	public void handleJob() {
		var lastJob = dbService.getLatestJob();
		var startDate = calculateStartDate(lastJob);
		var endDate = LocalDate.now().minusDays(1);	// Always set enddate to yesterday

		dbService.saveScheduledJob(startDate, endDate);	//Save that the job has been triggered

		var processed = collectorService.triggerBetweenDates(startDate, endDate, emptySet());
		handleProcessed(processed);
	}

	/**
	 * Handle the processed jobs
	 * @param processed The processed jobs
	 */
	private void handleProcessed(List<String> processed) {
		if (processed.isEmpty()) {
			LOG.info("No flowInstances processed.");
			return;
		}

		//TODO handle failed jobs.. something slack.. something email..?

		var foundSuccessful = dbService.getHistory(processed);
		LOG.info("Found successful jobs: {}", foundSuccessful.stream().map(HistoryEntity::getFlowInstanceId).toList());

		var foundFailed = dbService.getFallouts(processed);
		LOG.info("Found failed jobs: {}", foundFailed.stream().map(FalloutEntity::getFlowInstanceId).toList());
	}

	/**
	 * If we have no last date, set it to fetch for yesterday
	 * This will render a fetch for one day, yesterday.
	 * @param lastJob The last job fetched
	 * @return The start date to fetch from
	 */
	private LocalDate calculateStartDate(Optional<ScheduledJobEntity> lastJob) {
		return lastJob.map(ScheduledJobEntity::getFetchedEndDate)
			.orElse(LocalDate.now().minusDays(1));
	}
}
