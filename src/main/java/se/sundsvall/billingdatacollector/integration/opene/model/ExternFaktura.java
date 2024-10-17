package se.sundsvall.billingdatacollector.integration.opene.model;

import se.sundsvall.billingdatacollector.integration.opene.util.annotation.XPath;

public record ExternFaktura(

	@XPath("/FlowInstance/Header/Flow/FamilyID")
	String familyId,

	@XPath("/FlowInstance/Header/FlowInstanceID")
	String flowInstanceId,

	@XPath("/FlowInstance/Header/Posted")
	String posted,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Firstname")
	String fornamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Lastname")
	String efternamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Address")
	String adress,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/ZipCode")
	String postnummer,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/PostalAddress")
	String ort,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/SocialSecurityNumber")
	String personnummer,

	@XPath("/FlowInstance/Values/MotpartPrivatperson/Name")
	String motpartNamn,

	@XPath("/FlowInstance/Values/SaljarensUppg/Firstname")
	String saljarensFornamn,

	@XPath("/FlowInstance/Values/SaljarensUppg/Lastname")
	String saljarensEfternamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Firstname")
	String kontaktuppgifterPrivatpersonFornamn,

	@XPath("/FlowInstance/Values/KontaktuppgifterPrivatperson/Lastname")
	String kontaktuppgifterPrivatpersonEfternamn

	/*
	@XPath("/FlowInstance/Values/ObjektkontoExtern1/Value")
	String objektkonto,

	@XPath("/FlowInstance/Values/BarakningarExtern1/FakturatextExtern1")
	String fakturaText,

	@XPath("/FlowInstance/Values/BarakningarExtern1/AntalExtern1")
	Integer antal,

	@XPath("/FlowInstance/Values/BarakningarExtern1/APrisExtern1")
	String aPris,

	@XPath("/FlowInstance/Values/SummeringExtern1/TotSummeringExtern1")
	String summeringExtern,

	@XPath("/FlowInstance/Values/MomssatsExtern1/Value")
	String momssats,

	@XPath("/FlowInstance/Values/AnsvarExtern1/Value")
	String ansvar,

	@XPath("/FlowInstance/Values/UnderkontoExtern1/Value")
	String underkonto,

	@XPath("/FlowInstance/Values/VerksamhetExtern1/Value")
	String verksamhet,

	@XPath("/FlowInstance/Values/AktivitetskontoExtern1/Value")
	String aktivitetskonto,*/
	) { }
