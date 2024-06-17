package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InternFakturaTest {

	@Test
	void constructorAndAccessors() {
		var familyId = "358";
		var flowInstanceId = "12345";
		var fakturanSkickasTill = "fakturanSkickasTill";
		var forvaltningSomSkaBetala = "forvaltningSomSkaBetala";
		var fakturaText = "fakturaText";
		var antal = 123;
		var aPris = "123,45";
		var summering = "15184,35";
		var ansvar = "ansvar";
		var underkonto = "underkonto";
		var verksamhet = "verksamhet";
		var aktivitetskonto = "aktivitetskonto";

		var internFaktura = new InternFaktura(familyId, flowInstanceId, fakturanSkickasTill, forvaltningSomSkaBetala,
			fakturaText, antal, aPris, summering, ansvar, underkonto, verksamhet, aktivitetskonto);

		assertThat(internFaktura.familyId()).isEqualTo(familyId);
		assertThat(internFaktura.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(internFaktura.fakturanSkickasTill()).isEqualTo(fakturanSkickasTill);
		assertThat(internFaktura.forvaltningSomSkaBetala()).isEqualTo(forvaltningSomSkaBetala);
		assertThat(internFaktura.fakturaText()).isEqualTo(fakturaText);
		assertThat(internFaktura.antal()).isEqualTo(antal);
		assertThat(internFaktura.aPris()).isEqualTo(aPris);
		assertThat(internFaktura.summering()).isEqualTo(summering);
		assertThat(internFaktura.ansvar()).isEqualTo(ansvar);
		assertThat(internFaktura.underkonto()).isEqualTo(underkonto);
		assertThat(internFaktura.verksamhet()).isEqualTo(verksamhet);
		assertThat(internFaktura.aktivitetskonto()).isEqualTo(aktivitetskonto);
	}
}
