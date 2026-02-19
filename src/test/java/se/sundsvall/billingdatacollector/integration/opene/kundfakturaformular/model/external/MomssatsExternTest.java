package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external;

import org.junit.jupiter.api.Test;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

class MomssatsExternTest {

	@Test
	void testBean() {
		assertThat(MomssatsExtern.class, allOf(
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

		final var momssatsExtern = MomssatsExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withValue(value)
			.build();

		assertThat(momssatsExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(momssatsExtern.getQueryID()).isEqualTo(queryID);
		assertThat(momssatsExtern.getName()).isEqualTo(name);
		assertThat(momssatsExtern.getValue()).isEqualTo(value);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(MomssatsExtern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
