package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.ExtraParameterGroup;
import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
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

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Component("contract")
public class ContractBillingHandler extends AbstractHandler {
	private static final String ERROR_NO_CONTRACT_FOUND_TITLE = "No active contract found";
	private static final String ERROR_NO_CONTRACT_FOUND = "No active contract with contract id {} was found within municipalityId {}";
	private static final String CONTRACT_DETAILS_GROUP_NAME = "ContractDetails";
	private static final String FINAL_BILLING_DATE_KEY = "finalBillingDate";

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
			.filter(isBeforeLastBillingDateIfPresent())
			.map(contract -> contractMapper.createBillingRecord(municipalityId, contract))
			.ifPresentOrElse(
				billingRecord -> sendAndSave(municipalityId, billingRecord),
				() -> handleNoMatchInContract(municipalityId, externalId));
	}

	private void sendAndSave(String municipalityId, BillingRecord billingRecord) {
		logInfo("Sending billing record to billing preprocessor");
		final var response = billingPreprocessorClient.createBillingRecord(municipalityId, billingRecord);
		logInfo("Billing record sent successfully with response status: {}", response.getStatusCode());
		// Save to history
		historyRepository.saveAndFlush(EntityMapper.mapToHistoryEntity(municipalityId, billingRecord, getLocation(response)));
	}

	private void handleNoMatchInContract(String municipalityId, String externalId) {
		logError(ERROR_NO_CONTRACT_FOUND, externalId, municipalityId);

		throw Problem.builder()
			.withTitle(ERROR_NO_CONTRACT_FOUND_TITLE)
			.withDetail(String.format(ERROR_NO_CONTRACT_FOUND, externalId, municipalityId))
			.withStatus(INTERNAL_SERVER_ERROR)
			.build();
	}

	private static Predicate<? super Contract> isBeforeLastBillingDateIfPresent() {
		return contract -> ofNullable(contract.getExtraParameters())
			.orElse(emptyList()).stream()
			.filter(extraParameterGroup -> CONTRACT_DETAILS_GROUP_NAME.equals(extraParameterGroup.getName()))
			.map(ExtraParameterGroup::getParameters)
			.map(parameters -> parameters.get(FINAL_BILLING_DATE_KEY))
			.filter(Objects::nonNull)
			.map(LocalDate::parse)
			.findFirst()
			.map(finalBillingDate -> LocalDate.now().isBefore(finalBillingDate))
			.orElse(true);
	}

	private String getLocation(ResponseEntity<Void> response) {
		return Optional.of(response.getHeaders())
			.map(HttpHeaders::getLocation)
			.map(URI::toString)
			.orElse(null);
	}
}
