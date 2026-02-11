package se.sundsvall.billingdatacollector.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class CounterpartMappingEntityTest {

	@Test
	void testBean() {
		MatcherAssert.assertThat(CounterpartMappingEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var id = "id";
		final var legalIdPattern = "legalIdPattern";
		final var stakeholderType = "stakeholderType";
		final var counterpart = "counterpart";

		final var entity = CounterpartMappingEntity.builder()
			.withId(id)
			.withLegalIdPattern(legalIdPattern)
			.withStakeholderType(stakeholderType)
			.withCounterpart(counterpart)
			.build();

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getLegalIdPattern()).isEqualTo(legalIdPattern);
		assertThat(entity.getStakeholderType()).isEqualTo(stakeholderType);
		assertThat(entity.getCounterpart()).isEqualTo(counterpart);
	}

	@Test
	void testNoDirt() {
		assertThat(CounterpartMappingEntity.builder().build()).hasAllNullFieldsOrProperties();
		assertThat(new CounterpartMappingEntity()).hasAllNullFieldsOrProperties();
	}
}
