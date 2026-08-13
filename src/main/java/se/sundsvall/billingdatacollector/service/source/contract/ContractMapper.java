package se.sundsvall.billingdatacollector.service.source.contract;

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
import generated.se.sundsvall.contract.PropertyDesignation;
import generated.se.sundsvall.contract.Stakeholder;
import generated.se.sundsvall.contract.StakeholderRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import se.sundsvall.billingdatacollector.integration.scb.ScbIntegration;
import se.sundsvall.billingdatacollector.service.util.BillingPeriodCalculator;
import se.sundsvall.billingdatacollector.service.util.BillingPeriodCalculator.BillingPeriod;
import se.sundsvall.dept44.problem.Problem;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static java.time.Month.OCTOBER;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static se.sundsvall.billingdatacollector.service.source.contract.util.CalculationUtil.calculateIndexedCost;
import static se.sundsvall.billingdatacollector.service.source.contract.util.CalculationUtil.calculateNonIndexedCost;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getAccrualKey;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getContractId;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getDetailedDescriptions;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getExtraParameter;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getKPIBaseYear;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.isIndexed;

@Component
public class ContractMapper {

	private static final String CATEGORY = "MEX_INVOICE";
	private static final String APPROVED_BY = "CONTRACT-SERVICE";
	private static final String PARAMETER_KEY_CONTRACT_ID = "contractId";
	private static final String PARAMETER_KEY_KPI = "index";
	private static final String NOT_APPLICABLE = "N/A";
	private static final int INDEX_MONTH = OCTOBER.getValue(); // Month to use when fetching KPI is always october
	private static final BigDecimal QUANTITY = BigDecimal.ONE; // Quantity is always one for periodical invoicing
	private static final String[] SWEDISH_MONTH_NAMES = {
		"januari", "februari", "mars", "april", "maj", "juni",
		"juli", "augusti", "september", "oktober", "november", "december"
	};
	// Same-year period: "Avser januari-december 2026"
	private static final String SAME_YEAR_DESCRIPTION_FORMAT = "Avser %s-%s %d";
	// Cross-year period: "Avser juli 2026-juni 2027"
	private static final String SPANNING_YEARS_DESCRIPTION_FORMAT = "Avser %s %d-%s %d";
	// BillingPreprocessor enforces maxLength: 30 on each entry in
	// InvoiceRow.descriptions; longer strings would be rejected as BAD_REQUEST.
	private static final int DESCRIPTION_MAX_LENGTH = 30;
	private static final int DETAILED_DESCRIPTION_MAX_LENGTH = 51;

	private final ScbIntegration scbIntegration;
	private final SettingsProvider settingsProvider;
	private final CounterpartMappingService counterpartMappingService;

	public ContractMapper(ScbIntegration scbIntegration,
		SettingsProvider settingsProvider, CounterpartMappingService counterpartMappingService) {

		this.scbIntegration = scbIntegration;
		this.settingsProvider = settingsProvider;
		this.counterpartMappingService = counterpartMappingService;
	}

	public BillingRecord createBillingRecord(String municipalityId, Contract contract, LocalDate scheduledDate) {
		return ofNullable(contract)
			.map(contract1 -> this.toBillingRecord(municipalityId, contract1, scheduledDate))
			.orElse(null);
	}

	private BillingRecord toBillingRecord(String municipalityId, Contract contract, LocalDate scheduledDate) {
		final var transferDate = LocalDate.now();
		final var billingRecord = new BillingRecord()
			.approvedBy(APPROVED_BY)
			.category(CATEGORY)
			.invoice(toInvoice(municipalityId, contract, scheduledDate, transferDate))
			.recipient(toRecipient(contract))
			.status(APPROVED)
			.type(EXTERNAL)
			.transferDate(transferDate)
			.putExtraParametersItem(PARAMETER_KEY_CONTRACT_ID, getContractId(contract));

		if (isIndexed(contract)) {
			final var indexBaseYear = getKPIBaseYear(contract);
			final var indexPeriod = getIndexPeriod(scheduledDate);
			billingRecord.putExtraParametersItem(PARAMETER_KEY_KPI, String.valueOf(scbIntegration.getKPI(indexBaseYear, indexPeriod))); // KPI value used when calculating prices
		}

		return billingRecord;
	}

