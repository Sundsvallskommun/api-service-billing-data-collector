package se.sundsvall.billingdatacollector.service.scheduling;

import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.sundsvall.billingdatacollector.service.ScheduledBillingService;
import se.sundsvall.billingdatacollector.service.source.BillingSourceHandler;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@Service
public class BillingScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(BillingScheduler.class);

	@Value("${scheduler.billing.name}")
	private String jobName;

	private final ScheduledBillingService scheduledBillingService;
	private final Map<String, BillingSourceHandler> billingSourceHandlerMap;
	private final Consumer<String> billingSetUnHealthyConsumer;

	public BillingScheduler(final Dept44HealthUtility dept44HealthUtility,
		final ScheduledBillingService scheduledBillingService,
		final Map<String, BillingSourceHandler> billingSourceHandlerMap) {

		this.scheduledBillingService = scheduledBillingService;
		this.billingSourceHandlerMap = billingSourceHandlerMap;
		this.billingSetUnHealthyConsumer = msg -> dept44HealthUtility.setHealthIndicatorUnhealthy(jobName, String.format("Billing error: %s", msg));
	}

	@Dept44Scheduled(cron = "${scheduler.billing.cron}",
		name = "${scheduler.billing.name}",
		lockAtMostFor = "${scheduler.billing.shedlock-lock-at-most-for}",
		maximumExecutionTime = "${scheduler.billing.maximum-execution-time}")
	public void createBillingRecords() {
		scheduledBillingService.getDueScheduledBillings()
			.forEach(scheduledBillingEntity -> {
				try {
					var billingHandler = billingSourceHandlerMap.get(scheduledBillingEntity.getSource().name().toLowerCase());
					if (billingHandler != null) {
						billingHandler.sendBillingRecords(scheduledBillingEntity.getMunicipalityId(), scheduledBillingEntity.getExternalId());
						scheduledBillingService.updateNextScheduledBilling(scheduledBillingEntity);
					} else {
						LOG.error("Skipping scheduled billing since no handler exists. municipalityId '{}', source: '{}', externalId: '{}'",
							scheduledBillingEntity.getMunicipalityId(),
							scheduledBillingEntity.getSource().name(),
							scheduledBillingEntity.getExternalId());
						billingSetUnHealthyConsumer.accept(String.format("Missing billing handler implementation for source '%s'", scheduledBillingEntity.getSource().name()));
					}
				} catch (final Exception e) {
					LOG.error("Exception when sending billing records! municipalityId '{}', source: '{}', externalId: '{}'",
						scheduledBillingEntity.getMunicipalityId(),
						scheduledBillingEntity.getSource().name(),
						scheduledBillingEntity.getExternalId(),
						e);
					billingSetUnHealthyConsumer.accept(String.format("Failed to create billing record(s) for source '%s'", scheduledBillingEntity.getSource().name()));
				}
			});
	}
}
