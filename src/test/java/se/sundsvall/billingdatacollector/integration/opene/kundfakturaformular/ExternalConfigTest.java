package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ExternalConfig.class)
class ExternalConfigTest {

	@Autowired
	private ExternalConfig config;

	@Test
	void testAktivitetskontoExtern() {
		var aktivitetskontoExtern = config.aktivitetskontoExtern();
		var aktivitetskontoExtern2 = config.aktivitetskontoExtern();
		assertThat(aktivitetskontoExtern).isNotNull();
		assertThat(aktivitetskontoExtern2).isNotNull();
		assertThat(aktivitetskontoExtern).isNotSameAs(aktivitetskontoExtern2);
	}

	@Test
	void testAnsvarExtern() {
		var ansvarExtern = config.ansvarExtern();
		var ansvarExtern2 = config.ansvarExtern();
		assertThat(ansvarExtern).isNotNull();
		assertThat(ansvarExtern2).isNotNull();
		assertThat(ansvarExtern).isNotSameAs(ansvarExtern2);
	}

	@Test
	void testBarakningarExtern() {
		var barakningarExtern = config.barakningarExtern();
		var barakningarExtern2 = config.barakningarExtern();
		assertThat(barakningarExtern).isNotNull();
		assertThat(barakningarExtern2).isNotNull();
		assertThat(barakningarExtern).isNotSameAs(barakningarExtern2);
	}

	@Test
	void testBerakningarExtern() {
		var berakningarExtern = config.berakningarExtern();
		var berakningarExtern2 = config.berakningarExtern();
		assertThat(berakningarExtern).isNotNull();
		assertThat(berakningarExtern2).isNotNull();
		assertThat(berakningarExtern).isNotSameAs(berakningarExtern2);
	}

	@Test
	void testBerakningExtern() {
		var berakningExtern = config.berakningExtern();
		var berakningExtern2 = config.berakningExtern();
		assertThat(berakningExtern).isNotNull();
		assertThat(berakningExtern2).isNotNull();
		assertThat(berakningExtern).isNotSameAs(berakningExtern2);
	}

	@Test
	void testMomssatsExtern() {
		var momssatsExtern = config.momssatsExtern();
		var momssatsExtern2 = config.momssatsExtern();
		assertThat(momssatsExtern).isNotNull();
		assertThat(momssatsExtern2).isNotNull();
		assertThat(momssatsExtern).isNotSameAs(momssatsExtern2);
	}

	@Test
	void testObjektkontoExtern() {
		var objektkontoExtern = config.objektkontoExtern();
		var objektkontoExtern2 = config.objektkontoExtern();
		assertThat(objektkontoExtern).isNotNull();
		assertThat(objektkontoExtern2).isNotNull();
		assertThat(objektkontoExtern).isNotSameAs(objektkontoExtern2);
	}

	@Test
	void testProjektkontoExtern() {
		var projektkontoExtern = config.projektkontoExtern();
		var projektkontoExtern2 = config.projektkontoExtern();
		assertThat(projektkontoExtern).isNotNull();
		assertThat(projektkontoExtern2).isNotNull();
		assertThat(projektkontoExtern).isNotSameAs(projektkontoExtern2);
	}

	@Test
	void testSummeringExtern() {
		var summeringExtern = config.summeringExtern();
		var summeringExtern2 = config.summeringExtern();
		assertThat(summeringExtern).isNotNull();
		assertThat(summeringExtern2).isNotNull();
		assertThat(summeringExtern).isNotSameAs(summeringExtern2);
	}

	@Test
	void testUnderkontoExtern() {
		var underkontoExtern = config.underkontoExtern();
		var underkontoExtern2 = config.underkontoExtern();
		assertThat(underkontoExtern).isNotNull();
		assertThat(underkontoExtern2).isNotNull();
		assertThat(underkontoExtern).isNotSameAs(underkontoExtern2);
	}

	@Test
	void testVerksamhetExtern() {
		var verksamhetExtern = config.verksamhetExtern();
		var verksamhetExtern2 = config.verksamhetExtern();
		assertThat(verksamhetExtern).isNotNull();
		assertThat(verksamhetExtern2).isNotNull();
		assertThat(verksamhetExtern).isNotSameAs(verksamhetExtern2);
	}
}
