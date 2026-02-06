package se.sundsvall.billingdatacollector.integration.party;

import static generated.se.sundsvall.party.PartyType.ENTERPRISE;
import static generated.se.sundsvall.party.PartyType.PRIVATE;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.service.util.LegalIdUtil;

@Component
public class PartyIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(PartyIntegration.class);

	private final PartyClient partyClient;

	PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	/**
	 * Fetch partyId by first checking for enterprise, then private.
	 * Private legalIds are checked for valid format and cleaned (century digits added).
	 *
	 * @param  municipalityId The municipalityId
	 * @param  legalId        The legalId
	 * @return                The partyId
	 */
	@Cacheable("partyId")
	public String getPartyId(final String municipalityId, final String legalId) {
		return partyClient.getPartyId(municipalityId, ENTERPRISE, legalId)
			.orElseGet(() -> checkAndGetCorrectPersonalNumber(municipalityId, legalId)
				.orElseThrow(() -> Problem.builder()
					.withTitle("Couldn't find partyId for legalId " + legalId)
					.withStatus(INTERNAL_SERVER_ERROR)
					.build()));
	}

	/**
	 * Fetch legalId by partyId.
	 *
	 * @param  municipalityId The municipalityId
	 * @param  partyId        The partyId
	 * @return                The legalId
	 */
	@Cacheable("legalId")
	public String getLegalId(final String municipalityId, final String partyId) {
		return partyClient.getLegalId(municipalityId, partyId)
			.orElseThrow(() -> Problem.builder()
				.withTitle("Couldn't find legalId for partyId " + partyId)
				.withStatus(INTERNAL_SERVER_ERROR)
				.build());
	}

	private Optional<String> checkAndGetCorrectPersonalNumber(final String municipalityId, final String legalId) {
		// Add century digits to legalId if they are missing
		var cleanedLegalId = LegalIdUtil.addCenturyDigitsToLegalId(legalId);
		if (!LegalIdUtil.isValidLegalId(cleanedLegalId)) {
			LOG.warn("Invalid personal number: {}", cleanedLegalId);
			return Optional.empty();
		}
		return partyClient.getPartyId(municipalityId, PRIVATE, cleanedLegalId);
	}
}
