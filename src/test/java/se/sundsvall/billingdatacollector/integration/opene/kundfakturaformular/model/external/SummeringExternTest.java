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

class SummeringExternTest {

	@Test
	void testBean() {
		assertThat(SummeringExtern.class, allOf(
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
		final var totSummeringExtern = "totSummeringExtern";

		final var summeringExtern = SummeringExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withTotSummeringExtern(totSummeringExtern)
			.build();

		assertThat(summeringExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(summeringExtern.getQueryID()).isEqualTo(queryID);
		assertThat(summeringExtern.getName()).isEqualTo(name);
		assertThat(summeringExtern.getTotSummeringExtern()).isEqualTo(totSummeringExtern);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SummeringExtern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
