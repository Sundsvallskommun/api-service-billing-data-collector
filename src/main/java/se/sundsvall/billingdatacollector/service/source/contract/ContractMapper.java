package se.sundsvall.billingdatacollector.service.source.contract;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static generated.se.sundsvall.contract.InvoicedIn.ADVANCE;
import static java.time.Month.OCTOBER;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.billingdatacollector.service.source.contract.util.CalculationUtil.calculateIndexedCost;
import static se.sundsvall.billingdatacollector.service.source.contract.util.CalculationUtil.calculateNonIndexedCost;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getAccrualKey;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getContractId;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getExtraParameter;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getKPIBaseYear;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.isIndexed;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.contract.Address;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Fees;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.Invoicing;
import generated.se.sundsvall.contract.PropertyDesignation;
import generated.se.sundsvall.contract.Stakeholder;
import generated.se.sundsvall.contract.StakeholderRole;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.scb.ScbIntegration;

@Component
public class ContractMapper {

	private static final String CATEGORY = "MEX_INVOICE";
	private static final String APPROVED_BY = "CONTRACT-SERVICE";
	private static final String PARAMETER_KEY_CONTRACT_ID = "contractId";
	private static final String PARAMETER_KEY_KPI = "index";
	private static final int INDEX_MONTH = OCTOBER.getValue(); // Month to use when fetching KPI is always october
	private static final BigDecimal QUANTITY = BigDecimal.ONE; // Quantity is always one for periodical invoicing

	private final ScbIntegration scbIntegration;
	private final SettingsProvider settingsProvider;
	private final CounterpartMappingService counterpartMappingService;

	public ContractMapper(ScbIntegration scbIntegration,
		SettingsProvider settingsProvider, CounterpartMappingService counterpartMappingService) {

		this.scbIntegration = scbIntegration;
		this.settingsProvider = settingsProvider;
		this.counterpartMappingService = counterpartMappingService;
	}

	public BillingRecord createBillingRecord(Contract contract) {
		return ofNullable(contract)
			.map(this::toBillingRecord)
			.orElse(null);
	}

	private BillingRecord toBillingRecord(Contract contract) {
		final var billingRecord = new BillingRecord()
			.approvedBy(APPROVED_BY)
			.category(CATEGORY)
			.invoice(toInvoice(contract))
			.recipient(toRecipient(contract))
			.status(APPROVED)
			.type(EXTERNAL)
			.putExtraParametersItem(PARAMETER_KEY_CONTRACT_ID, getContractId(contract));

		if (isIndexed(contract)) {
			final var indexBaseYear = getKPIBaseYear(contract);
			final var currentIndexPeriod = YearMonth.now().withMonth(INDEX_MONTH);
			billingRecord.putExtraParametersItem(PARAMETER_KEY_KPI, String.valueOf(scbIntegration.getKPI(indexBaseYear, currentIndexPeriod))); // KPI value used when calculating prices
		}

		return billingRecord;
	}

	private Invoice toInvoice(Contract contract) {
		return new Invoice()
			.ourReference(getContractId(contract))
			.customerReference(getExtraParameter(contract, "InvoiceInfo", "markup"))
			.addInvoiceRowsItem(mapInvoiceRow(contract))
			.description(ofNullable(contract.getInvoicing())
				.filter(invoicing -> Objects.equals(ADVANCE, invoicing.getInvoicedIn()))
				.map(Invoicing::getInvoiceInterval)
				.map(IntervalType::getValue)
				.orElse(null));
	}

	private InvoiceRow mapInvoiceRow(Contract contract) {
		final var costPerUnit = calculateCost(contract);

		return new InvoiceRow()
			.costPerUnit(costPerUnit)
			.accountInformation(mapAccountInformation(contract, costPerUnit.multiply(QUANTITY)))
			.vatCode(settingsProvider.getVatCode(contract))
			.quantity(QUANTITY)
			.descriptions(ofNullable(contract.getFees()).map(Fees::getAdditionalInformation).orElse(null))
			.detailedDescriptions(ofNullable(contract.getPropertyDesignations())
				.orElse(emptyList()).stream()
				.map(PropertyDesignation::getName)
				.toList());
	}

	private BigDecimal calculateCost(Contract contract) {
		if (isIndexed(contract)) {
			final var indexBaseYear = getKPIBaseYear(contract);
			final var currentIndexPeriod = YearMonth.now().withMonth(INDEX_MONTH);
			final var currentKPI = scbIntegration.getKPI(indexBaseYear, currentIndexPeriod);
			return calculateIndexedCost(contract, currentKPI);
		}
		return calculateNonIndexedCost(contract);
	}

	private String getCounterpart(Contract contract) {
		return ofNullable(contract.getStakeholders()).orElse(emptyList()).stream()
			.filter(isPrimaryBillingParty())
			.findFirst()
			.map(stakeholder -> counterpartMappingService.findCounterpartByLegalId(stakeholder.getOrganizationNumber(),
				getStakeholderType(stakeholder)))
			.orElse(null);
	}

	private String getStakeholderType(Stakeholder stakeholder) {
		return ofNullable(stakeholder.getType())
			.map(Enum::name)
			.orElse(null);
	}

	private Recipient toRecipient(Contract contract) {
		return ofNullable(contract.getStakeholders()).orElse(emptyList()).stream()
			.filter(isPrimaryBillingParty())
			.findFirst()
			.map(toRecipient())
			.orElse(null);
	}

	private Function<? super Stakeholder, ? extends Recipient> toRecipient() {
		return billingParty -> new Recipient()
			.addressDetails(toAddressDetails(billingParty))
			.firstName(billingParty.getFirstName())
			.lastName(billingParty.getLastName())
			.partyId(billingParty.getPartyId())
			.organizationName(billingParty.getOrganizationName());
	}

	private AddressDetails toAddressDetails(Stakeholder billingParty) {
		return ofNullable(billingParty.getAddress())
			.map(toAddressDetails())
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Mandatory address information not found for billing party with party id %s".formatted(billingParty.getPartyId())));
	}

	private Function<? super Address, ? extends AddressDetails> toAddressDetails() {
		return address -> new AddressDetails()
			.careOf(address.getCareOf())
			.city(address.getTown())
			.postalCode(address.getPostalCode())
			.street(address.getStreetAddress());
	}

	private static Predicate<? super Stakeholder> isPrimaryBillingParty() {
		return stakeholder -> ofNullable(stakeholder.getRoles()).orElse(emptyList()).stream()
			.anyMatch(role -> role == StakeholderRole.PRIMARY_BILLING_PARTY);
	}

	private List<AccountInformation> mapAccountInformation(Contract contract, BigDecimal amount) {
		if (settingsProvider.isLeaseTypeSettingsPresent(contract)) {
			return List.of(new AccountInformation()
				.accuralKey(getAccrualKey(contract))
				.activity(settingsProvider.getActivity(contract))
				.costCenter(settingsProvider.getCostCenter(contract))
				.department(settingsProvider.getDepartment(contract))
				.subaccount(settingsProvider.getSubaccount(contract))
				.counterpart(getCounterpart(contract))
				.amount(amount));
		}
		return emptyList();
	}
}