	private YearMonth getIndexPeriod(LocalDate scheduledDate) {
		return YearMonth.from(scheduledDate.minusYears(1).withMonth(INDEX_MONTH));
	}

	private Invoice toInvoice(String municipalityId, Contract contract, LocalDate scheduledDate, LocalDate transferDate) {
		return new Invoice()
			.ourReference(getContractId(contract))
			.customerReference(getCustomerReference(contract))
			.customerId(NOT_APPLICABLE)
			.addInvoiceRowsItem(mapInvoiceRow(municipalityId, contract, scheduledDate))
			.date(transferDate)
			.dueDate(YearMonth.now().atEndOfMonth());
	}

	private String getCustomerReference(Contract contract) {
		// BillingPreprocessor requires customerReference to be non-blank (minLength: 1),
		// so blank candidates must fall through to the next fallback rather than be
		// emitted as empty strings.
		return ofNullable(trimToNull(getExtraParameter(contract, "InvoiceInfo", "markup")))
			.or(() -> ofNullable(trimToNull(contract.getExternalReferenceId())))
			.orElse(contract.getContractId());
	}

	private InvoiceRow mapInvoiceRow(String municipalityId, Contract contract, LocalDate scheduledDate) {
		final var costPerUnit = calculateCost(contract, scheduledDate);

		var invoiceRow = new InvoiceRow()
			.costPerUnit(costPerUnit)
			.accountInformation(mapAccountInformation(municipalityId, contract, costPerUnit.multiply(QUANTITY)))
			.vatCode(settingsProvider.getVatCode(contract))
			.quantity(QUANTITY)
			.descriptions(sanitizeDescriptions(
				ofNullable(contract.getFees()).map(Fees::getAdditionalInformation).orElse(null)));

		getDetailedDescriptions(contract).stream()
			.map(v -> v.length() > DETAILED_DESCRIPTION_MAX_LENGTH ? v.substring(0, DETAILED_DESCRIPTION_MAX_LENGTH) : v)
			.forEach(invoiceRow::addDetailedDescriptionsItem);
		ofNullable(getPropertyDesignation(contract)).ifPresent(invoiceRow::addDetailedDescriptionsItem);
		ofNullable(getInvoiceDescription(contract, scheduledDate)).ifPresent(invoiceRow::addDetailedDescriptionsItem);

		return invoiceRow;
	}

	private BigDecimal calculateCost(Contract contract, LocalDate scheduledDate) {
		if (isIndexed(contract)) {
			final var indexBaseYear = getKPIBaseYear(contract);
			final var indexPeriod = getIndexPeriod(scheduledDate);
			final var currentKPI = scbIntegration.getKPI(indexBaseYear, indexPeriod);
			return calculateIndexedCost(contract, currentKPI);
		}
		return calculateNonIndexedCost(contract);
	}

