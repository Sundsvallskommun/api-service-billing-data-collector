package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import se.sundsvall.billingdatacollector.integration.opene.util.annotation.XPath;

record InternFaktura(

	@XPath("/FlowInstance/Header/Flow/FamilyID")
	String familyId,

	@XPath("/FlowInstance/Header/FlowInstanceID")
	String flowInstanceId,

	@XPath("/FlowInstance/Values/FakturanSkickasTill/Value")
	String fakturanSkickasTill,

	@XPath("/FlowInstance/Values/ForvaltningSomSkaBetala/Value")
	String forvaltningSomSkaBetala,

	@XPath("/FlowInstance/Values/BerakningIntern1/FakturatextIntern1")
	String fakturaText,

	@XPath("/FlowInstance/Values/BerakningIntern1/AntalIntern1")
	Integer antal,

	@XPath("/FlowInstance/Values/BerakningIntern1/APrisIntern1")
	String aPris,

	@XPath("/FlowInstance/Values/SummeringIntern1/TotSummeringIntern1")
	String summering,

	@XPath("/FlowInstance/Values/AnsvarIntern1/Value")
	String ansvar,

	@XPath("/FlowInstance/Values/UnderkontoIntern1/Value")
	String underkonto,

	@XPath("/FlowInstance/Values/VerksamhetIntern1/Value")
	String verksamhet,

	@XPath("/FlowInstance/Values/AktivitetskontoIntern1/Value")
	String aktivitetskonto
	) { }
