package se.sundsvall.billingdatacollector.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import org.junit.jupiter.api.Test;

class BillingRecordWrapperTest {

	@Test
	void testBean() {
		assertThat(BillingRecordWrapper.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		final var billingRecord = new BillingRecord();
		final var legalId = "1234567890";
		final var familyId = "123";
		final var flowInstanceId = "4657";
		final var municipalityId = "2281";
		final var contractId = "contractId";
		final var isRecipientPrivatePerson = true;

		final var wrapper = BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(legalId)
			.withFamilyId(familyId)
			.withFlowInstanceId(flowInstanceId)
			.withMunicipalityId(municipalityId)
			.withContractId(contractId)
			.withIsRecipientPrivatePerson(isRecipientPrivatePerson)
			.build();

		assertThat(wrapper).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(wrapper.getBillingRecord()).isEqualTo(billingRecord);
		assertThat(wrapper.getLegalId()).isEqualTo(legalId);
		assertThat(wrapper.getFamilyId()).isEqualTo(familyId);
		assertThat(wrapper.getFlowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(wrapper.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(wrapper.getContractId()).isEqualTo(contractId);
		assertThat(wrapper.isRecipientPrivatePerson()).isEqualTo(isRecipientPrivatePerson);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(BillingRecordWrapper.builder().build()).isNotNull().hasFieldOrPropertyWithValue("isRecipientPrivatePerson", false).hasAllNullFieldsOrPropertiesExcept("isRecipientPrivatePerson");
	}
}
