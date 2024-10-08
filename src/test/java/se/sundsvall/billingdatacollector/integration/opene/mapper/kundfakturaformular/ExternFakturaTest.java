package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternFakturaTest {

	@Test
	void constructorAndAccessors() {
		var familyId = "358";
		var flowInstanceId = "12345";
		var posted = "2024-09-20T15:28:23";
		var fornamn = "fornamn";
		var efternamn = "efternamn";
		var adress = "adress";
		var postnummer = "postnummer";
		var ort = "ort";
		var personnummer = "personnummer";
		var motpartNamn = "motpartNamn";
		var fakturaText = "fakturaText";
		var antal = 345;
		var aPris = "456,78";
		var summeringExtern = "157589,1";
		var momssats = "momssats";
		var ansvar = "ansvar";
		var underkonto = "underkonto";
		var verksamhet = "verksamhet";
		var aktivitetskonto = "aktivitetskonto";
		var objektkonto = "objektkonto";

		var externFaktura = new ExternFaktura(familyId, flowInstanceId, posted, fornamn, efternamn, adress, postnummer, ort,
			personnummer, motpartNamn, fakturaText, antal, aPris, summeringExtern, momssats, ansvar, underkonto,
			verksamhet, aktivitetskonto, objektkonto);

		assertThat(externFaktura.familyId()).isEqualTo(familyId);
		assertThat(externFaktura.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(externFaktura.fornamn()).isEqualTo(fornamn);
		assertThat(externFaktura.efternamn()).isEqualTo(efternamn);
		assertThat(externFaktura.adress()).isEqualTo(adress);
		assertThat(externFaktura.postnummer()).isEqualTo(postnummer);
		assertThat(externFaktura.ort()).isEqualTo(ort);
		assertThat(externFaktura.personnummer()).isEqualTo(personnummer);
		assertThat(externFaktura.motpartNamn()).isEqualTo(motpartNamn);
		assertThat(externFaktura.fakturaText()).isEqualTo(fakturaText);
		assertThat(externFaktura.antal()).isEqualTo(antal);
		assertThat(externFaktura.aPris()).isEqualTo(aPris);
		assertThat(externFaktura.summeringExtern()).isEqualTo(summeringExtern);
		assertThat(externFaktura.momssats()).isEqualTo(momssats);
		assertThat(externFaktura.ansvar()).isEqualTo(ansvar);
		assertThat(externFaktura.underkonto()).isEqualTo(underkonto);
		assertThat(externFaktura.verksamhet()).isEqualTo(verksamhet);
		assertThat(externFaktura.aktivitetskonto()).isEqualTo(aktivitetskonto);
		assertThat(externFaktura.objektkonto()).isEqualTo(objektkonto);
	}
}
