package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BillingRecordConstantsTest {

	@Test
	void testConstants() {
		assertThat(BillingRecordConstants.SUNDSVALLS_MUNICIPALITY_ID).isEqualTo("2281");
		assertThat(BillingRecordConstants.SUNDSVALLS_MUNICIPALITY).isEqualTo("Sundsvalls Kommun");
		assertThat(BillingRecordConstants.SUNDSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER).isEqualTo("2120002411");
	}
}
