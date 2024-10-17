package se.sundsvall.billingdatacollector.integration.opene.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sundsvall.billingdatacollector.integration.opene.model.external.AktivitetskontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.AnsvarExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.BarakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.BerakningExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.MomssatsExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.ObjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.ProjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.SummeringExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.UnderkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.external.VerksamhetExtern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.AktivitetskontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.AnsvarIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.BerakningarIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.ProjektkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.SummeringIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.UnderkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.model.internal.VerksamhetIntern;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpeneCollections {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpeneCollections.class);

	// Internal
	private HashMap<Integer, AktivitetskontoIntern> aktivitetskontoInternMap = new HashMap<>();
	private HashMap<Integer, AnsvarIntern> ansvarInternMap = new HashMap<>();
	private HashMap<Integer, BerakningIntern> berakningInternMap = new HashMap<>();
	private HashMap<Integer, ProjektkontoIntern> projektkontoInternMap = new HashMap<>();
	private HashMap<Integer, SummeringIntern> summeringInternMap = new HashMap<>();
	private HashMap<Integer, UnderkontoIntern> underkontoInternMap = new HashMap<>();
	private HashMap<Integer, VerksamhetIntern> verksamhetInternMap = new HashMap<>();

	// External
	private HashMap<Integer, AktivitetskontoExtern> aktivitetskontoExternMap = new HashMap<>();
	private HashMap<Integer, AnsvarExtern> ansvarExternMap = new HashMap<>();
	//private HashMap<Integer, BarakningarExtern> barakningarExternMap = new HashMap<>();
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
			// Special case, don't actually use the "BerakningarIntern", consolidate instead
			case BerakningarIntern berakningarIntern -> berakningInternMap.put(index, consolidateBerakningarWithBerakningIntern(berakningarIntern));
			case BerakningIntern berakningIntern -> berakningInternMap.put(index, berakningIntern);
			case ProjektkontoIntern projektkontoIntern -> projektkontoInternMap.put(index, projektkontoIntern);
			case SummeringIntern summeringIntern -> summeringInternMap.put(index, summeringIntern);
			case UnderkontoIntern underkontoIntern -> underkontoInternMap.put(index, underkontoIntern);
			case VerksamhetIntern verksamhetIntern -> verksamhetInternMap.put(index, verksamhetIntern);
			// All external objects
			case AktivitetskontoExtern aktivitetskontoExtern -> aktivitetskontoExternMap.put(index, aktivitetskontoExtern);
			case AnsvarExtern ansvarExtern -> ansvarExternMap.put(index, ansvarExtern);
			// Also special, don't use BarakningarExtern, consolidate instead
			case BarakningarExtern barakningar -> berakningExternMap.put(index, consolirateBerakningarWithBarakningExtern(barakningar));
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
	 * Special case since OpenE names the first iteration to BerakningIntern1 and the second one to BerakningarIntern[x]
	 * Consolidate the two into one and only use the BerakningIntern name
	 * @param berakningarIntern the object to consolidate
	 * @return the consolidated object
	 */
	private BerakningIntern consolidateBerakningarWithBerakningIntern(BerakningarIntern berakningarIntern) {
		// Create a new BerakningIntern object
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
	 * @param barakningarExtern the object to consolidate
	 * @return the consolidated object
	 */
	private BerakningExtern consolirateBerakningarWithBarakningExtern(BarakningarExtern barakningarExtern) {
		// Create a new BerakningExtern object
		return BerakningExtern.builder()
			.withAntalExtern(barakningarExtern.getAntalExtern())
			.withAPrisExtern(barakningarExtern.getAPrisExtern())
			.withFakturatextExtern(barakningarExtern.getFakturatextExtern())
			.withName(barakningarExtern.getName())
			.withQueryID(barakningarExtern.getQueryID())
			.build();
	}

	/**
	 * Used to determine how many iterations there are in the maps, used in KundfakturaformularMapper to determine how many rows to loop over
	 * @return the number of rows in the maps
	 */
	public int getNumberOfRows() {
		//Loop through all maps and get the size of each map
		Set<Integer> sizes = new HashSet<>(List.of(
			aktivitetskontoInternMap.size(),
			ansvarInternMap.size(),
			berakningInternMap.size(),
			projektkontoInternMap.size(),
			summeringInternMap.size(),
			underkontoInternMap.size(),
			verksamhetInternMap.size(),
			aktivitetskontoExternMap.size(),
			ansvarExternMap.size(),
			berakningExternMap.size(),
			momssatsExternMap.size(),
			objektKontoExternMap.size(),
			projektkontoExternMap.size(),
			summeringExternMap.size(),
			underkontoExternMap.size(),
			verksamhetExternMap.size()
		));

		// We only want two sizes, any more means we have missing information
		if (sizes.size() == 2) {
			//Get the highest number of rows
			return sizes.stream().max(Integer::compareTo).orElse(0);
		} else {
			LOGGER.error("Got mismatch in the number of rows in the maps: {}", sizes);
			throw new IllegalArgumentException("Missing information in the mapping");
		}
	}
}
