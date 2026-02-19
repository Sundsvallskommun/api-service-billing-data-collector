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

class BerakningarExternTest {

	@Test
	void testBean() {
		assertThat(BerakningarExtern.class, allOf(
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

		final var berakningarExtern = BerakningarExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withFakturatextExtern(fakturatextExtern)
			.withAntalExtern(antalExtern)
			.withAPrisExtern(aPrisExtern)
			.build();

		assertThat(berakningarExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(berakningarExtern.getQueryID()).isEqualTo(queryID);
		assertThat(berakningarExtern.getName()).isEqualTo(name);
		assertThat(berakningarExtern.getFakturatextExtern()).isEqualTo(fakturatextExtern);
		assertThat(berakningarExtern.getAntalExtern()).isEqualTo(antalExtern);
		assertThat(berakningarExtern.getAPrisExtern()).isEqualTo(aPrisExtern);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(BerakningarExtern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
