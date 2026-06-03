package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import se.sundsvall.billingdatacollector.service.scheduling.certificate.model.Health;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.time.DateUtils.toLocalDateTime;

@Component
public class CertificateValidityCheckHandler {
	private static final String CERTIFICATE_SUFFIX_PATTERN = "^.*(\\.cer|\\.crt)$";
	private static final String MESSAGE_UNHEALTHY_CERTIFICATES = "Local certificates are approaching expiration date and should be replaced";
	private static final String MESSAGE_COULD_NOT_READ_CERTIFICATES = "Local certificates could not be read, see logs for more information";
	private static final String MESSAGE_UNKNOWN_EXCEPTION = "%s occurred when validating certificate health (%s)";
	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateValidityCheckHandler.class);
	private static final String PATH = "truststore/";

	private final Consumer<Health> certificateHealthConsumer;
	private final CertificateFactory certificateFactory;

	@Value("${scheduler.certificate-health.warn-days-before-expiration:30}")
	private int warnDaysBeforeExpiration;

	@Value("${scheduler.certificate-health.name}")
	private String schedulerName;

	public CertificateValidityCheckHandler(final Dept44HealthUtility dept44HealthUtility) throws CertificateException {
		this.certificateFactory = CertificateFactory.getInstance("X509");
		this.certificateHealthConsumer = health -> {
			if (TRUE.equals(health.isHealthy())) {
				dept44HealthUtility.setHealthIndicatorHealthy(schedulerName);
			} else {
				dept44HealthUtility.setHealthIndicatorUnhealthy(schedulerName, health.getMessage());
			}
		};
	}

	public void checkCertificateHealth() {
		LOGGER.info("Checking validity of local certificates");

		try {
			// Aggregate the verdict over ALL certificates, then signal the health
			// indicator exactly once (see evaluateCertificates for why a per-cert
			// loop would be unsafe).
			certificateHealthConsumer.accept(evaluateCertificates(getLocalCertificates()));
		} catch (final Exception e) {
			LOGGER.error("Unknown exception occurred when checking certificate health", e);
			certificateHealthConsumer.accept(Health.create().withHealthy(false).withMessage(MESSAGE_UNKNOWN_EXCEPTION.formatted(e.getClass().getSimpleName(), e.getMessage())));
		}
	}

	/**
	 * Derives a single health verdict for the whole truststore.
	 *
	 * <p>
	 * Unhealthy if the certificates could not be read at all, or if <em>any</em>
	 * certificate is approaching expiration. Computing the verdict once (rather
	 * than calling the health consumer per certificate) is deliberate: the
	 * underlying {@code setHealthIndicatorHealthy()/Unhealthy()} calls are
	 * last-write-wins, so a healthy certificate evaluated after an expiring one
	 * would otherwise mask the problem.
	 */
	Health evaluateCertificates(final List<X509Certificate> certificates) {
		if (certificates.isEmpty()) {
			return Health.create().withHealthy(false).withMessage(MESSAGE_COULD_NOT_READ_CERTIFICATES);
		}
		if (certificates.stream().anyMatch(this::isApproachingExpiration)) {
			return Health.create().withHealthy(false).withMessage(MESSAGE_UNHEALTHY_CERTIFICATES);
		}
		return Health.create().withHealthy(true);
	}

	private boolean isApproachingExpiration(final X509Certificate certificate) {
		// Subtract the warning window from the expiration date to get some wiggle
		// room before the certificate actually expires.
		final var warningDate = toLocalDateTime(certificate.getNotAfter())
			.minusDays(warnDaysBeforeExpiration)
			.toLocalDate();
		return LocalDate.now().isAfter(warningDate);
	}

	private List<X509Certificate> getLocalCertificates() throws IOException {
		return Stream.of(ResourceUtils.getFile("classpath:" + PATH).listFiles())
			.filter(file -> file.getName().matches(CERTIFICATE_SUFFIX_PATTERN))
			.map(file -> new ClassPathResource(PATH + file.getName()))
			.map(this::toCertificate)
			.filter(Objects::nonNull)
			.map(X509Certificate.class::cast)
			.toList();
	}

	Certificate toCertificate(ClassPathResource classpathResource) {
		try (var inputStream = classpathResource.getInputStream()) {
			return certificateFactory.generateCertificate(inputStream);
		} catch (final Exception e) {
			LOGGER.error("Exception occurred when generating certificate from classpath resource", e);
			return null;
		}
	}
}
