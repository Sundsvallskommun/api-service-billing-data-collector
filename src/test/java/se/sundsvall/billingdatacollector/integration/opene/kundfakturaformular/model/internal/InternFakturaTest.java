package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.InternFaktura;

class InternFakturaTest {

	@Test
	void constructorAndAccessors() {
		var familyId = "358";
		var flowInstanceId = "12345";
		var posted = "2023-11-24T15:40:23";
		var sendInvoiceTo = "sendInvoiceTo";
		var payingAdministration = "payingAdministration";
		var sellerInformationFirstName = "sellerInformationFirstName";
		var sellerInformationLastName = "sellerInformationLastName";
		var referenceSundsvallsMunicipality = "referenceSundsvallsMunicipality";

		var internFaktura = new InternFaktura(familyId, flowInstanceId, posted, sendInvoiceTo, payingAdministration,
			sellerInformationFirstName, sellerInformationLastName, referenceSundsvallsMunicipality);

		assertThat(internFaktura.familyId()).isEqualTo(familyId);
		assertThat(internFaktura.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(internFaktura.sendInvoiceTo()).isEqualTo(sendInvoiceTo);
		assertThat(internFaktura.payingAdministration()).isEqualTo(payingAdministration);
		assertThat(internFaktura.sellerInformationFirstName()).isEqualTo(sellerInformationFirstName);
		assertThat(internFaktura.sellerInformationLastName()).isEqualTo(sellerInformationLastName);
		assertThat(internFaktura.referenceSundsvallsMunicipality()).isEqualTo(referenceSundsvallsMunicipality);
	}
}
