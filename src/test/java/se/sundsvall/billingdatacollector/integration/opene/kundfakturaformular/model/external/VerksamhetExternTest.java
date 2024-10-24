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

class VerksamhetExternTest {

	@Test
	void testBean() {
		assertThat(VerksamhetExtern.class, allOf(
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

		final var verksamhetExtern = VerksamhetExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withValue(value)
			.build();

		assertThat(verksamhetExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(verksamhetExtern.getQueryID()).isEqualTo(queryID);
		assertThat(verksamhetExtern.getName()).isEqualTo(name);
		assertThat(verksamhetExtern.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(VerksamhetExtern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}