package se.sundsvall.billingdatacollector.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.allOf;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

class HistoryEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> LocalDate.now().plusDays(new Random().nextInt()), LocalDate.class);
	}

	@Test
	void testBean() {
		MatcherAssert.assertThat(HistoryEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("billingRecordWrapper"),
			hasValidBeanEqualsExcluding("billingRecordWrapper"),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		// Set values as variables
		final var id = "id";
		final var billingRecordWrapper = new BillingRecordWrapper();
		final var familyId = "familyId";
		final var flowInstanceId = "flowInstanceId";
		final var created = LocalDate.now();
		final var location = "location";

		final var historyEntity = HistoryEntity.builder()
			.withId(id)
			.withBillingRecordWrapper(billingRecordWrapper)
			.withFamilyId(familyId)
			.withFlowInstanceId(flowInstanceId)
			.withCreated(created)
			.withLocation(location)
			.build();

		assertThat(historyEntity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(historyEntity.getId()).isEqualTo(id);
		assertThat(historyEntity.getBillingRecordWrapper()).isEqualTo(billingRecordWrapper);
		assertThat(historyEntity.getFamilyId()).isEqualTo(familyId);
		assertThat(historyEntity.getFlowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(historyEntity.getCreated()).isEqualTo(created);
		assertThat(historyEntity.getLocation()).isEqualTo(location);
	}

	@Test
	void testPrepersist() {
		final var historyEntity = new HistoryEntity();
		historyEntity.prePersist();
		assertThat(historyEntity.getCreated()).isCloseTo(LocalDate.now(), within(0, ChronoUnit.DAYS));
	}

}
