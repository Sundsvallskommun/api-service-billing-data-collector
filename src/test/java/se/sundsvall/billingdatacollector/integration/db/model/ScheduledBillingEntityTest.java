package se.sundsvall.billingdatacollector.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.sundsvall.billingdatacollector.api.model.BillingSource;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class ScheduledBillingEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		assertThat(ScheduledBillingEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = UUID.randomUUID().toString();
		final var municipalityId = "2281";
		final var externalId = UUID.randomUUID().toString();
		final var source = BillingSource.CONTRACT;
		final var billingDaysOfMonth = Set.of(1, 15);
		final var billingMonths = Set.of(1, 4, 7, 10);
		final var lastBilled = OffsetDateTime.now();
		final var nextScheduledBilling = LocalDate.now().plusMonths(1);
		final var paused = true;

		final var entity = ScheduledBillingEntity.builder()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withExternalId(externalId)
			.withSource(source)
			.withBillingDaysOfMonth(billingDaysOfMonth)
			.withBillingMonths(billingMonths)
			.withLastBilled(lastBilled)
			.withNextScheduledBilling(nextScheduledBilling)
			.withPaused(paused)
			.build();

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getExternalId()).isEqualTo(externalId);
		assertThat(entity.getSource()).isEqualTo(source);
		assertThat(entity.getBillingDaysOfMonth()).isEqualTo(billingDaysOfMonth);
		assertThat(entity.getBillingMonths()).isEqualTo(billingMonths);
		assertThat(entity.getLastBilled()).isEqualTo(lastBilled);
		assertThat(entity.getNextScheduledBilling()).isEqualTo(nextScheduledBilling);
		assertThat(entity.isPaused()).isEqualTo(paused);
	}

	@Test
	void testNoDirt() {
		assertThat(ScheduledBillingEntity.builder().build())
			.hasAllNullFieldsOrPropertiesExcept("paused")
			.extracting("paused").isEqualTo(false);
		assertThat(new ScheduledBillingEntity())
			.hasAllNullFieldsOrPropertiesExcept("paused")
			.extracting("paused").isEqualTo(false);
	}
}
