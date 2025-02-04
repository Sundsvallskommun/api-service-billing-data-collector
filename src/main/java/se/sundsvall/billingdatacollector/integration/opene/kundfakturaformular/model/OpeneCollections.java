package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.OpeneCollectionsMapper.consolidateBarakningarExternWithBerakningExtern;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.OpeneCollectionsMapper.consolidateBerakningarExternWithBerakningExtern;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.OpeneCollectionsMapper.consolidateBerakningarWithBerakningIntern;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
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

/**
 * A class to hold all the different collections of objects from OpenE since there are no schema for them.
 */
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

			// Special cases, don't actually use the "BerakningarIntern", consolidate instead
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
			case BarakningarExtern barakningarExtern -> berakningExternMap.put(index, consolidateBarakningarExternWithBerakningExtern(barakningarExtern));

			// And consolidate here as well
			case BerakningarExtern berakningarExtern -> berakningExternMap.put(index, consolidateBerakningarExternWithBerakningExtern(berakningarExtern));
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
	 * Used to determine how many iterations there are in the maps, which is later used in
	 * KundfakturaformularMapper to determine how many rows to loop over.
	 * If no rows are found, an exception is thrown.
	 * 
	 * @return The number of rows in the maps
	 */
	public int getNumberOfRows() {
		// Get the number of iterations, if none, return 0
		int rows = getAllMaps(this).values().stream()
			.mapToInt(Map::size)
			.max()
			.orElse(0);

		if (rows == 0) {
			throw Problem.builder()
				.withStatus(Status.INTERNAL_SERVER_ERROR)
				.withTitle("No data from OpenE")
				.build();
		}

		return rows;

	}

	private static Map<String, HashMap<?, ?>> getAllMaps(OpeneCollections openeCollections) {
		Map<String, HashMap<?, ?>> maps = new HashMap<>();
		Field[] fields = OpeneCollections.class.getDeclaredFields();

		for (Field field : fields) {
			if (HashMap.class.isAssignableFrom(field.getType())) {
				field.setAccessible(true);
				try {
					maps.put(field.getName(), (HashMap<?, ?>) field.get(openeCollections));
				} catch (IllegalAccessException e) {
					LOGGER.error("Could not access field: {}", field.getName(), e);
				}
			}
		}
		return maps;
	}
}
