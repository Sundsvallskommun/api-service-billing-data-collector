package se.sundsvall.billingdatacollector.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

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
		var billingRecord = BillingRecord.builder().build();
		var legalId = "1234567890";
		var familyId = "123";
		var flowInstanceId = "4657";

		var wrapper = BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(legalId)
			.withFamilyId(familyId)
			.withFlowInstanceId(flowInstanceId)
			.build();

		assertThat(wrapper).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(wrapper.getBillingRecord()).isEqualTo(billingRecord);
		assertThat(wrapper.getLegalId()).isEqualTo(legalId);
		assertThat(wrapper.getFamilyId()).isEqualTo(familyId);
		assertThat(wrapper.getFlowInstanceId()).isEqualTo(flowInstanceId);
	}

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(BillingRecordWrapper.builder().build()).isNotNull().hasAllNullFieldsOrProperties();
	}
}
