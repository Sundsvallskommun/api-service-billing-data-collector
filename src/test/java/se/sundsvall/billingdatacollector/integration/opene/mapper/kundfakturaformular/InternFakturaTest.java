package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.InternFaktura;

class InternFakturaTest {

	@Test
	void constructorAndAccessors() {
		var familyId = "358";
		var flowInstanceId = "12345";
		var posted = "2023-11-24T15:40:23";
		var fakturanSkickasTill = "fakturanSkickasTill";
		var forvaltningSomSkaBetala = "forvaltningSomSkaBetala";
		var saljarensFornamn = "saljarensFornamn";
		var saljarensEfternamn = "saljarensEfternamn";
		var internReferens = "internReferens";

		var internFaktura = new InternFaktura(familyId, flowInstanceId, posted, fakturanSkickasTill, forvaltningSomSkaBetala,
			saljarensFornamn, saljarensEfternamn, internReferens);

		assertThat(internFaktura.familyId()).isEqualTo(familyId);
		assertThat(internFaktura.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(internFaktura.fakturanSkickasTill()).isEqualTo(fakturanSkickasTill);
		assertThat(internFaktura.forvaltningSomSkaBetala()).isEqualTo(forvaltningSomSkaBetala);
		assertThat(internFaktura.saljarensFornamn()).isEqualTo(saljarensFornamn);
		assertThat(internFaktura.saljarensEfternamn()).isEqualTo(saljarensEfternamn);
		assertThat(internFaktura.internReferens()).isEqualTo(internReferens);
	}
}
