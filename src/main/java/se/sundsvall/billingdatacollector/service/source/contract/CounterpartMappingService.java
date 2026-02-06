package se.sundsvall.billingdatacollector.service.source.contract;

import static org.zalando.problem.Status.NOT_FOUND;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.db.CounterpartMappingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.CounterpartMappingEntity;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;

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
	 * 1. Exact legalId match
	 * 2. Regexp pattern matching on legalId
	 * 3. Stakeholder type fallback
	 *
	 * @param  partyId                              the partyId to find legalId for
	 * @param  stakeholderType                      the stakeholder type for fallback matching
	 * @return                                      the counterpart value
	 * @throws org.zalando.problem.ThrowableProblem if no match found
	 */
	public String findCounterpart(String municipalityId, String partyId, String stakeholderType) {
		LOG.debug("Finding counterpart for partyId: {}, stakeholderType: {}", partyId, stakeholderType);

		final var legalId = partyIntegration.getLegalId(municipalityId, partyId);

		// 1. First try exact legalId match
		var exactMatch = repository.findByLegalId(legalId);
		if (exactMatch.isPresent()) {
			return exactMatch.get().getCounterpart();
		}

		// 2. Try pattern matching - load all and filter in Java
		var patternMatch = repository.findAll().stream()
			.filter(mapping -> mapping.getLegalIdPattern() != null)
			.filter(mapping -> matchesPattern(mapping.getLegalIdPattern(), legalId))
			.map(CounterpartMappingEntity::getCounterpart)
			.findFirst();

		if (patternMatch.isPresent()) {
			return patternMatch.get();
		}

		// 3. Fall back to stakeholder type
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

	private boolean matchesPattern(String patternString, String legalId) {
		try {
			final var pattern = Pattern.compile(patternString);
			return pattern.matcher(legalId).matches();
		} catch (PatternSyntaxException e) {
			LOG.error("Invalid regex pattern in database: {}", patternString, e);
			return false;
		}
	}
}
