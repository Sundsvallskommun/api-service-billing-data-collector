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

class AktivitetskontoInternTest {

	@Test
	void testBean() {
		assertThat(AktivitetskontoIntern.class, allOf(
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

		final var aktivitetskontoIntern = AktivitetskontoIntern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withValue(value)
			.build();

		assertThat(aktivitetskontoIntern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(aktivitetskontoIntern.getQueryID()).isEqualTo(queryID);
		assertThat(aktivitetskontoIntern.getName()).isEqualTo(name);
		assertThat(aktivitetskontoIntern.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(AktivitetskontoIntern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
