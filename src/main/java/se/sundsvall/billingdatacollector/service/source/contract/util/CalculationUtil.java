package se.sundsvall.billingdatacollector.service.source.contract.util;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_EVEN;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ObjectUtils.anyNull;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.billingdatacollector.service.source.contract.util.ContractUtil.getSplitFactor;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Fees;
import java.math.BigDecimal;
import java.util.Optional;
import org.zalando.problem.Problem;

public final class CalculationUtil {

	private CalculationUtil() {
		// Prevent instansiation
	}

	/**
	 * Calculates the indexed cost for a contract based on yearly fees, split factor and KPI index adjustment.
	 *
	 * <p>
	 * The calculation follows this formula:
	 * </p>
	 *
	 * <pre>
	 * (fees.yearly / splitFactor) * (1 + splitFactor * (currentYearKPI / fees.indexNumber - 1))
	 * </pre>
	 *
	 * <p>
	 * The calculation is performed in three logical steps:
	 * </p>
	 * <ol>
	 * <li>Derive the periodic (split) fee from the yearly fee based on the invoicing interval of the contract</li>
	 * <li>Apply index adjustment based on KPI development</li>
	 * <li>Apply index adjustment based on KPI development</li>
	 * <li>Result is rounded to two decimals</li>
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
			// Step 1: Calculate periodic fee (yearly fee divided by split factor)
			.map(fees -> calculatePeriodicFee(fees.getYearly(), splitFactor))
			.filter(Optional::isPresent)
			.map(Optional::get)
			// Step 2: Apply index adjustment using KPI and base index
			.map(periodicFee -> calculateForIndexAdjustment(BigDecimal.valueOf(contract.getFees().getIndexNumber()), contract.getFees().getIndexationRate(), periodicFee, currentYearKPI))
			.filter(Optional::isPresent)
			.map(Optional::get)
			// Step 3: Round fee to two decimals
			.map(indexAdjustedFee -> indexAdjustedFee.setScale(2, HALF_EVEN))
			// Fail fast if any required information is missing
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "Contract %s is missing crucial information for calculating indexed cost".formatted(contract.getContractId())));
	}

	/**
	 * Calculates the periodic fee by dividing the yearly fee by the split factor.
	 *
	 * <p>
	 * Returns an empty {@link Optional} if the yearly fee is missing.
	 * </p>
	 *
	 * @param  yearlyFee   yearly fee for the contract
	 * @param  splitFactor factor used to split the yearly fee
	 * @return             optional periodic fee
	 */
	private static Optional<BigDecimal> calculatePeriodicFee(BigDecimal yearlyFee, BigDecimal splitFactor) {
		return ofNullable(yearlyFee)
			.map(fee -> fee.divide(splitFactor, 10, HALF_EVEN));
	}

	/**
	 * Applies index adjustment to the periodic fee based on KPI development.
	 *
	 * <p>
	 * The adjustment factor is calculated as:
	 * </p>
	 *
	 * <pre>
	 * 1 + splitFactor * (currentYearKPI / baseIndex - 1)
	 * </pre>
	 *
	 * <p>
	 * The resulting factor is then multiplied with the periodic fee.
	 * </p>
	 *
	 * <p>
	 * Returns an empty {@link Optional} if the base index is missing.
	 * </p>
	 *
	 * @param  index          base index to use
	 * @param  indexationRate indexation rate to use
	 * @param  periodicFee    fee amount after split
	 * @param  currentYearKPI KPI value for the current year
	 * @return                optional indexed fee
	 */

	private static Optional<BigDecimal> calculateForIndexAdjustment(BigDecimal index, BigDecimal indexationRate, BigDecimal periodicFee, BigDecimal currentYearKPI) {
		if (anyNull(index, indexationRate, periodicFee, currentYearKPI)) {
			return Optional.empty();
		}

		return Optional.of(index)
			.filter(baseIndex -> notEqual(ZERO, baseIndex))
			.map(baseIndex -> currentYearKPI.divide(baseIndex, 10, HALF_EVEN))
			.map(kpiRatio -> kpiRatio.subtract(BigDecimal.ONE))
			.map(indexationRate::multiply)
			.map(kpiDelta -> kpiDelta.add(BigDecimal.ONE))
			.map(periodicFee::multiply);
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
