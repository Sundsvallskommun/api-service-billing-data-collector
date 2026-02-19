package se.sundsvall.billingdatacollector.integration.db.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.allOf;

class ScheduledJobEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(ScheduledJobEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {

		final var id = UUID.randomUUID().toString();
		final var municipalityId = "municipalityId";
		final var fetchedStartDate = LocalDate.now();
		final var fetchedEndDate = LocalDate.now().plusDays(1);
		final var processed = OffsetDateTime.now();

		final var scheduledJobEntity = ScheduledJobEntity.builder()
			.withId(id)
			.withMunicipalityId(municipalityId)
			.withFetchedStartDate(fetchedStartDate)
			.withFetchedEndDate(fetchedEndDate)
			.withProcessed(processed)
			.build();

		assertThat(scheduledJobEntity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(scheduledJobEntity.getId()).isEqualTo(id);
		assertThat(scheduledJobEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(scheduledJobEntity.getFetchedStartDate()).isEqualTo(fetchedStartDate);
		assertThat(scheduledJobEntity.getFetchedEndDate()).isEqualTo(fetchedEndDate);
		assertThat(scheduledJobEntity.getProcessed()).isEqualTo(processed);
	}

	@Test
	void testNoDirt() {
		assertThat(ScheduledJobEntity.builder().build()).hasAllNullFieldsOrProperties();
		assertThat(new ScheduledJobEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersist() {
		final ScheduledJobEntity scheduledJobEntity = new ScheduledJobEntity();
		scheduledJobEntity.prePersist();
		assertThat(scheduledJobEntity.getProcessed()).isCloseTo(OffsetDateTime.now(), within(1, ChronoUnit.SECONDS));
		assertThat(scheduledJobEntity).hasAllNullFieldsOrPropertiesExcept("processed");
	}
}
