package se.sundsvall.billingdatacollector.service.scheduling.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
public class BillingScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(BillingScheduler.class);

	private final BillingJobHandler billingJobHandler;

	public BillingScheduler(final BillingJobHandler billingJobHandler) {
		this.billingJobHandler = billingJobHandler;
	}

	@Dept44Scheduled(
		cron = "${scheduler.opene.cron.expression}",
		name = "${scheduler.opene.name}",
		lockAtMostFor = "${scheduler.opene.lock-at-most-for}",
		maximumExecutionTime = "${scheduler.opene.maximum-execution-time}")
	public void runBillingJob() {
		billingJobHandler.performBilling();
	}
}
