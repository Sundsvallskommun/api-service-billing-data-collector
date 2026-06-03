package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for {@link CertificateValidityCheckHandler#evaluateCertificates}.
 *
 * <p>
 * Guards the fix for the masking bug: the verdict for the whole truststore must
 * be computed by aggregation (unhealthy if <em>any</em> certificate is
 * approaching expiration), independent of iteration order. The previous
 * implementation signalled the health indicator once per certificate, so a
 * healthy certificate evaluated after an expiring one reset the indicator back
 * to healthy and masked the problem.
 */
class CertificateValidityCheckHandlerEvaluateTest {

	private static final String MSG_EXPIRING = "Local certificates are approaching expiration date and should be replaced";
	private static final String MSG_UNREADABLE = "Local certificates could not be read, see logs for more information";

	private CertificateValidityCheckHandler handler;

	@BeforeEach
	void setUp() throws Exception {
		handler = new CertificateValidityCheckHandler(mock(Dept44HealthUtility.class));
		ReflectionTestUtils.setField(handler, "warnDaysBeforeExpiration", 30);
	}

	@Test
	void emptyList_isUnhealthy_couldNotRead() {
		final var health = handler.evaluateCertificates(List.of());

		assertThat(health.isHealthy()).isFalse();
		assertThat(health.getMessage()).isEqualTo(MSG_UNREADABLE);
	}

	@Test
	void allFarFromExpiry_isHealthy() {
		final var health = handler.evaluateCertificates(List.of(certExpiringInDays(3650), certExpiringInDays(1000)));

		assertThat(health.isHealthy()).isTrue();
		assertThat(health.getMessage()).isNull();
	}

	@Test
	void expiringCertFirst_healthyCertLast_isUnhealthy() {
		// This is exactly the ordering the old per-certificate loop got wrong:
		// the trailing healthy cert used to reset the indicator to healthy.
		final var health = handler.evaluateCertificates(List.of(certExpiringInDays(5), certExpiringInDays(3650)));

		assertThat(health.isHealthy()).isFalse();
		assertThat(health.getMessage()).isEqualTo(MSG_EXPIRING);
	}

	@Test
	void healthyCertFirst_expiringCertLast_isUnhealthy() {
		final var health = handler.evaluateCertificates(List.of(certExpiringInDays(3650), certExpiringInDays(5)));

		assertThat(health.isHealthy()).isFalse();
		assertThat(health.getMessage()).isEqualTo(MSG_EXPIRING);
	}

	private static X509Certificate certExpiringInDays(final long days) {
		final var certificate = mock(X509Certificate.class);
		when(certificate.getNotAfter())
			.thenReturn(Date.from(LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return certificate;
	}
}
