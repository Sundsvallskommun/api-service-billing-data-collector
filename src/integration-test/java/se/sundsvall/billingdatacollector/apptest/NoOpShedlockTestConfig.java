package se.sundsvall.billingdatacollector.apptest;

import java.util.Optional;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the production {@link LockProvider} (JdbcTemplateLockProvider) with
 * a no-op so successive scheduler invocations from different test methods
 * inside the same Spring context all proceed. Without this, the first test
 * acquires the shedlock and subsequent invocations skip the scheduled method —
 * truncating the {@code shedlock} table between tests is not always enough,
 * since the provider's bookkeeping lives outside that table.
 *
 * <p>
 * Imported via {@code @Import(NoOpShedlockTestConfig.class)} on any IT that
 * triggers {@code BillingScheduler.createBillingRecords()} more than once.
 */
@TestConfiguration
public class NoOpShedlockTestConfig {

	@Bean
	@Primary
	LockProvider noOpLockProvider() {
		return _ -> Optional.of(new SimpleLock() {
			@Override
			public void unlock() {
				// no-op
			}
		});
	}
}
