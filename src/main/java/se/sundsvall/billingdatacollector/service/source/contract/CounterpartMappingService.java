package se.sundsvall.billingdatacollector.service.source.contract;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.billingdatacollector.integration.db.CounterpartMappingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.CounterpartMappingEntity;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.dept44.problem.Problem;

import static java.util.function.Predicate.not;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
public class CounterpartMappingService {

	private static final Logger LOG = LoggerFactory.getLogger(CounterpartMappingService.class);

	private final CounterpartMappingRepository repository;
	private final PartyIntegration partyIntegration;

	public CounterpartMappingService(CounterpartMappingRepository repository, PartyIntegration partyIntegration) {
		this.repository = repository;
		this.partyIntegration = partyIntegration;
	}

	/**
	 * Find the counterpart value for a given legalId and stakeholder type.
	 * Lookup priority:
	 * 1. Pattern matching on legalId
	 * 2. Stakeholder type fallback
	 *
	 * @param  partyId                                      the partyId to find legalId for
	 * @param  stakeholderType                              the stakeholder type for fallback matching
	 * @return                                              the counterpart value
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem if no match found
	 */
	public String findCounterpart(String municipalityId, String partyId, String stakeholderType) {
		LOG.debug("Finding counterpart for partyId: {}, stakeholderType: {}", partyId, stakeholderType);

		final var legalId = partyIntegration.getLegalId(municipalityId, partyId);

		var counterpartMappingMatch = findBestMatch(legalId, repository.findAll());

		if (counterpartMappingMatch != null) {
			return counterpartMappingMatch.getCounterpart();
		}

		// Fall back to stakeholder type
		if (stakeholderType != null) {
			var typeMatch = repository.findByStakeholderType(stakeholderType);
			if (typeMatch.isPresent()) {
				return typeMatch.get().getCounterpart();
			}
		}

		LOG.warn("No counterpart found for partyId: {}, stakeholderType: {}", partyId, stakeholderType);
		throw Problem.builder()
			.withStatus(NOT_FOUND)
			.withDetail("No counterpart found for partyId: " + partyId + " or stakeholderType: " + stakeholderType)
			.build();
	}

	private CounterpartMappingEntity findBestMatch(String legalId, List<CounterpartMappingEntity> mappingEntities) {
		String cleanLegalId = legalId.replace("-", "");

		return mappingEntities.stream()
			.filter(Objects::nonNull)
			.filter(not(mappingEntity -> mappingEntity.getLegalIdPattern() == null))
			.filter(mappingEntity -> cleanLegalId.startsWith(mappingEntity.getLegalIdPattern()))
			// If multiple patterns match, select the one with the longest match (most specific)
			.max(Comparator.comparingInt(mappingEntity -> mappingEntity.getLegalIdPattern().length()))
			.orElse(null);
	}
}
