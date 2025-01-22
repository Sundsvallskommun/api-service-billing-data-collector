package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Service
public class FalloutScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(FalloutScheduler.class);

	private final FalloutJobHandler falloutJobHandler;

	public FalloutScheduler(final FalloutJobHandler falloutJobHandler) {
		this.falloutJobHandler = falloutJobHandler;
	}

	@Dept44Scheduled(
		cron = "${scheduler.fallout.cron.expression}",
		name = "${scheduler.fallout.name}",
		lockAtMostFor = "${scheduler.fallout.lock-at-most-for}",
		maximumExecutionTime = "${scheduler.fallout.maximum-execution-time}")
	public void runFalloutJob() {
		LOG.info("Scheduled task is starting billing job.");
		RequestId.init();
		falloutJobHandler.handleFallout();
	}
}
