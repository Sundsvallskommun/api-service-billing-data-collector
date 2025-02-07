package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BarakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningarIntern;

public final class OpeneCollectionsMapper {

	private OpeneCollectionsMapper() {
		// Shouldn't be instantiated
	}

	/**
	 * @param  berakningarExtern the object to consolidate
	 * @return                   a BerakningExtern object
	 */
	public static BerakningExtern consolidateBerakningarExternWithBerakningExtern(BerakningarExtern berakningarExtern) {
		return BerakningExtern.builder()
			.withAntalExtern(berakningarExtern.getAntalExtern())
			.withAPrisExtern(berakningarExtern.getAPrisExtern())
			.withFakturatextExtern(berakningarExtern.getFakturatextExtern())
			.withName(berakningarExtern.getName())
			.withQueryID(berakningarExtern.getQueryID())
			.build();
	}

	/**
	 * Special case since OpenE names the first iteration to BerakningIntern1 and the second one to BerakningarIntern[x]
	 * Consolidate the two into one and only use the BerakningIntern name
	 *
	 * @param  berakningarIntern the object to consolidate
	 * @return                   a BerakningExtern object
	 */
	public static BerakningIntern consolidateBerakningarWithBerakningIntern(BerakningarIntern berakningarIntern) {
		return BerakningIntern.builder()
			.withAntalIntern(berakningarIntern.getAntalIntern())
			.withAPrisIntern(berakningarIntern.getAPrisIntern())
			.withFakturatextIntern(berakningarIntern.getFakturatextIntern())
			.withName(berakningarIntern.getName())
			.withQueryID(berakningarIntern.getQueryID())
			.build();
	}

	/**
	 * Special case since OpenE names the first iteration to BerakningExtern1 and the second one to BarakningarExtern[x]
	 * Consolidate the two into one and only use the BarakningExtern name
	 *
	 * @param  barakningarExtern the object to consolidate
	 * @return                   a BerakningExtern object
	 */
	public static BerakningExtern consolidateBarakningarExternWithBerakningExtern(BarakningarExtern barakningarExtern) {
		// Create a new BerakningExtern object
		return BerakningExtern.builder()
			.withAntalExtern(barakningarExtern.getAntalExtern())
			.withAPrisExtern(barakningarExtern.getAPrisExtern())
			.withFakturatextExtern(barakningarExtern.getFakturatextExtern())
			.withName(barakningarExtern.getName())
			.withQueryID(barakningarExtern.getQueryID())
			.build();
	}
}
