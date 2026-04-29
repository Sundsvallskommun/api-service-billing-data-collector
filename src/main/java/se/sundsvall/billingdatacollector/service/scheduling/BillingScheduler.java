package se.sundsvall.billingdatacollector.service.scheduling;

import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.billingdatacollector.service.ScheduledBillingService;
import se.sundsvall.billingdatacollector.service.source.BillingResult;
import se.sundsvall.billingdatacollector.service.source.BillingResult.Failed;
import se.sundsvall.billingdatacollector.service.source.BillingResult.Sent;
import se.sundsvall.billingdatacollector.service.source.BillingResult.Skipped;
import se.sundsvall.billingdatacollector.service.source.BillingSourceHandler;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

/**
 * Cron-driven scheduler that picks up due {@code ScheduledBillingEntity}
 * rows and dispatches each to its source-specific {@link BillingSourceHandler}
 * (e.g. {@code "contract"}). The handler performs the actual billing and
 * returns a {@link BillingResult} that tells this scheduler whether to
 * advance the row, delete it, or report a failure.
 */
@Service
public class BillingScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(BillingScheduler.class);

	@Value("${scheduler.billing.name}")
	private String jobName;

	private final Dept44HealthUtility dept44HealthUtility;
	private final ScheduledBillingService scheduledBillingService;
	private final Map<String, BillingSourceHandler> billingSourceHandlerMap;

	public BillingScheduler(final Dept44HealthUtility dept44HealthUtility,
		final ScheduledBillingService scheduledBillingService,
		final Map<String, BillingSourceHandler> billingSourceHandlerMap) {
		this.dept44HealthUtility = dept44HealthUtility;
		this.scheduledBillingService = scheduledBillingService;
		this.billingSourceHandlerMap = billingSourceHandlerMap;
	}

	@Dept44Scheduled(cron = "${scheduler.billing.cron}",
		name = "${scheduler.billing.name}",
		lockAtMostFor = "${scheduler.billing.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.billing.maximum-execution-time}")
	public void createBillingRecords() {
		scheduledBillingService.getDueScheduledBillings().forEach(this::processEntity);
	}

	private void processEntity(ScheduledBillingEntity entity) {
		var sourceKey = entity.getSource().name().toLowerCase();
		var handler = billingSourceHandlerMap.get(sourceKey);
		if (handler == null) {
			LOG.error("Skipping scheduled billing — no handler for source '{}', municipalityId '{}', externalId: '{}'",
				entity.getSource().name(), entity.getMunicipalityId(), entity.getExternalId());
			markUnhealthy("Missing billing handler implementation for source '%s'".formatted(entity.getSource().name()));
			return;
		}

		BillingResult result;
		try {
			result = handler.sendBillingRecords(entity);
		} catch (final Exception e) {
			LOG.error("Exception when sending billing records! municipalityId '{}', source: '{}', externalId: '{}'",
				entity.getMunicipalityId(), entity.getSource().name(), entity.getExternalId(), e);
			markUnhealthy("Failed to create billing record(s) for source '%s'".formatted(entity.getSource().name()));
			return;
		}

		switch (result) {
			case Sent sent -> handleSent(entity, sent);
			case Skipped skipped -> handleSkipped(entity, skipped);
			case Failed failed -> markUnhealthy(failed.reason());
		}
	}

	private void handleSent(ScheduledBillingEntity entity, Sent sent) {
		entity.setLastBilled(OffsetDateTime.now());
		if (sent.nextSlot() == null) {
			// Last billing for this contract — drop the schedule.
			scheduledBillingService.deleteScheduledBillingEntity(entity);
		} else {
			entity.setNextScheduledBilling(sent.nextSlot());
			scheduledBillingService.saveScheduledBillingEntity(entity);
		}
	}

	private void handleSkipped(ScheduledBillingEntity entity, Skipped skipped) {
		LOG.info("Dropping scheduled billing for source '{}', externalId '{}': {}",
			entity.getSource().name(), entity.getExternalId(), skipped.reason());
		scheduledBillingService.deleteScheduledBillingEntity(entity);
	}

	private void markUnhealthy(String reason) {
		dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, "Billing error: " + reason);
	}
}
