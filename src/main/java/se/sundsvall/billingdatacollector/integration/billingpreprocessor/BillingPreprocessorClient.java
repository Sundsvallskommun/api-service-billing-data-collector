package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorIntegrationConfiguration.CLIENT_ID;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.billing-preprocessor.base-url}",
	configuration = BillingPreprocessorIntegrationConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface BillingPreprocessorClient {

	@PostMapping(
		path = "/{municipalityId}/billingrecords",
		consumes = APPLICATION_JSON_VALUE,
		produces = ALL_VALUE)
	ResponseEntity<Void> createBillingRecord(@PathVariable("municipalityId") String municipalityId, @RequestBody BillingRecord billingRecord);
}
