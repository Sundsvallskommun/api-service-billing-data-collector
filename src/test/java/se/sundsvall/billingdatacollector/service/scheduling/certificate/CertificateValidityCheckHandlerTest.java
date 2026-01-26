package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("junit")
class CertificateValidityCheckHandlerTest {

	@Autowired
	private CertificateValidityCheckHandler handler;

	@MockitoBean
	Dept44HealthUtility dept44HealthUtilityMock;

	@Test
	void certificateHasNotPassedCloseToExpiration() {
		// We need to change defined period for days until expiration by reflection for this test
		ReflectionTestUtils.setField(handler, "warnDaysBeforeExpiration", -1000);

		// Act
		handler.checkCertificateHealth();

		// Verify that health mock is triggered at least once (as startup sequence will interfere with verification)
		verify(dept44HealthUtilityMock).setHealthIndicatorHealthy("certificate-health");
	}

	@Test
	void certificateHasPassedCloseToExpiration() {
		// We need to change defined period for days until expiration by reflection for this test
		ReflectionTestUtils.setField(handler, "warnDaysBeforeExpiration", 1000);

		// Act
		handler.checkCertificateHealth();

		// Verify that health mock is triggered at least once (as startup sequence will interfere with verification)
		verify(dept44HealthUtilityMock).setHealthIndicatorUnhealthy(eq("certificate-health"), matches("One or more certificate are approaching its expiration date,.+, and should be replaced with a new one"));
	}

	@Test
	void toCertificateThrowsException() {
		assertDoesNotThrow(() -> assertThat(handler.toCertificate(null)).isNull());
	}

	@Test
	void toInputStreamThrowsException() {
		assertDoesNotThrow(() -> assertThat(handler.toInputStream(null)).isNull());
	}
}
