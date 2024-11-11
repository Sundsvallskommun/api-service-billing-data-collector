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
		var fornamn = "fornamn";
		var efternamn = "efternamn";
		var adress = "adress";
		var postnummer = "postnummer";
		var ort = "ort";
		var personnummer = "personnummer";
		var motpartNamn = "motpartNamn";

		var saljarensFornamn = "saljarensFornamn";
		var saljarensEfternamn = "saljarensEfternamn";
		var kontaktuppgifterPrivatpersonFornamn = "Name";
		var kontaktuppgifterPrivatpersonEfternamn = "Namesson";
		var organisationsNummer = "1234567890";
		var referensForetag = "referensForetag";

		var externFaktura = new ExternFaktura(familyId, flowInstanceId, posted, fornamn, efternamn, adress, postnummer, ort,
			personnummer, motpartNamn, saljarensFornamn, saljarensEfternamn, kontaktuppgifterPrivatpersonFornamn,
			kontaktuppgifterPrivatpersonEfternamn, organisationsNummer, referensForetag);

		assertThat(externFaktura.familyId()).isEqualTo(familyId);
		assertThat(externFaktura.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(externFaktura.fornamn()).isEqualTo(fornamn);
		assertThat(externFaktura.efternamn()).isEqualTo(efternamn);
		assertThat(externFaktura.adress()).isEqualTo(adress);
		assertThat(externFaktura.postnummer()).isEqualTo(postnummer);
		assertThat(externFaktura.ort()).isEqualTo(ort);
		assertThat(externFaktura.personnummer()).isEqualTo(personnummer);
		assertThat(externFaktura.motpartNamn()).isEqualTo(motpartNamn);
		assertThat(externFaktura.saljarensFornamn()).isEqualTo(saljarensFornamn);
		assertThat(externFaktura.saljarensEfternamn()).isEqualTo(saljarensEfternamn);
		assertThat(externFaktura.kontaktuppgifterPrivatpersonFornamn()).isEqualTo(kontaktuppgifterPrivatpersonFornamn);
		assertThat(externFaktura.kontaktuppgifterPrivatpersonEfternamn()).isEqualTo(kontaktuppgifterPrivatpersonEfternamn);
		assertThat(externFaktura.organisationsInformation()).isEqualTo(organisationsNummer);
		assertThat(externFaktura.referensForetag()).isEqualTo(referensForetag);
	}
}
