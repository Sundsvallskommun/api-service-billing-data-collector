package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import org.springframework.stereotype.Component;
import se.sundsvall.dept44.scheduling.Dept44Scheduled;

@Component
public class CertificateValidityCheckScheduler {

	private final CertificateValidityCheckHandler certificateValidityCheckHandler;

	public CertificateValidityCheckScheduler(CertificateValidityCheckHandler certificateValidityCheckHandler) {
		this.certificateValidityCheckHandler = certificateValidityCheckHandler;
	}

	/**
	 * Scheduled check of certificate health
	 */
	@Dept44Scheduled(
		name = "${scheduler.certificate-health.name}",
		cron = "${scheduler.certificate-health.cron:-}",
		lockAtMostFor = "${scheduler.certificate-health.lock-at-most-for}",
		maximumExecutionTime = "${scheduler.certificate-health.maximum-execution-time}")
	void execute() {
		certificateValidityCheckHandler.checkCertificateHealth();
	}
}
