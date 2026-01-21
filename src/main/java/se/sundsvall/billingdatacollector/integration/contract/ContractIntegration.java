package se.sundsvall.billingdatacollector.integration.contract;

import generated.se.sundsvall.contract.Contract;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ContractIntegration {

	private final ContractClient client;

	public ContractIntegration(ContractClient client) {
		this.client = client;
	}

	/**
	 * Method returns an optional contract matching provided muncipality id and contract id, or an optional empty if no
	 * match is found
	 *
	 * @param  municipalityId the id of the muncipality to which the contract belongs
	 * @param  contractId     the id of the contract to fetch
	 * @return                the contract matching the provided filters or optional empty if no match is found
	 */
	public Optional<Contract> getContract(String municipalityId, String contractId) {
		return client.getContract(municipalityId, contractId);
	}
}
