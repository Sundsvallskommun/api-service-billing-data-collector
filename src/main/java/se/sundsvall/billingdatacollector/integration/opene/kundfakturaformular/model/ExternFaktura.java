package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import se.sundsvall.billingdatacollector.integration.opene.util.annotation.XPath;

public record ExternFaktura(

	@XPath("/FlowInstance/Header/Flow/FamilyID") String familyId,

	@XPath("/FlowInstance/Header/FlowInstanceID") String flowInstanceId,

	@XPath("/FlowInstance/Header/Posted") String posted,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Firstname") String fornamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Lastname") String efternamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Address") String adress,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/ZipCode") String postnummer,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/PostalAddress") String ort,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/SocialSecurityNumber") String personnummer,

	@XPath("/FlowInstance/Values/MotpartPrivatperson/Name") String motpartNamn,

	@XPath("/FlowInstance/Values/SaljarensUppg/Firstname") String saljarensFornamn,

	@XPath("/FlowInstance/Values/SaljarensUppg/Lastname") String saljarensEfternamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Firstname") String kontaktuppgifterPrivatpersonFornamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Lastname") String kontaktuppgifterPrivatpersonEfternamn,

	@XPath("/FlowInstance/Values/OrganisationsnummerExtForetagRD/Value") String organisationsInformation,

	@XPath("/FlowInstance/Values/ReferensForetag/Value") String referensForetag) {}
