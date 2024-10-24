package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class AktivitetskontoExternTest {

	@Test
	void testBean() {
		assertThat(AktivitetskontoExtern.class, allOf(
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

		final var aktivitetskontoExtern = AktivitetskontoExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withValue(value)
			.build();

		assertThat(aktivitetskontoExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(aktivitetskontoExtern.getQueryID()).isEqualTo(queryID);
		assertThat(aktivitetskontoExtern.getName()).isEqualTo(name);
		assertThat(aktivitetskontoExtern.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(AktivitetskontoExtern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
