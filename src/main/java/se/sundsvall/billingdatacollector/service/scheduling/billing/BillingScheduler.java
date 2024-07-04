package se.sundsvall.billingdatacollector.service.scheduling.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sundsvall.dept44.requestid.RequestId;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
public class BillingScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(BillingScheduler.class);

	private final BillingJobHandler billingJobHandler;

	public BillingScheduler(BillingJobHandler billingJobHandler) {
		this.billingJobHandler = billingJobHandler;
	}

	@Scheduled(cron = "${scheduler.opene.cron.expression}")
	@SchedulerLock(name = "${scheduler.opene.name}", lockAtMostFor = "${scheduler.opene.lock-at-most-for}")
	public void runBillingJob() {
		LOG.info("Scheduled task is starting billing job.");
		RequestId.init();
		billingJobHandler.performBilling();
	}
}
