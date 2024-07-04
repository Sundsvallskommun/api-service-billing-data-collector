package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.sundsvall.dept44.requestid.RequestId;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
public class FalloutScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(FalloutScheduler.class);

	private final FalloutJobHandler falloutJobHandler;

	public FalloutScheduler(FalloutJobHandler falloutJobHandler) {
		this.falloutJobHandler = falloutJobHandler;
	}

	@Scheduled(cron = "${scheduler.fallout.cron.expression}")
	@SchedulerLock(name = "${scheduler.fallout.name}", lockAtMostFor = "${scheduler.fallout.lock-at-most-for}")
	public void runFalloutJob() {
		LOG.info("Scheduled task is starting billing job.");
		RequestId.init();
		falloutJobHandler.handleFallout();
	}
}
