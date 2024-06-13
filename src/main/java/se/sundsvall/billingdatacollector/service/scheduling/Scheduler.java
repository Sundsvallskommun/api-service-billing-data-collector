package se.sundsvall.billingdatacollector.service.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class Scheduler {

	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);

	private final JobHandler jobHandler;

	public Scheduler(JobHandler jobHandler) {
		this.jobHandler = jobHandler;
	}

	@Scheduled(cron = "${opene.cron}")
	@SchedulerLock(name = "${opene.name}", lockAtMostFor = "${opene.lock-at-most-for}")
	public void runScheduledJob() {
		LOG.info("Running scheduled job");
		jobHandler.handleJob();
		LOG.info("Scheduled job finished");
	}
}
