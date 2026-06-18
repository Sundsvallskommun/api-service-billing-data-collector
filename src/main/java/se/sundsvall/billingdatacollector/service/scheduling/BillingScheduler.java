package se.sundsvall.billingdatacollector.service.scheduling;

import java.time.Duration;
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

import static java.time.OffsetDateTime.now;
import static java.util.Objects.isNull;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

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
		// A matching "completed" line is logged at the end of every tick. If a
		// "started" line ever appears without its "completed" counterpart, the
		// tick hung (e.g. an integration call without a read timeout blocking
		// the scheduler thread) — that is the prime suspect when the health
		// indicator gets stuck unhealthy until a restart, since the scheduling
		// aspect can only reset the indicator on a tick that actually returns.
		final var startedAt = now();
		var sent = 0;
		var skipped = 0;
		var failed = 0;
		var processed = 0;
		try {
			final var dueBillings = scheduledBillingService.getDueScheduledBillings();
			processed = dueBillings.size();
			LOG.info("Billing tick started — {} due scheduled billing(s) to process", processed);

			for (final var entity : dueBillings) {
				switch (processEntity(entity)) {
					case SENT -> sent++;
					case SKIPPED -> skipped++;
					case FAILED -> failed++;
				}
			}
		} catch (final Error e) {
			// The dept44 scheduling aspect only catches Exception, and it has
			// already called resetErrors() before this method ran. If an Error
			// (e.g. OutOfMemoryError, NoClassDefFoundError) escaped here, the
			// aspect's finally-block would see no error flag and FALSELY mark the
			// indicator healthy. Record the failure so the indicator stays
			// unhealthy, then rethrow so the platform still observes the Error.
			markUnhealthy("Fatal error during billing tick: " + e);
			throw e;
		}

		final var durationMs = Duration.between(startedAt, now()).toMillis();
		if (failed == 0) {
			// Heal the indicator explicitly instead of relying solely on the
			// dept44 scheduling aspect's finally-block. The aspect only resets
			// the indicator when this method runs through the Spring proxy; an
			// out-of-band or otherwise un-proxied invocation would execute the
			// body (and log this line) without ever reaching the aspect, leaving
			// an earlier unhealthy state latched until a restart. This call is
			// idempotent, and when the aspect IS in the path its finally still
			// runs after us, so the maximum-execution-time guard is preserved.
			markHealthy();
			LOG.info("Billing tick completed in {} ms — processed {} (sent: {}, skipped: {}, failed: 0); "
				+ "no failures this tick, so health indicator '{}' was reset to healthy",
				durationMs, processed, sent, skipped, jobName);
		} else {
			LOG.warn("Billing tick completed in {} ms — processed {} (sent: {}, skipped: {}, failed: {}); "
				+ "health indicator '{}' was marked UNHEALTHY this tick and stays so until a future failure-free tick",
				durationMs, processed, sent, skipped, failed, jobName);
		}
	}

	private Outcome processEntity(ScheduledBillingEntity entity) {
		final var source = entity.getSource().name();
		final var sourceKey = source.toLowerCase();
		final var sanitizedMunicipalityId = sanitizeForLogging(entity.getMunicipalityId());
		final var sanitizedExternalId = sanitizeForLogging(entity.getExternalId());

		LOG.info("Processing scheduled billing — source: '{}', municipalityId: '{}', externalId: '{}', nextScheduledBilling: '{}'",
			source, sanitizedMunicipalityId, sanitizedExternalId, entity.getNextScheduledBilling());

		final var handler = billingSourceHandlerMap.get(sourceKey);
		if (isNull(handler)) {
			LOG.error("Skipping scheduled billing — no handler for source '{}', municipalityId '{}', externalId: '{}'",
				source, sanitizedMunicipalityId, sanitizedExternalId);
			markUnhealthy("Missing billing handler implementation for source '%s'".formatted(source));
			return Outcome.FAILED;
		}

		final var handlerStartedAt = now();
		BillingResult result;
		try {
			result = handler.sendBillingRecords(entity);
		} catch (final Exception e) {
			LOG.error("Exception when sending billing records after {} ms! municipalityId '{}', source: '{}', externalId: '{}'",
				Duration.between(handlerStartedAt, now()).toMillis(), sanitizedMunicipalityId, source, sanitizedExternalId, e);
			markUnhealthy("Failed to create billing record(s) for source '%s'".formatted(source));
			return Outcome.FAILED;
		}
		final var handlerMs = Duration.between(handlerStartedAt, now()).toMillis();

		return switch (result) {
			case Sent sent -> {
				handleSent(entity, sent);
				LOG.info("Scheduled billing SENT in {} ms — source: '{}', externalId: '{}', nextSlot: '{}'{}",
					handlerMs, source, sanitizedExternalId, sent.nextSlot(),
					isNull(sent.nextSlot()) ? " (last billing — schedule deleted)" : "");
				yield Outcome.SENT;
			}
			case Skipped skipped -> {
				handleSkipped(entity, skipped);
				yield Outcome.SKIPPED;
			}
			case Failed failed -> {
				LOG.warn("Scheduled billing FAILED in {} ms — source: '{}', externalId: '{}', reason: {}",
					handlerMs, source, sanitizedExternalId, failed.reason());
				markUnhealthy(failed.reason());
				yield Outcome.FAILED;
			}
		};
	}

	private void handleSent(ScheduledBillingEntity entity, Sent sent) {
		entity.setLastBilled(now());
		if (isNull(sent.nextSlot())) {
			// Last billing for this contract — drop the schedule.
			scheduledBillingService.deleteScheduledBillingEntity(entity);
		} else {
			entity.setNextScheduledBilling(sent.nextSlot());
			scheduledBillingService.saveScheduledBillingEntity(entity);
		}
	}

	private void handleSkipped(ScheduledBillingEntity entity, Skipped skipped) {
		LOG.info("Dropping scheduled billing for source '{}', externalId '{}': {}",
			entity.getSource().name(), sanitizeForLogging(entity.getExternalId()), skipped.reason());
		scheduledBillingService.deleteScheduledBillingEntity(entity);
	}

	private void markUnhealthy(String reason) {
		LOG.warn("Marking health indicator '{}' UNHEALTHY: {}", jobName, reason);
		dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, "Billing error: " + reason);
	}

	private void markHealthy() {
		dept44HealthUtility.setHealthIndicatorHealthy(jobName);
	}

	/**
	 * Per-entity outcome of a single tick, tallied so the closing tick log can
	 * report how many rows were sent, skipped and failed.
	 */
	private enum Outcome {
		SENT,
		SKIPPED,
		FAILED
	}
}
