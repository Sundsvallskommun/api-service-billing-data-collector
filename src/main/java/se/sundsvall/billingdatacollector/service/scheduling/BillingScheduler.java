package se.sundsvall.billingdatacollector.service.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sundsvall.dept44.requestid.RequestId;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
public class BillingScheduler {

	private final BillingJobHandler billingJobHandler;

	public BillingScheduler(BillingJobHandler billingJobHandler) {
		this.billingJobHandler = billingJobHandler;
	}

	@Scheduled(cron = "${scheduler.opene.cron.expression}")
	@SchedulerLock(name = "${scheduler.opene.name}", lockAtMostFor = "${scheduler.opene.lock-at-most-for}")
	public void runBillingJob() {
		RequestId.init();
		billingJobHandler.handleBilling();
	}
}
