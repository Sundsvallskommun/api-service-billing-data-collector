package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import se.sundsvall.billingdatacollector.integration.opene.util.annotation.XPath;

public record ExternFaktura(

	@XPath("/FlowInstance/Header/Flow/FamilyID") String familyId,

	@XPath("/FlowInstance/Header/FlowInstanceID") String flowInstanceId,

	@XPath("/FlowInstance/Header/Posted") String posted,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Firstname") String privatePersonFirstName,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Lastname") String privatePersonLastName,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Address") String privatePersonAddress,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/ZipCode") String privatePersonZipCode,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/PostalAddress") String privatePersonPostalAddress,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/SocialSecurityNumber") String socialSecurityNumber,

	@XPath("/FlowInstance/Values/MotpartPrivatperson/Name") String counterpartPrivatePersonName,

	@XPath("/FlowInstance/Values/SaljarensUppg/Firstname") String sellerInformationFirstName,

	@XPath("/FlowInstance/Values/SaljarensUppg/Lastname") String sellerInformationLastName,

	@XPath("/FlowInstance/Values/OrganisationsnummerExtForetagRD/Value") String organizationInformation,

	@XPath("/FlowInstance/Values/ReferensForetag/Value") String referenceOrganization,

	@XPath("/FlowInstance/Values/FakturanSkickasTill/Value") String sendInvoiceTo,

	// External organization information
	// Only used when someone has entered organization information manually
	// Cannot use OpenECollections here since it will be interpretad as a list.
	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag/Organisationsnummer") String manualOrgInfoOrganizationNumber,

	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag2/Foretagets_namn") String manualOrgInfoName,

	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag2/Adress") String manualOrgInfoAddress,

	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag2/CO") String manualOrgInfoCo,

	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag2/Postnummer") String manualOrgInfoZipCode,

	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag2/Postadress") String manualOrgInfoCity,

	@XPath("/FlowInstance/Values/KundensOrgUppgExterntForetag2/Kundens_referens") String manualOrgInfoReference,

	@XPath("/FlowInstance/Values/MotpartListaExterntForetag/Value") String manualOrgInfoMotpart

) {}
