package se.sundsvall.billingdatacollector.integration.party;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import se.sundsvall.billingdatacollector.service.util.LegalIdUtil;
import se.sundsvall.dept44.problem.Problem;

import static generated.se.sundsvall.party.PartyType.ENTERPRISE;
import static generated.se.sundsvall.party.PartyType.PRIVATE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

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
	 *                        param type The stakeholder type
	 * @return                The legalId
	 */
	public Optional<String> getLegalId(final String municipalityId, final String partyId, final String type) {
		return switch (type) {
			case "ORGANIZATION", "MUNICIPALITY", "ASSOCIATION" -> partyClient.getLegalId(municipalityId, ENTERPRISE, partyId);
			case "PRIVATE" -> partyClient.getLegalId(municipalityId, PRIVATE, partyId);
			default -> Optional.empty();
		};
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
