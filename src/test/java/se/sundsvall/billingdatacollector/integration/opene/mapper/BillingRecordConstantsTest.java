package se.sundsvall.billingdatacollector.integration.opene.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BillingRecordConstantsTest {

	@Test
	void testConstants() {
		assertThat(BillingRecordConstants.SUNDSVALLS_MUNICIPALIY).isEqualTo("Sundsvalls Kommun");
		assertThat(BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER).isEqualTo("2120002411");
		assertThat(BillingRecordConstants.KUNDFAKTURA_FORMULAR_FAMILY_ID).isEqualTo("358");
	}
}
