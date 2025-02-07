package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class UnderkontoInternTest {

	@Test
	void testBean() {
		assertThat(UnderkontoIntern.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var queryID = "1234567890";
		final var name = "name";
		final var value = "value";

		final var underkontoIntern = UnderkontoIntern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withValue(value)
			.build();

		assertThat(underkontoIntern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(underkontoIntern.getQueryID()).isEqualTo(queryID);
		assertThat(underkontoIntern.getName()).isEqualTo(name);
		assertThat(underkontoIntern.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(UnderkontoIntern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
