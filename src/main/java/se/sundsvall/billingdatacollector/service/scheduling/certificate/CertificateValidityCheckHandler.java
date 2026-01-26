package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.time.DateUtils.toLocalDateTime;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.time.LocalDate;
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

@Component
public class CertificateValidityCheckHandler {
	private static final String CERTIFICATE_SUFFIX_PATTERN = "^.*(\\.cer|\\.crt)$";
	private static final String MESSAGE_UNHEALTHY_CERTIFICATE = "One or more certificate are approaching its expiration date, %s, and should be replaced with a new one";
	private static final String MESSAGE_COULD_NOT_READ_CERTIFICATE = "Certificate could not be read, see logs for more information";
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
			// Read SCB certificate and validate if it is approaching its expiration date
			getLocalCertificates().findFirst().ifPresentOrElse(certificate -> {
				final var warningDate = toLocalDateTime(certificate.getNotAfter()).minusDays(warnDaysBeforeExpiration).toLocalDate(); // Subtract days from exipiration date to get some wiggle room before the certificate expires

				if (LocalDate.now().isAfter(warningDate)) {
					certificateHealthConsumer.accept(Health.create()
						.withHealthy(false)
						.withMessage(MESSAGE_UNHEALTHY_CERTIFICATE.formatted(DateFormat.getDateInstance().format(certificate.getNotAfter()))));
				} else {
					certificateHealthConsumer.accept(Health.create()
						.withHealthy(true));
				}
			}, () -> certificateHealthConsumer.accept(Health.create().withHealthy(false).withMessage(MESSAGE_COULD_NOT_READ_CERTIFICATE)));

		} catch (final Exception e) {
			certificateHealthConsumer.accept(Health.create().withHealthy(false).withMessage(MESSAGE_UNKNOWN_EXCEPTION.formatted(e.getClass().getSimpleName(), e.getMessage())));
		}
	}

	private Stream<X509Certificate> getLocalCertificates() throws IOException {
		return Stream.of(ResourceUtils.getFile("classpath:" + PATH).listFiles())
			.filter(file -> file.getName().matches(CERTIFICATE_SUFFIX_PATTERN))
			.map(file -> new ClassPathResource(PATH + file.getName()))
			.map(this::toInputStream)
			.map(this::toCertificate)
			.map(X509Certificate.class::cast);
	}

	Certificate toCertificate(InputStream inputStream) {
		try {
			return certificateFactory.generateCertificate(inputStream);
		} catch (final Exception e) {
			return null;
		}
	}

	InputStream toInputStream(ClassPathResource classpathResource) {
		try {
			return classpathResource.getInputStream();
		} catch (final Exception e) {
			return null;
		}
	}
}
