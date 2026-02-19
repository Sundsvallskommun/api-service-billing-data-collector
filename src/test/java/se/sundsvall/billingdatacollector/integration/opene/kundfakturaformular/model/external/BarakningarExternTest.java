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

class BarakningarExternTest {

	@Test
	void testBean() {
		assertThat(BarakningarExtern.class, allOf(
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
		final var fakturatextExtern = "fakturatextExtern";
		final var antalExtern = "antalExtern";
		final var aPrisExtern = "aPrisExtern";

		final var barakningarExtern = BarakningarExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withFakturatextExtern(fakturatextExtern)
			.withAntalExtern(antalExtern)
			.withAPrisExtern(aPrisExtern)
			.build();

		assertThat(barakningarExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(barakningarExtern.getQueryID()).isEqualTo(queryID);
		assertThat(barakningarExtern.getName()).isEqualTo(name);
		assertThat(barakningarExtern.getFakturatextExtern()).isEqualTo(fakturatextExtern);
		assertThat(barakningarExtern.getAntalExtern()).isEqualTo(antalExtern);
		assertThat(barakningarExtern.getAPrisExtern()).isEqualTo(aPrisExtern);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(BarakningarExtern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
