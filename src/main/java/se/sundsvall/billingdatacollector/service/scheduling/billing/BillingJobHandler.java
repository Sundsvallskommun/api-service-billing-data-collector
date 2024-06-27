package se.sundsvall.billingdatacollector.service.scheduling.billing;

import static java.util.Collections.emptySet;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.billingdatacollector.service.DbService;

@Component
public class BillingJobHandler {

	private static final Logger LOG = LoggerFactory.getLogger(BillingJobHandler.class);

	private final CollectorService collectorService;
	private final DbService dbService;

	public BillingJobHandler(CollectorService collectorService, DbService dbService) {
		this.collectorService = collectorService;
		this.dbService = dbService;
	}

	public void performBilling() {
		LOG.info("Starting billing job");
		var latestJob = dbService.getLatestJob();
		var startDate = calculateStartDate(latestJob);
		var endDate = LocalDate.now().minusDays(1);	// Always set enddate to yesterday

		dbService.saveScheduledJob(startDate, endDate);	//Save that the job has been triggered

		var processed = collectorService.triggerBillingBetweenDates(startDate, endDate, emptySet());
		LOG.info("Billing job done, processed {} records", processed.size());
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
