package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.service.EntityMapper;
import se.sundsvall.billingdatacollector.service.source.AbstractHandler;

@Component("contract")
public class ContractBillingHandler extends AbstractHandler {
	private static final String ERROR_NO_CONTRACT_FOUND = "No contract found";

	private final ContractIntegration contractIntegration;
	private final ContractMapper contractMapper;
	private final BillingPreprocessorClient billingPreprocessorClient;
	private final HistoryRepository historyRepository;
	private final FalloutRepository falloutRepository;

	ContractBillingHandler(
		ContractIntegration contractIntegration,
		ContractMapper contractMapper,
		BillingPreprocessorClient billingPreprocessorClient,
		HistoryRepository historyRepository,
		FalloutRepository falloutRepository) {

		this.contractIntegration = contractIntegration;
		this.contractMapper = contractMapper;
		this.billingPreprocessorClient = billingPreprocessorClient;
		this.historyRepository = historyRepository;
		this.falloutRepository = falloutRepository;
	}

	@Transactional
	@Override
	public void sendBillingRecords(String municipalityId, String externalId) {
		logInfo("Processing contract with id {} in municipality {}", externalId, municipalityId);

		contractIntegration.getContract(municipalityId, externalId)
			.map(contract -> contractMapper.createBillingRecord(municipalityId, contract))
			.ifPresentOrElse(
				billingRecord -> sendAndSave(municipalityId, billingRecord),
				() -> handleNoMatchInContract(municipalityId, externalId));
	}

	private void sendAndSave(String municipalityId, BillingRecord billingRecord) {
		logInfo("Sending billing record to billing preprocessor");
		try {
			final var response = billingPreprocessorClient.createBillingRecord(municipalityId, billingRecord);
			logInfo("Billing record sent successfully with response status: {}", response.getStatusCode());
			// Save to history
			historyRepository.saveAndFlush(EntityMapper.mapToHistoryEntity(municipalityId, billingRecord, getLocation(response)));
		} catch (Exception e) {
			logError("Failed to send billing record to billing preprocessor: {}", e.getMessage());
			falloutRepository.saveAndFlush(EntityMapper.mapToBillingRecordFalloutEntity(municipalityId, billingRecord, e.getMessage()));
		}
	}

	private void handleNoMatchInContract(String municipalityId, String externalId) {
		logError("No contract with contract id {} was found within municipalityId {}", externalId, municipalityId);

		falloutRepository.saveAndFlush(EntityMapper.mapToContractFalloutEntity(municipalityId, externalId, ERROR_NO_CONTRACT_FOUND));
	}

	private String getLocation(ResponseEntity<Void> response) {
		return Optional.of(response.getHeaders())
			.map(HttpHeaders::getLocation)
			.map(URI::toString)
			.orElse(null);
	}
}
