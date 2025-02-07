package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = InternalConfig.class)
class InternalConfigTest {

	@Autowired
	private InternalConfig config;

	@Test
	void testAktivitetskontoIntern() {
		var aktivitetskontoIntern = config.aktivitetskontoIntern();
		var aktivitetskontoIntern2 = config.aktivitetskontoIntern();
		assertThat(aktivitetskontoIntern).isNotNull();
		assertThat(aktivitetskontoIntern2).isNotNull();
		assertThat(aktivitetskontoIntern).isNotSameAs(aktivitetskontoIntern2);
	}

	@Test
	void testAnsvarIntern() {
		var ansvarIntern = config.ansvarIntern();
		var ansvarIntern2 = config.ansvarIntern();
		assertThat(ansvarIntern).isNotNull();
		assertThat(ansvarIntern2).isNotNull();
		assertThat(ansvarIntern).isNotSameAs(ansvarIntern2);
	}

	@Test
	void testBerakningarIntern() {
		var berakningarIntern = config.berakningarIntern();
		var berakningarIntern2 = config.berakningarIntern();
		assertThat(berakningarIntern).isNotNull();
		assertThat(berakningarIntern2).isNotNull();
		assertThat(berakningarIntern).isNotSameAs(berakningarIntern2);
	}

	@Test
	void testBerakningIntern() {
		var berakningIntern = config.berakningIntern();
		var berakningIntern2 = config.berakningIntern();
		assertThat(berakningIntern).isNotNull();
		assertThat(berakningIntern2).isNotNull();
		assertThat(berakningIntern).isNotSameAs(berakningIntern2);
	}

	@Test
	void testProjektkontoIntern() {
		var projektkontoIntern = config.projektkontoIntern();
		var projektkontoIntern2 = config.projektkontoIntern();
		assertThat(projektkontoIntern).isNotNull();
		assertThat(projektkontoIntern2).isNotNull();
		assertThat(projektkontoIntern).isNotSameAs(projektkontoIntern2);
	}

	@Test
	void testSummeringIntern() {
		var summeringIntern = config.summeringIntern();
		var summeringIntern2 = config.summeringIntern();
		assertThat(summeringIntern).isNotNull();
		assertThat(summeringIntern2).isNotNull();
		assertThat(summeringIntern).isNotSameAs(summeringIntern2);
	}

	@Test
	void testUnderkontoIntern() {
		var underkontoIntern = config.underkontoIntern();
		var underkontoIntern2 = config.underkontoIntern();
		assertThat(underkontoIntern).isNotNull();
		assertThat(underkontoIntern2).isNotNull();
		assertThat(underkontoIntern).isNotSameAs(underkontoIntern2);
	}

	@Test
	void testVerksamhetIntern() {
		var verksamhetIntern = config.verksamhetIntern();
		var verksamhetIntern2 = config.verksamhetIntern();
		assertThat(verksamhetIntern).isNotNull();
		assertThat(verksamhetIntern2).isNotNull();
		assertThat(verksamhetIntern).isNotSameAs(verksamhetIntern2);
	}
}
