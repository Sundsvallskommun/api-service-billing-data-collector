package se.sundsvall.billingdatacollector.service.scheduling.certificate.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class HealthTest {

	@Test
	void testBean() {
		assertThat(Health.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var healthy = true;
		final var message = "message";

		final var bean = Health.create()
			.withMessage(message)
			.withHealthy(healthy);

		assertThat(bean.getMessage()).isEqualTo(message);
		assertThat(bean.isHealthy()).isEqualTo(healthy);
	}

	@Test
	void noDirtOnCreatedBean() {
		assertThat(new Health()).hasAllNullFieldsOrPropertiesExcept("healthy")
			.extracting("healthy").isEqualTo(false);
		assertThat(Health.create()).hasAllNullFieldsOrPropertiesExcept("healthy")
			.extracting("healthy").isEqualTo(false);
	}
}
