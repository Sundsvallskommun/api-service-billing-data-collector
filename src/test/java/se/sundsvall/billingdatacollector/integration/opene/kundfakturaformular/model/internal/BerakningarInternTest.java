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

class BerakningarInternTest {

	@Test
	void testBean() {
		assertThat(BerakningarIntern.class, allOf(
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
		final var fakturaTextIntern = "fakturaTextIntern";
		final var antalIntern = "antalIntern";
		final var aPrisIntern = "aPrisIntern";

		final var berakningarIntern = BerakningarIntern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withFakturatextIntern(fakturaTextIntern)
			.withAntalIntern(antalIntern)
			.withAPrisIntern(aPrisIntern)
			.build();

		assertThat(berakningarIntern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(berakningarIntern.getQueryID()).isEqualTo(queryID);
		assertThat(berakningarIntern.getName()).isEqualTo(name);
		assertThat(berakningarIntern.getFakturatextIntern()).isEqualTo(fakturaTextIntern);
		assertThat(berakningarIntern.getAntalIntern()).isEqualTo(antalIntern);
		assertThat(berakningarIntern.getAPrisIntern()).isEqualTo(aPrisIntern);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(BerakningarIntern.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
