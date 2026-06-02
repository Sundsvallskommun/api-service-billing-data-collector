package se.sundsvall.billingdatacollector.service.scheduling;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.billingdatacollector.service.ScheduledBillingService;
import se.sundsvall.dept44.scheduling.health.Dept44CompositeHealthContributor;
import se.sundsvall.dept44.scheduling.health.Dept44HealthUtility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Reproduction harness for the "health indicator never goes back to healthy"
 * report. Runs the REAL {@link BillingScheduler} proxy (so the ShedLock
 * interceptor and the dept44 scheduling aspect both apply) against the REAL
 * {@link Dept44HealthUtility}/{@link Dept44CompositeHealthContributor}, in a
 * single-instance context where the ShedLock lock is always free (i.e. the
 * lock is always acquired, exactly like a single-pod deployment).
 *
 * <p>
 * If the dept44 aspect's self-heal works in-process, a failure-free tick must
 * flip the indicator from RESTRICTED back to UP. This also verifies the aspect
 * heals the SAME indicator object the utility marked (name identity).
 */
@SpringBootTest(properties = {
	"spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
	"spring.datasource.url=jdbc:tc:mariadb:10.6.4:////",
	"server.shutdown=immediate",
	"spring.lifecycle.timeout-per-shutdown-phase=0s"
})
@ActiveProfiles("junit")
class BillingSchedulerHealResetTest {

	private static final String INDICATOR = "scheduled-billing";

	@TestConfiguration
	static class MockConfig {
		@Bean
		@Primary
		ScheduledBillingService mockScheduledBillingService() {
			final var mock = Mockito.mock(ScheduledBillingService.class);
			when(mock.getDueScheduledBillings()).thenReturn(List.of()); // clean tick — no rows to process
			return mock;
		}
	}

	@Autowired
	private BillingScheduler billingScheduler;

	@Autowired
	private Dept44HealthUtility healthUtility;

	@Autowired
	private Dept44CompositeHealthContributor composite;

	@Test
	void cleanTickThroughProxyResetsIndicatorToHealthy() {
		// Arrange: seed the indicator unhealthy exactly like BillingScheduler.markUnhealthy does.
		healthUtility.setHealthIndicatorUnhealthy(INDICATOR, "seed unhealthy");
		assertThat(statusCode())
			.as("precondition: indicator should be RESTRICTED after being marked unhealthy")
			.isEqualTo("RESTRICTED");

		// Act: run one failure-free tick through the real proxy (ExposeInvocation -> ShedLock -> dept44 aspect -> target).
		billingScheduler.createBillingRecords();

		// Assert: the dept44 aspect's finally-block must have reset it to healthy.
		assertThat(statusCode())
			.as("a failure-free tick must reset '%s' from RESTRICTED back to UP via the dept44 scheduling aspect", INDICATOR)
			.isEqualTo("UP");
	}

	private String statusCode() {
		final var contributor = composite.getContributor(INDICATOR);
		assertThat(contributor).as("indicator '%s' must exist in the composite", INDICATOR).isNotNull();
		return ((HealthIndicator) contributor).health().getStatus().getCode();
	}
}
