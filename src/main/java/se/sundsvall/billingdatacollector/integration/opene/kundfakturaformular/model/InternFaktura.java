package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import se.sundsvall.billingdatacollector.integration.opene.util.annotation.XPath;

public record InternFaktura(

	@XPath("/FlowInstance/Header/Flow/FamilyID") String familyId,

	@XPath("/FlowInstance/Header/FlowInstanceID") String flowInstanceId,

	@XPath("/FlowInstance/Header/Posted") String posted,

	@XPath("/FlowInstance/Values/FakturanSkickasTill/Value") String sendInvoiceTo,

	@XPath("/FlowInstance/Values/ForvaltningSomSkaBetala/Value") String payingAdministration,

	@XPath("/FlowInstance/Values/SaljarensUppg/Firstname") String sellerInformationFirstName,

	@XPath("/FlowInstance/Values/SaljarensUppg/Lastname") String sellerInformationLastName,

	@XPath("/FlowInstance/Values/RefNrSundsvallsKommun/Value") String referenceSundsvallsMunicipality) {}