	private String getCounterpart(String municipalityId, Contract contract) {
		return ofNullable(contract.getStakeholders()).orElse(emptyList()).stream()
			.filter(isPrimaryBillingParty())
			.findFirst()
			.map(stakeholder -> counterpartMappingService.findCounterpart(municipalityId, stakeholder.getPartyId(),
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

	private List<AccountInformation> mapAccountInformation(String municipalityId, Contract contract, BigDecimal amount) {
		if (settingsProvider.isLeaseTypeSettingsPresent(contract)) {
			return List.of(new AccountInformation()
				.accuralKey(getAccrualKey(contract))
				.activity(settingsProvider.getActivity(contract))
				.costCenter(settingsProvider.getCostCenter(contract))
				.department(settingsProvider.getDepartment(contract))
				.subaccount(settingsProvider.getSubaccount(contract))
				.counterpart(getCounterpart(municipalityId, contract))
				.amount(amount));
		}
		return emptyList();
	}

	private String getPropertyDesignation(Contract contract) {
		return ofNullable(contract.getPropertyDesignations())
			.orElse(emptyList()).stream()
			.map(PropertyDesignation::getName)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Filters out null/blank entries and truncates each remaining string to
	 * {@value #DESCRIPTION_MAX_LENGTH} characters so the payload satisfies the
	 * minLength/maxLength constraints on {@code InvoiceRow.descriptions} in the
	 * billing-preprocessor schema. Returns {@code null} for null input so the
	 * field is simply omitted from the request when no data is available.
	 */
	private static List<String> sanitizeDescriptions(List<String> descriptions) {
		if (descriptions == null) {
			return null;
		}
		return descriptions.stream()
			.filter(StringUtils::isNotBlank)
			.map(d -> d.length() > DESCRIPTION_MAX_LENGTH ? d.substring(0, DESCRIPTION_MAX_LENGTH) : d)
			.toList();
	}

	/**
	 * Builds the human-readable description text shown on the invoice row,
	 * derived from the period covered by the billing — "Avser
	 * &lt;startMonth&gt;[ &lt;startYear&gt;]-&lt;endMonth&gt; &lt;endYear&gt;".
	 *
	 * <p>
	 * The period itself is computed by {@link BillingPeriodCalculator}, which
	 * is also the source of truth used by the scheduler to decide whether a
	 * billing's period still fits inside the contract.
	 */
	private String getInvoiceDescription(Contract contract, LocalDate scheduledDate) {
		// MONTHLY contracts have never carried a period description on the
		// invoice row; preserve that behaviour and only describe yearly,
		// half-yearly and quarterly billings.
		return ofNullable(contract.getInvoicing())
			.filter(invoicing -> invoicing.getInvoiceInterval() != null
				&& invoicing.getInvoiceInterval() != IntervalType.MONTHLY)
			.filter(invoicing -> invoicing.getInvoicedIn() != null)
			.map(invoicing -> describePeriod(BillingPeriodCalculator.computePeriod(
				effectiveDateForDescription(contract, scheduledDate, invoicing.getInvoiceInterval()),
				invoicing.getInvoiceInterval(), invoicing.getInvoicedIn())))
			.orElse(null);
	}

	/**
	 * For YEARLY billing the period type (July–June vs January–December) must be
	 * determined by {@code currentPeriod.endDate}, not by the scheduled billing
	 * date. This guards against the case where {@code scheduledDate} is set to an
	 * unexpected month while the contract's period boundary is still 30 June.
	 */
	private static LocalDate effectiveDateForDescription(Contract contract, LocalDate scheduledDate, IntervalType interval) {
		if (interval != IntervalType.YEARLY) {
			return scheduledDate;
		}
		var periodEndDate = contract.getCurrentPeriod() != null ? contract.getCurrentPeriod().getEndDate() : null;
		if (periodEndDate != null && periodEndDate.getMonth() == Month.JUNE && periodEndDate.getDayOfMonth() == 30) {
			return scheduledDate.withMonth(Month.JUNE.getValue());
		}
		return scheduledDate.withMonth(Month.DECEMBER.getValue());
	}

	private static String describePeriod(BillingPeriod period) {
		var startMonth = SWEDISH_MONTH_NAMES[period.startDate().getMonthValue() - 1];
		var endMonth = SWEDISH_MONTH_NAMES[period.endDate().getMonthValue() - 1];
		if (period.startDate().getYear() == period.endDate().getYear()) {
			return SAME_YEAR_DESCRIPTION_FORMAT.formatted(startMonth, endMonth, period.endDate().getYear());
		}
		return SPANNING_YEARS_DESCRIPTION_FORMAT.formatted(
			startMonth, period.startDate().getYear(), endMonth, period.endDate().getYear());
	}
}
