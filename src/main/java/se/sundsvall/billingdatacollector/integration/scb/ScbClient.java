package se.sundsvall.billingdatacollector.integration.scb;

import generated.se.sundsvall.scb.Dataset;
import generated.se.sundsvall.scb.VariablesSelection;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.billingdatacollector.integration.scb.ScbConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.scb.base-url}",
	configuration = ScbConfiguration.class,
	dismiss404 = true)
@CircuitBreaker(name = CLIENT_ID)
public interface ScbClient {

	@PostMapping(path = "/tables/{id}/data", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	Dataset getKPI(
		@PathVariable final String id,
		@RequestParam final String lang,
		@RequestParam final String outputFormat,
		@RequestBody final VariablesSelection body);
}
