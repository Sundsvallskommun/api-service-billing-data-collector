package se.sundsvall.billingdatacollector.service.source.contract.util;

import static generated.se.sundsvall.contract.InvoicedIn.ADVANCE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.NOT_FOUND;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.ExtraParameterGroup;
import generated.se.sundsvall.contract.Fees;
import generated.se.sundsvall.contract.Invoicing;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;
import se.sundsvall.billingdatacollector.service.source.contract.model.Interval;

public final class ContractUtil {
	private static final String FULL_CONTRACT_ID_TEMPLATE = "%s (%s)";
	private static final String MESSAGE_CONTRACT_CAN_NOT_BE_NULL = "Parameter 'contract' can not be null";

	private ContractUtil() {
		// Prevent instantiation
	}

	public static String getContractId(Contract contract) {
		if (isNull(contract)) {
			throw Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL);
		}

		return ofNullable(contract.getExternalReferenceId())
			.filter(StringUtils::isNotBlank)
			.map(externalReferenceId -> FULL_CONTRACT_ID_TEMPLATE.formatted(contract.getContractId(), externalReferenceId))
			.orElseGet(contract::getContractId);
	}

	public static boolean isIndexed(Contract contract) {
		return ofNullable(contract)
			.map(c -> ofNullable(c.getFees())
				.map(Fees::getIndexType)
				.filter(StringUtils::isNotBlank)
				.isPresent())
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL));
	}

	public static KPIBaseYear getKPIBaseYear(Contract contract) {
		final var indexType = ofNullable(contract)
			.map(c -> ofNullable(c.getFees())
				.map(Fees::getIndexType)
				.filter(StringUtils::isNotBlank)
				.map(String::toUpperCase)
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Contract %s has no defined index type".formatted(c.getContractId()))))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL));

		return switch (indexType) {
			case "KPI 2020" -> KPIBaseYear.KPI_2020;
			default -> KPIBaseYear.KPI_80;
		};
	}

	public static String getExtraParameter(Contract contract, String parameterGroup, String parameterKey) {
		if (isNull(contract)) {
			throw Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL);
		}

		return ofNullable(contract.getExtraParameters()).orElse(emptyList()).stream()
			.filter(group -> Objects.nonNull(group.getName()))
			.filter(group -> Strings.CI.equals(parameterGroup, group.getName()))
			.map(ExtraParameterGroup::getParameters)
			.map(values -> values.get(parameterKey))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	public static String getAccrualKey(Contract contract) {
		if (isNull(contract)) {
			throw Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL);
		}

		return ofNullable(contract.getInvoicing())
			.filter(invoicing -> Objects.equals(ADVANCE, invoicing.getInvoicedIn()))
			.map(Invoicing::getInvoiceInterval)
			.filter(Objects::nonNull)
			.map(Interval::getByIntervalType)
			.map(Interval::getAccrualKey)
			.orElse(null);
	}

	public static int getSplitFactor(Contract contract) {
		return ofNullable(contract)
			.map(c -> ofNullable(c.getInvoicing())
				.map(Invoicing::getInvoiceInterval)
				.filter(Objects::nonNull)
				.map(Interval::getByIntervalType)
				.map(Interval::getSplitFactor)
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Contract %s is missing crucial information for calculating split factor".formatted(contract.getContractId()))))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL));
	}
}
