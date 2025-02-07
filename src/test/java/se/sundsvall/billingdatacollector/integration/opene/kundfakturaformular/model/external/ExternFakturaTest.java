package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.ExternFaktura;

class ExternFakturaTest {

	@Test
	void constructorAndAccessors() {
		var familyId = "358";
		var flowInstanceId = "12345";
		var posted = "2024-09-20T15:28:23";
		var privatePersonFirstName = "firstName";
		var privatePersonLastName = "lastName";
		var privatePersonAddress = "privatePersonAddress";
		var privatePersonZipCode = "privatePersonZipCode";
		var privatePersonPostalAddress = "privatePersonPostalAddress";
		var socialSecurityNumber = "socialSecurityNumber";
		var counterpartPrivatePersonName = "counterpartPrivatePersonName";
		var sellerInformationFirstName = "sellerInformationFirstName";
		var sellerInformationLastName = "sellerInformationLastName";
		var organizationInformation = "1234567890";
		var referenceOrganization = "referenceOrganization";
		var sendInvoiceTo = "sendInvoiceTo";
		var manualOrgInfoOrganizationNumber = "manualOrgInfoOrganizationNumber";
		var manualOrgInfoName = "manualOrgInfoName";
		var manualOrgInfoAddress = "manualOrgInfoAddress";
		var manualOrgInfoCo = "manualOrgInfoCo";
		var manualOrgInfoZipCode = "manualOrgInfoZipCode";
		var manualOrgInfoCity = "manualOrgInfoCity";
		var manualOrgInfoReference = "manualOrgInfoReference";
		var manualOrgInfoMotpart = "manualOrgInfoMotpart";

		var externFaktura = new ExternFaktura(familyId, flowInstanceId, posted, privatePersonFirstName, privatePersonLastName, privatePersonAddress, privatePersonZipCode, privatePersonPostalAddress,
			socialSecurityNumber, counterpartPrivatePersonName, sellerInformationFirstName, sellerInformationLastName, organizationInformation, referenceOrganization, sendInvoiceTo,
			manualOrgInfoOrganizationNumber, manualOrgInfoName, manualOrgInfoAddress, manualOrgInfoCo, manualOrgInfoZipCode, manualOrgInfoCity, manualOrgInfoReference, manualOrgInfoMotpart);

		assertThat(externFaktura.familyId()).isEqualTo(familyId);
		assertThat(externFaktura.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(externFaktura.posted()).isEqualTo(posted);
		assertThat(externFaktura.privatePersonFirstName()).isEqualTo(privatePersonFirstName);
		assertThat(externFaktura.privatePersonLastName()).isEqualTo(privatePersonLastName);
		assertThat(externFaktura.privatePersonAddress()).isEqualTo(privatePersonAddress);
		assertThat(externFaktura.privatePersonZipCode()).isEqualTo(privatePersonZipCode);
		assertThat(externFaktura.privatePersonPostalAddress()).isEqualTo(privatePersonPostalAddress);
		assertThat(externFaktura.socialSecurityNumber()).isEqualTo(socialSecurityNumber);
		assertThat(externFaktura.counterpartPrivatePersonName()).isEqualTo(counterpartPrivatePersonName);
		assertThat(externFaktura.sellerInformationFirstName()).isEqualTo(sellerInformationFirstName);
		assertThat(externFaktura.sellerInformationLastName()).isEqualTo(sellerInformationLastName);
		assertThat(externFaktura.organizationInformation()).isEqualTo(organizationInformation);
		assertThat(externFaktura.referenceOrganization()).isEqualTo(referenceOrganization);
		assertThat(externFaktura.sendInvoiceTo()).isEqualTo(sendInvoiceTo);
		assertThat(externFaktura.manualOrgInfoOrganizationNumber()).isEqualTo(manualOrgInfoOrganizationNumber);
		assertThat(externFaktura.manualOrgInfoName()).isEqualTo(manualOrgInfoName);
		assertThat(externFaktura.manualOrgInfoAddress()).isEqualTo(manualOrgInfoAddress);
		assertThat(externFaktura.manualOrgInfoCo()).isEqualTo(manualOrgInfoCo);
		assertThat(externFaktura.manualOrgInfoZipCode()).isEqualTo(manualOrgInfoZipCode);
		assertThat(externFaktura.manualOrgInfoCity()).isEqualTo(manualOrgInfoCity);
		assertThat(externFaktura.manualOrgInfoReference()).isEqualTo(manualOrgInfoReference);
		assertThat(externFaktura.manualOrgInfoMotpart()).isEqualTo(manualOrgInfoMotpart);
	}
}
