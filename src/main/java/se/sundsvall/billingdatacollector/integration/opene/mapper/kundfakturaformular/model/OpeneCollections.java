package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.AktivitetskontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.AnsvarExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.BarakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.BerakningExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.MomssatsExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.ObjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.ProjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.SummeringExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.UnderkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.VerksamhetExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.AktivitetskontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.AnsvarIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.BerakningarIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.ProjektkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.SummeringIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.UnderkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.VerksamhetIntern;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpeneCollections {

	// Internal
	private HashMap<Integer, AktivitetskontoIntern> aktivitetskontoInternMap = new HashMap<>();
	private HashMap<Integer, AnsvarIntern> ansvarInternMap = new HashMap<>();
	private HashMap<Integer, BerakningarIntern> berakningarInternMap = new HashMap<>();
	private HashMap<Integer, BerakningIntern> berakningInternMap = new HashMap<>();
	private HashMap<Integer, ProjektkontoIntern> projektkontoInternMap = new HashMap<>();
	private HashMap<Integer, SummeringIntern> summeringInternMap = new HashMap<>();
	private HashMap<Integer, UnderkontoIntern> underkontoInternMap = new HashMap<>();
	private HashMap<Integer, VerksamhetIntern> verksamhetInternMap = new HashMap<>();

	// External
	private HashMap<Integer, AktivitetskontoExtern> aktivitetskontoExternMap = new HashMap<>();
	private HashMap<Integer, AnsvarExtern> ansvarExternMap = new HashMap<>();
	private HashMap<Integer, BarakningarExtern> barakningarExternMap = new HashMap<>();
	private HashMap<Integer, BerakningExtern> berakningExternMap = new HashMap<>();
	private HashMap<Integer, MomssatsExtern> momssatsExternMap = new HashMap<>();
	private HashMap<Integer, ObjektkontoExtern> objektKontoExternMap = new HashMap<>();
	private HashMap<Integer, ProjektkontoExtern> projektkontoExternMap = new HashMap<>();
	private HashMap<Integer, SummeringExtern> summeringExternMap = new HashMap<>();
	private HashMap<Integer, UnderkontoExtern> underkontoExternMap = new HashMap<>();
	private HashMap<Integer, VerksamhetExtern> verksamhetExternMap = new HashMap<>();

	// Add to the correct hashmap based on the type of the object
	public void add(Integer index, Object object) {
		switch (object) {
			// All internal objects
			case AktivitetskontoIntern aktivitetskontoIntern -> aktivitetskontoInternMap.put(index, aktivitetskontoIntern);
			case AnsvarIntern ansvarIntern -> ansvarInternMap.put(index, ansvarIntern);
			case BerakningarIntern berakningarIntern -> berakningarInternMap.put(index, berakningarIntern);
			case BerakningIntern berakningIntern -> berakningInternMap.put(index, berakningIntern);
			case ProjektkontoIntern projektkontoIntern -> projektkontoInternMap.put(index, projektkontoIntern);
			case SummeringIntern summeringIntern -> summeringInternMap.put(index, summeringIntern);
			case UnderkontoIntern underkontoIntern -> underkontoInternMap.put(index, underkontoIntern);
			case VerksamhetIntern verksamhetIntern -> verksamhetInternMap.put(index, verksamhetIntern);
			// All external objects
			case AktivitetskontoExtern aktivitetskontoExtern -> aktivitetskontoExternMap.put(index, aktivitetskontoExtern);
			case AnsvarExtern ansvarExtern -> ansvarExternMap.put(index, ansvarExtern);
			case BarakningarExtern barakningar -> barakningarExternMap.put(index, barakningar);
			case BerakningExtern berakningExtern -> berakningExternMap.put(index, berakningExtern);
			case MomssatsExtern momssatsExtern -> momssatsExternMap.put(index, momssatsExtern);
			case ObjektkontoExtern objektKontoExtern -> objektKontoExternMap.put(index, objektKontoExtern);
			case ProjektkontoExtern projektkontoExtern -> projektkontoExternMap.put(index, projektkontoExtern);
			case SummeringExtern summeringExtern -> summeringExternMap.put(index, summeringExtern);
			case UnderkontoExtern underkontoExtern -> underkontoExternMap.put(index, underkontoExtern);
			case VerksamhetExtern verksamhetExtern -> verksamhetExternMap.put(index, verksamhetExtern);
			default -> throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
		}
	}

	/**
	 * Count the max number of entries in the internal and external maps.
	 * @return the number of rows in the maps
	 */
	public int getNumberOfRows() {
		//Loop through all maps and get the size of each map
		Set<Integer> sizes = new HashSet<>(List.of(
			aktivitetskontoInternMap.size(),
			ansvarInternMap.size(),
			berakningarInternMap.size(),
			berakningInternMap.size(),
			projektkontoInternMap.size(),
			summeringInternMap.size(),
			underkontoInternMap.size(),
			verksamhetInternMap.size(),
			aktivitetskontoExternMap.size(),
			ansvarExternMap.size(),
			barakningarExternMap.size(),
			berakningExternMap.size(),
			momssatsExternMap.size(),
			objektKontoExternMap.size(),
			projektkontoExternMap.size(),
			summeringExternMap.size(),
			underkontoExternMap.size(),
			verksamhetExternMap.size()
		));

		// Return the max size of the set
		return sizes.stream().max(Integer::compareTo).orElse(0);
	}
}
