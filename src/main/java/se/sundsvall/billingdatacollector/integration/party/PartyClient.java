package se.sundsvall.billingdatacollector.integration.party;

import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static se.sundsvall.billingdatacollector.integration.party.PartyIntegrationConfiguration.CLIENT_ID;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import generated.se.sundsvall.party.PartyType;

@FeignClient(
	name = CLIENT_ID,
	configuration = PartyIntegrationConfiguration.class,
	url = "${integration.party.base-url}",
	dismiss404 = true
)
interface PartyClient {

	@GetMapping(
		path = "/{type}/{legalId}/partyId",
		produces = { TEXT_PLAIN_VALUE, APPLICATION_PROBLEM_JSON_VALUE }
	)
	@Cacheable("partyId")
	Optional<String> getPartyId(@PathVariable("type") PartyType partyType, @PathVariable("legalId") String legalId);
}
