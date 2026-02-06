package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import org.springframework.stereotype.Component;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.billingdatacollector.service.source.AbstractHandler;

@Component("contract")
public class ContractBillingHandler extends AbstractHandler {
	private final ContractIntegration contractIntegration;
	private final ContractMapper contractMapper;
	private final BillingPreprocessorClient billingPreprocessorClient;
	private final HistoryRepository historyRepository;
	private final FalloutRepository falloutRepository;
	private final PartyIntegration partyIntegration;

	ContractBillingHandler(
		ContractIntegration contractIntegration,
		ContractMapper contractMapper,
		BillingPreprocessorClient billingPreprocessorClient,
		HistoryRepository historyRepository,
		FalloutRepository falloutRepository,
		PartyIntegration partyIntegration) {

		this.contractIntegration = contractIntegration;
		this.contractMapper = contractMapper;
		this.billingPreprocessorClient = billingPreprocessorClient;
		this.historyRepository = historyRepository;
		this.falloutRepository = falloutRepository;
		this.partyIntegration = partyIntegration;
	}

	@Override
	public void sendBillingRecords(String municipalityId, String externalId) {
		logInfo("Processing contract with id {} in municipality {}", externalId, municipalityId);

		contractIntegration.getContract(municipalityId, externalId)
			.map(contract -> contractMapper.createBillingRecord(municipalityId, contract))
			.ifPresentOrElse(
				this::sendAndSave,
				() -> handleNoMatchInContract(municipalityId, externalId));
	}

	private void sendAndSave(BillingRecord billingRecord) {
		logInfo("Sending billing record to billing preprocessor");

		// TODO: Send to billing preprocessor is implemented in ticket DRAKEN-3183
		// TODO: Save in history repository (or fallout if exception occurs) is implemented in ticket DRAKEN-3183
	}

	private void handleNoMatchInContract(String municipalityId, String externalId) {
		logError("No contract with contract id {} was found within municipalityId {}", externalId, municipalityId);

		// TODO: // Save in fallout repository is implemented in ticket DRAKEN-3183
	}
}
