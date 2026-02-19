package se.sundsvall.billingdatacollector.integration.contract;

import generated.se.sundsvall.contract.Contract;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.billingdatacollector.integration.contract.ContractConfiguration.CLIENT_ID;

@FeignClient(
	name = CLIENT_ID,
	url = "${integration.contract.base-url}",
	configuration = ContractConfiguration.class,
	dismiss404 = true)
@CircuitBreaker(name = CLIENT_ID)
public interface ContractClient {

	@GetMapping(path = "/{municipalityId}/contracts/{/contractId}", produces = APPLICATION_JSON_VALUE)
	Optional<Contract> getContract(
		@PathVariable final String municipalityId,
		@PathVariable final String contractId);
}
