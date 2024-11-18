package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.AktivitetskontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.AnsvarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BarakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.MomssatsExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.ObjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.ProjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.SummeringExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.UnderkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.VerksamhetExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.AktivitetskontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.AnsvarIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningarIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.ProjektkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.SummeringIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.UnderkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.VerksamhetIntern;

class OpeneCollectionsTest {

	@Test
	void testAddBerakningarIntern_shouldAddToBerakningIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, BerakningarIntern.builder().build());

		assertThat(openeCollections.getBerakningInternMap()).hasSize(1);
	}

	@Test
	void testAddBarakningarExtern_shouldAddToBerakningExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, BarakningarExtern.builder().build());

		assertThat(openeCollections.getBerakningExternMap()).hasSize(1);
	}

	@Test
	void testAddBerakningarExtern_shouldAddToBerakningExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, BerakningarExtern.builder().build());

		assertThat(openeCollections.getBerakningExternMap()).hasSize(1);
	}

	@Test
	void testAddUnknownObject_shouldThrowException() {
		OpeneCollections openeCollections = new OpeneCollections();
		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> openeCollections.add(1, new Object()))
			.satisfies(exception -> assertThat(exception).hasMessage("Unsupported object type: java.lang.Object"));
	}

	@Test
	void testGetNumberOfRows() {

		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, AktivitetskontoIntern.builder().build());
		openeCollections.add(1, BerakningarIntern.builder().build());

		assertThat(openeCollections.getNumberOfRows()).isEqualTo(1);
	}

	@Test
	void testGetNumberOfRows_shouldThrowException_whenDiverging() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, AktivitetskontoIntern.builder().build());
		openeCollections.add(1, BerakningarIntern.builder().build());
		openeCollections.add(2, BerakningarIntern.builder().build());

		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> openeCollections.getNumberOfRows())
			.satisfies(exception -> assertThat(exception).hasMessage("Mismatch in the number of iterations in the maps: [0, 1, 2]"));
	}

	@Test
	void testAddAktivitetskontoIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, AktivitetskontoIntern.builder().build());

		assertThat(openeCollections.getAktivitetskontoInternMap()).hasSize(1);
	}

	@Test
	void testAddAnsvarInternMap() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, AnsvarIntern.builder().build());

		assertThat(openeCollections.getAnsvarInternMap()).hasSize(1);
	}

	@Test
	void testAddBerakningIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, BerakningIntern.builder().build());

		assertThat(openeCollections.getBerakningInternMap()).hasSize(1);
	}

	@Test
	void testAddProjektkontoIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, ProjektkontoIntern.builder().build());

		assertThat(openeCollections.getProjektkontoInternMap()).hasSize(1);
	}

	@Test
	void testAddSummeringIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, SummeringIntern.builder().build());

		assertThat(openeCollections.getSummeringInternMap()).hasSize(1);
	}

	@Test
	void testAddUnderkontoIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, UnderkontoIntern.builder().build());

		assertThat(openeCollections.getUnderkontoInternMap()).hasSize(1);
	}

	@Test
	void testAddVerksamhetIntern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, VerksamhetIntern.builder().build());

		assertThat(openeCollections.getVerksamhetInternMap()).hasSize(1);
	}

	@Test
	void testAddAktivitetskontoExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, AktivitetskontoExtern.builder().build());

		assertThat(openeCollections.getAktivitetskontoExternMap()).hasSize(1);
	}

	@Test
	void testAddAnsvarExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, AnsvarExtern.builder().build());

		assertThat(openeCollections.getAnsvarExternMap()).hasSize(1);
	}

	@Test
	void testAddBerakningExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, BerakningExtern.builder().build());

		assertThat(openeCollections.getBerakningExternMap()).hasSize(1);
	}

	@Test
	void testAddMomssatsExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, MomssatsExtern.builder().build());

		assertThat(openeCollections.getMomssatsExternMap()).hasSize(1);
	}

	@Test
	void testAddObjektkontoExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, ObjektkontoExtern.builder().build());

		assertThat(openeCollections.getObjektKontoExternMap()).hasSize(1);
	}

	@Test
	void testAddProjektkontoExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, ProjektkontoExtern.builder().build());

		assertThat(openeCollections.getProjektkontoExternMap()).hasSize(1);
	}

	@Test
	void testAddSummeringExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, SummeringExtern.builder().build());

		assertThat(openeCollections.getSummeringExternMap()).hasSize(1);
	}

	@Test
	void testAddUnderkontoExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, UnderkontoExtern.builder().build());

		assertThat(openeCollections.getUnderkontoExternMap()).hasSize(1);
	}

	@Test
	void testAddVerksamhetExtern() {
		OpeneCollections openeCollections = new OpeneCollections();
		openeCollections.add(1, VerksamhetExtern.builder().build());

		assertThat(openeCollections.getVerksamhetExternMap()).hasSize(1);
	}

	@Test
	void testNewOpenECollection_shouldOnlyHaveEmptyMaps() {
		OpeneCollections openeCollections = new OpeneCollections();

		assertThat(openeCollections.getAktivitetskontoInternMap()).isEmpty();
		assertThat(openeCollections.getAnsvarInternMap()).isEmpty();
		assertThat(openeCollections.getBerakningInternMap()).isEmpty();
		assertThat(openeCollections.getProjektkontoInternMap()).isEmpty();
		assertThat(openeCollections.getSummeringInternMap()).isEmpty();
		assertThat(openeCollections.getUnderkontoInternMap()).isEmpty();
		assertThat(openeCollections.getVerksamhetInternMap()).isEmpty();

		assertThat(openeCollections.getAktivitetskontoExternMap()).isEmpty();
		assertThat(openeCollections.getAnsvarExternMap()).isEmpty();
		assertThat(openeCollections.getBerakningExternMap()).isEmpty();
		assertThat(openeCollections.getMomssatsExternMap()).isEmpty();
		assertThat(openeCollections.getObjektKontoExternMap()).isEmpty();
		assertThat(openeCollections.getProjektkontoExternMap()).isEmpty();
		assertThat(openeCollections.getSummeringExternMap()).isEmpty();
		assertThat(openeCollections.getUnderkontoExternMap()).isEmpty();
		assertThat(openeCollections.getVerksamhetExternMap()).isEmpty();
	}
}
