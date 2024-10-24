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

class BerakningExternTest {

	@Test
	void testBean() {
		assertThat(BerakningExtern.class, allOf(
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

		final var berakningExtern = BerakningExtern.builder()
			.withQueryID(queryID)
			.withName(name)
			.withFakturatextExtern(fakturatextExtern)
			.withAntalExtern(antalExtern)
			.withAPrisExtern(aPrisExtern)
			.build();

		assertThat(berakningExtern).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(berakningExtern.getQueryID()).isEqualTo(queryID);
		assertThat(berakningExtern.getName()).isEqualTo(name);
		assertThat(berakningExtern.getFakturatextExtern()).isEqualTo(fakturatextExtern);
		assertThat(berakningExtern.getAntalExtern()).isEqualTo(antalExtern);
		assertThat(berakningExtern.getAPrisExtern()).isEqualTo(aPrisExtern);
	}
}
