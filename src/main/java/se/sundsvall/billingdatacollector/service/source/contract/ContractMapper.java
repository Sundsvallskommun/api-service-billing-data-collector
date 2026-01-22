package se.sundsvall.billingdatacollector.service.source.contract;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.contract.Address;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Stakeholder;
import generated.se.sundsvall.contract.StakeholderRole;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

@Component
public class ContractMapper {

	private static final String CATEGORY = "MEX_INVOICE";
	private static final String FULL_CONTRACT_ID_TEMPLATE = "%s (%s)";
	private static final String APPROVED_BY = "CONTRACT-SERVICE";
	private static final String PARAMETER_KEY_CONTRACT_ID = "contractId";
	private static final String PARAMETER_KEY_KPI = "index";

	public BillingRecord toBillingRecord(Contract nullablecontract) {

		return ofNullable(nullablecontract)
			.map(contract -> new BillingRecord()
				.approvedBy(APPROVED_BY)
				.category(CATEGORY)
				.invoice(toInvoice())
				.recipient(toRecipient(contract))
				.status(APPROVED)
				.type(EXTERNAL)
				.putExtraParametersItem(PARAMETER_KEY_CONTRACT_ID, calculateContractId(contract))
				.putExtraParametersItem(PARAMETER_KEY_KPI, retrieveKPI()))
			.orElse(null);
	}

	private String calculateContractId(Contract contract) {
		return ofNullable(contract.getExternalReferenceId())
			.filter(StringUtils::isNotBlank)
			.map(externalReferenceId -> FULL_CONTRACT_ID_TEMPLATE.formatted(contract.getContractId(), externalReferenceId))
			.orElse(contract.getContractId());
	}

	private Invoice toInvoice() {
		return null; // TODO: Implemented in ticket DRAKEN-3178
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
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Manadatory address information not found for billing party with party id %s".formatted(billingParty.getPartyId())));
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

	private String retrieveKPI() {
		return "TODO";  // TODO: Implemented in ticket DRAKEN-3178
	}
}
