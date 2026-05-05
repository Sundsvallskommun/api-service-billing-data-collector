package se.sundsvall.billingdatacollector.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a {@link Clock} bean so services that depend on "today" can inject
 * it (instead of calling {@code LocalDate.now()} directly). Tests override
 * this bean with a fixed clock to make date-sensitive logic deterministic.
 */
@Configuration
class ClockConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}
}
