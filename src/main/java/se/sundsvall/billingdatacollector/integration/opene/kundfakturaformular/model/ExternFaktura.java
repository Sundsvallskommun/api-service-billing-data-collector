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

	@XPath("/FlowInstance/Values/ReferensForetag/Value") String referenceOrganization) {}
