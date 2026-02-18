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

class SummeringInternTest {

	@Test
	void testBean() {
		assertThat(SummeringIntern.class, allOf(
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
		final var totSummeringIntern = "totSummeringIntern";

		final var summeringIntern = SummeringIntern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withTotSummeringIntern(totSummeringIntern)
			.build();

		assertThat(summeringIntern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(summeringIntern.getQueryID()).isEqualTo(queryID);
		assertThat(summeringIntern.getName()).isEqualTo(name);
		assertThat(summeringIntern.getTotSummeringIntern()).isEqualTo(totSummeringIntern);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(SummeringIntern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
