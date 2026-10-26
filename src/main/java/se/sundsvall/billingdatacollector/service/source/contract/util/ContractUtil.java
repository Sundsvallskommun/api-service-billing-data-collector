package se.sundsvall.billingdatacollector.service.source.contract.util;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.ExtraParameterGroup;
import generated.se.sundsvall.contract.Fees;
import generated.se.sundsvall.contract.Invoicing;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;
import se.sundsvall.billingdatacollector.service.source.contract.model.Interval;
import se.sundsvall.dept44.problem.Problem;

import static generated.se.sundsvall.contract.InvoicedIn.ADVANCE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;

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

	public static List<String> getDetailedDescriptions(Contract contract) {
		if (isNull(contract)) {
			throw Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL);
		}

		return ofNullable(contract.getExtraParameters()).orElse(emptyList()).stream()
			.filter(group -> Objects.nonNull(group.getName()))
			.filter(group -> Strings.CI.equals("InvoiceInfo", group.getName()))
			.map(ExtraParameterGroup::getParameters)
			.filter(Objects::nonNull)
			.findFirst()
			.map(params -> params.entrySet().stream()
				.filter(entry -> entry.getKey().matches("detailedDescription\\d{1,3}"))
				.sorted(Map.Entry.comparingByKey())
				.map(Map.Entry::getValue)
				.filter(StringUtils::isNotBlank)
				.toList())
			.orElse(emptyList());
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
			.map(Interval::getByIntervalType)
			.map(Interval::getAccrualKey)
			.orElse(null);
	}

	/**
	 * Single source of truth for whether a contract should have a scheduled
	 * billing at all: it needs an invoicing interval, and billing must not be
	 * explicitly disabled. A missing {@code billingEnabled} value is treated
	 * as enabled (not disabled) so that a contract whose source data has not
	 * yet been backfilled with the field keeps its existing behaviour, while
	 * an explicit {@code false} overrides every other rule.
	 */
	public static boolean isBillable(Contract contract) {
		return ofNullable(contract.getInvoicing())
			.filter(invoicing -> invoicing.getInvoiceInterval() != null)
			.isPresent()
			&& !isBillingDisabled(contract);
	}

	/**
	 * {@code true} only when {@code invoicing.billingEnabled} is explicitly
	 * {@code false}. {@code null} (field absent) is deliberately not treated
	 * as disabled.
	 */
	public static boolean isBillingDisabled(Contract contract) {
		return ofNullable(contract.getInvoicing())
			.map(Invoicing::getBillingEnabled)
			.filter(Boolean.FALSE::equals)
			.isPresent();
	}

	/**
	 * {@code true} when a YEARLY-billed contract's
	 * {@code invoicing.yearlyBillingMonth} is 6 (June). Any other value,
	 * including {@code null}, means the yearly billing slot and invoice
	 * period default to December.
	 */
	public static boolean isJuneYearlyBilling(Contract contract) {
		return ofNullable(contract.getInvoicing())
			.map(Invoicing::getYearlyBillingMonth)
			.filter(month -> month == Month.JUNE.getValue())
			.isPresent();
	}

	public static int getSplitFactor(Contract contract) {
		return ofNullable(contract)
			.map(c -> ofNullable(c.getInvoicing())
				.map(Invoicing::getInvoiceInterval)
				.map(Interval::getByIntervalType)
				.map(Interval::getSplitFactor)
				.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Contract %s is missing crucial information for calculating split factor".formatted(contract.getContractId()))))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, MESSAGE_CONTRACT_CAN_NOT_BE_NULL));
	}
}
