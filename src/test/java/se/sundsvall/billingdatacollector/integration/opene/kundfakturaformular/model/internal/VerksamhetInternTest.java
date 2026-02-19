package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class VerksamhetInternTest {

	@Test
	void testBean() {
		assertThat(VerksamhetIntern.class, allOf(
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

		final var verksamhetIntern = VerksamhetIntern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withValue(value)
			.build();

		assertThat(verksamhetIntern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(verksamhetIntern.getQueryID()).isEqualTo(queryID);
		assertThat(verksamhetIntern.getName()).isEqualTo(name);
		assertThat(verksamhetIntern.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(VerksamhetIntern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
