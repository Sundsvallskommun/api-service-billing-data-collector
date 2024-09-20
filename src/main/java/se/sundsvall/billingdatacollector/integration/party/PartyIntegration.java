package se.sundsvall.billingdatacollector.integration.party;

import static generated.se.sundsvall.party.PartyType.PRIVATE;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PartyIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(PartyIntegration.class);

	private final PartyClient partyClient;

	PartyIntegration(final PartyClient partyClient) {
		this.partyClient = partyClient;
	}

	public Optional<String> getPartyId(final String municipalityId, final String legalId) {
		try {
			return partyClient.getPartyId(municipalityId, PRIVATE, legalId);
		} catch (final Exception e) {
			LOG.info("Unable to get party id for municipalityId {} and legal id {}: {}", municipalityId, legalId, e.getMessage());

			return Optional.empty();
		}
	}
}
