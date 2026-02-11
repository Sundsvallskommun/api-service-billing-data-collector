package se.sundsvall.billingdatacollector.service.source.contract;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.service.EntityMapper;
import se.sundsvall.billingdatacollector.service.source.AbstractHandler;

@Component("contract")
public class ContractBillingHandler extends AbstractHandler {
	private static final String ERROR_FAILED_TO_SEND_BILLING_RECORD_TITLE = "Failed to send billing record to billing preprocessor";
	private static final String ERROR_NO_CONTRACT_FOUND_TITLE = "No contract found";
	private static final String ERROR_NO_CONTRACT_FOUND = "No contract with contract id {} was found within municipalityId {}";

	private final ContractIntegration contractIntegration;
	private final ContractMapper contractMapper;
	private final BillingPreprocessorClient billingPreprocessorClient;
	private final HistoryRepository historyRepository;

	ContractBillingHandler(
		ContractIntegration contractIntegration,
		ContractMapper contractMapper,
		BillingPreprocessorClient billingPreprocessorClient,
		HistoryRepository historyRepository) {

		this.contractIntegration = contractIntegration;
		this.contractMapper = contractMapper;
		this.billingPreprocessorClient = billingPreprocessorClient;
		this.historyRepository = historyRepository;
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

			throw Problem.builder()
				.withTitle(ERROR_FAILED_TO_SEND_BILLING_RECORD_TITLE)
				.withDetail(e.getMessage())
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	private void handleNoMatchInContract(String municipalityId, String externalId) {
		logError(ERROR_NO_CONTRACT_FOUND, externalId, municipalityId);

		throw Problem.builder()
			.withTitle(ERROR_NO_CONTRACT_FOUND_TITLE)
			.withDetail(String.format(ERROR_NO_CONTRACT_FOUND, externalId, municipalityId))
			.withStatus(INTERNAL_SERVER_ERROR)
			.build();
	}

	private String getLocation(ResponseEntity<Void> response) {
		return Optional.of(response.getHeaders())
			.map(HttpHeaders::getLocation)
			.map(URI::toString)
			.orElse(null);
	}
}
