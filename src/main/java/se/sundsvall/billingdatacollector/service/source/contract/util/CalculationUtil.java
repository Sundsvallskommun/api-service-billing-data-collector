package se.sundsvall.billingdatacollector.service.source.contract.util;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Fees;
import java.math.BigDecimal;
import java.util.Optional;
import se.sundsvall.dept44.problem.Problem;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getSplitFactor;

public final class CalculationUtil {

	private CalculationUtil() {
		// Prevent instansiation
	}

	/**
	 * Calculates the indexed cost for a contract based on yearly fees, split factor and KPI index adjustment.
	 *
	 * <p>
	 * The calculation is performed in three logical steps:
	 * </p>
	 * <ol>
	 * <li>Apply KPI index adjustment to {@code fees.yearly} to get the indexed yearly amount</li>
	 * <li>Use the higher of the indexed yearly amount and {@code fees.yearly} — the invoice amount
	 * never falls below the contract base fee due to a declining index</li>
	 * <li>Divide by the split factor and round to two decimals to get the periodic invoice amount</li>
	 * </ol>
	 *
	 * <p>
	 * If any required data is missing the calculation fails and a {@code NOT_FOUND} problem is raised.
	 * </p>
	 *
	 * @param  contract       the contract containing fee and index information
	 * @param  currentYearKPI the KPI value for the current year
	 * @return                the calculated indexed cost
	 * @throws Problem        if required contract or fee data is missing
	 */
	public static BigDecimal calculateIndexedCost(Contract contract, BigDecimal currentYearKPI) {
		final var splitFactor = BigDecimal.valueOf(getSplitFactor(contract));

		return ofNullable(contract.getFees())
			// Step 1: Calculate indexed yearly amount by applying KPI adjustment to fees.yearly
			.flatMap(fees -> calculateIndexedYearlyAmount(fees.getIndexNumber(), fees.getIndexationRate(), fees.getYearly(), currentYearKPI))
			// Step 2: Use the higher of indexed yearly amount and base yearly fee
			.map(indexedYearly -> indexedYearly.max(contract.getFees().getYearly()))
			// Step 3: Divide by split factor and round to two decimals
			.map(yearlyAmount -> yearlyAmount.divide(splitFactor, 2, HALF_EVEN))
			// Fail fast if any required information is missing
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Contract %s is missing crucial information for calculating indexed cost".formatted(contract.getContractId())));
	}

	/**
	 * Applies KPI index adjustment to the yearly fee.
	 *
	 * <p>
	 * The adjustment factor is calculated as:
	 * </p>
	 *
	 * <pre>
	 * yearlyFee * (1 + indexationRate * (currentYearKPI / baseIndex - 1))
	 * </pre>
	 *
	 * <p>
	 * Returns an empty {@link Optional} if any required value is missing or the base index is zero.
	 * </p>
	 *
	 * @param  index          base index to use
	 * @param  indexationRate indexation rate to use
	 * @param  yearlyFee      the yearly base fee
	 * @param  currentYearKPI KPI value for the current year
	 * @return                optional indexed yearly amount
	 */
	private static Optional<BigDecimal> calculateIndexedYearlyAmount(BigDecimal index, BigDecimal indexationRate, BigDecimal yearlyFee, BigDecimal currentYearKPI) {
		if (anyNull(index, indexationRate, yearlyFee, currentYearKPI)) {
			return Optional.empty();
		}

		return Optional.of(index)
			.filter(baseIndex -> notEqual(ZERO, baseIndex))
			.map(baseIndex -> currentYearKPI.divide(baseIndex, 10, HALF_EVEN))
			.map(kpiRatio -> kpiRatio.subtract(BigDecimal.ONE))
			.map(indexationRate::multiply)
			.map(kpiDelta -> kpiDelta.add(BigDecimal.ONE))
			.map(yearlyFee::multiply);
	}

	/**
	 * Non indexed cost is calculated according to following formula:
	 *
	 * ( fees.yearly / splitFactor )
	 *
	 * The result of formula is rounded to two decimals and returned
	 *
	 * @param  contract the contract containing fee information
	 * @return          the calculated non indexed cost
	 */
	public static BigDecimal calculateNonIndexedCost(Contract contract) {
		return ofNullable(contract.getFees())
			.map(Fees::getYearly)
			.map(yearlyFee -> yearlyFee.divide(BigDecimal.valueOf(getSplitFactor(contract)), 10, HALF_EVEN))
			.map(result -> result.setScale(2, HALF_EVEN))
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Contract %s is missing crucial information for calculating non indexed cost".formatted(contract.getContractId())));
	}
}
