package se.sundsvall.billingdatacollector.service.source.contract;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.contract.Contract;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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
				.invoice(toInvoice(contract))
				.recipient(toRecipient(contract))
				.status(APPROVED)
				.type(EXTERNAL)
				.putExtraParametersItem(PARAMETER_KEY_CONTRACT_ID, calculateContractId(contract))
				.putExtraParametersItem(PARAMETER_KEY_KPI, retrieveKPI(contract)))
			.orElse(null);
	}

	private String calculateContractId(Contract contract) {
		return ofNullable(contract.getExternalReferenceId())
			.filter(StringUtils::isNotBlank)
			.map(externalReferenceId -> FULL_CONTRACT_ID_TEMPLATE.formatted(contract.getContractId(), externalReferenceId))
			.orElse(contract.getContractId());
	}

	private Invoice toInvoice(Contract contract) {
		return null; // TODO: Implemented in ticket DRAKEN-3178
	}

	private Recipient toRecipient(Contract contract) {
		return null; // TODO: Implemented in ticket DRAKEN-3177
	}

	private String retrieveKPI(Contract contract) {
		return "TODO";  // TODO: Implemented in ticket DRAKEN-3178
	}
}
