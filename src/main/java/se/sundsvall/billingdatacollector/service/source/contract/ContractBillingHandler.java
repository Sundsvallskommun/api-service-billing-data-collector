package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.ExtraParameterGroup;
import generated.se.sundsvall.relation.Relation;
import generated.se.sundsvall.relation.ResourceIdentifier;
import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.relation.RelationClient;
import se.sundsvall.billingdatacollector.service.EntityMapper;
import se.sundsvall.billingdatacollector.service.source.AbstractHandler;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Component("contract")
public class ContractBillingHandler extends AbstractHandler {
	private static final String ERROR_NO_CONTRACT_FOUND_TITLE = "No active contract found";
	private static final String ERROR_NO_CONTRACT_FOUND = "No active contract with contract id {} was found within municipalityId {}";
	private static final String CONTRACT_DETAILS_GROUP_NAME = "ContractDetails";
	private static final String FINAL_BILLING_DATE_KEY = "finalBillingDate";
	private static final String CONTRACT = "contract";
	private static final String BILLING_PREPROCESSOR_SERVICE = "billingpreprocessor";

	private final ContractIntegration contractIntegration;
	private final ContractMapper contractMapper;
	private final BillingPreprocessorClient billingPreprocessorClient;
	private final HistoryRepository historyRepository;
	private final RelationClient relationClient;

	ContractBillingHandler(
		ContractIntegration contractIntegration,
		ContractMapper contractMapper,
		BillingPreprocessorClient billingPreprocessorClient,
		HistoryRepository historyRepository,
		RelationClient relationClient) {

		this.contractIntegration = contractIntegration;
		this.contractMapper = contractMapper;
		this.billingPreprocessorClient = billingPreprocessorClient;
		this.historyRepository = historyRepository;
		this.relationClient = relationClient;
	}

	@Transactional
	@Override
	public void sendBillingRecords(String municipalityId, String externalId, LocalDate scheduledDate, Consumer<String> unhealthyMessageConsumer) {
		logInfo("Processing contract with id {} in municipality {}", externalId, municipalityId);

		contractIntegration.getContract(municipalityId, externalId)
			.filter(isBeforeLastBillingDateIfPresent(scheduledDate))
			.map(contract -> contractMapper.createBillingRecord(municipalityId, contract, scheduledDate))
			.ifPresentOrElse(
				billingRecord -> sendAndSave(municipalityId, billingRecord, externalId, unhealthyMessageConsumer),
				() -> handleNoMatchInContract(municipalityId, externalId));
	}

	private void sendAndSave(String municipalityId, BillingRecord billingRecord, String contractId, Consumer<String> unhealthyMessageConsumer) {
		logInfo("Sending billing record to billing preprocessor");
		final var response = billingPreprocessorClient.createBillingRecord(municipalityId, billingRecord);
		logInfo("Billing record sent successfully with response status: {}", response.getStatusCode());

		// Save to history
		historyRepository.saveAndFlush(EntityMapper.mapToHistoryEntity(municipalityId, billingRecord, getLocation(response)));

		try {
			relationClient.createRelation(municipalityId, createRelation(contractId, extractIdFromLocationHeader(response)));
		} catch (Exception e) {
			unhealthyMessageConsumer.accept("Error creating relation: " + e.getMessage());
		}
	}

	private String extractIdFromLocationHeader(final ResponseEntity<Void> response) {
		final var locationValue = Optional.ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream().findFirst();
		return locationValue.map(string -> string.substring(string.lastIndexOf('/') + 1)).orElse(EMPTY);
	}

	private Relation createRelation(String contractId, String billingRecordId) {
		var relation = new Relation();

		relation.setType("LINK");
		relation.setSource(createResourceIdentifier(CONTRACT, CONTRACT, contractId));
		relation.setTarget(createResourceIdentifier(BILLING_PREPROCESSOR_SERVICE, "billing-record", billingRecordId));

		return relation;
	}

	private ResourceIdentifier createResourceIdentifier(String service, String type, String id) {
		var resource = new ResourceIdentifier();
		resource.setService(service);
		resource.setType(type);
		resource.setResourceId(id);
		return resource;
	}

	private void handleNoMatchInContract(String municipalityId, String externalId) {
		logError(ERROR_NO_CONTRACT_FOUND, externalId, municipalityId);

		throw Problem.builder()
			.withTitle(ERROR_NO_CONTRACT_FOUND_TITLE)
			.withDetail(String.format(ERROR_NO_CONTRACT_FOUND, externalId, municipalityId))
			.withStatus(INTERNAL_SERVER_ERROR)
			.build();
	}

	private static Predicate<? super Contract> isBeforeLastBillingDateIfPresent(LocalDate scheduledDate) {
		return contract -> ofNullable(contract.getExtraParameters())
			.orElse(emptyList()).stream()
			.filter(extraParameterGroup -> CONTRACT_DETAILS_GROUP_NAME.equals(extraParameterGroup.getName()))
			.map(ExtraParameterGroup::getParameters)
			.map(parameters -> parameters.get(FINAL_BILLING_DATE_KEY))
			.filter(Objects::nonNull)
			.map(LocalDate::parse)
			.findFirst()
			.map(scheduledDate::isBefore)
			.orElse(true);
	}

	private String getLocation(ResponseEntity<Void> response) {
		return Optional.of(response.getHeaders())
			.map(HttpHeaders::getLocation)
			.map(URI::toString)
			.orElse(null);
	}
}
