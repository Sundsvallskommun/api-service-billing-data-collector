package se.sundsvall.billingdatacollector.service.source.contract.util;

import static generated.se.sundsvall.contract.IntervalType.HALF_YEARLY;
import static generated.se.sundsvall.contract.IntervalType.MONTHLY;
import static generated.se.sundsvall.contract.IntervalType.QUARTERLY;
import static generated.se.sundsvall.contract.IntervalType.YEARLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Fees;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.Invoicing;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

@ExtendWith(MockitoExtension.class)
class CalculationUtilTest {

	@Mock
	private Contract contractMock;

	@Mock
	private Fees feesMock;

	@Mock
	private Invoicing invoicingMock;

	@ParameterizedTest(name = "{0}")
	@MethodSource("calculateIndexedCostArgumentProvider")
	void calculateIndexedCost(String description, IntervalType intervalType, BigDecimal yearlyFee, Integer indexNumber, BigDecimal indexationRate, BigDecimal currentYearKPI, BigDecimal expectedValue) {
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearlyFee);
		when(feesMock.getIndexNumber()).thenReturn(indexNumber);
		when(feesMock.getIndexationRate()).thenReturn(indexationRate);
		when(invoicingMock.getInvoiceInterval()).thenReturn(intervalType);

		assertThat(CalculationUtil.calculateIndexedCost(contractMock, currentYearKPI)).isEqualTo(expectedValue.setScale(2));
	}

	private static Stream<Arguments> calculateIndexedCostArgumentProvider() {
		final var yearlyFee = BigDecimal.valueOf(12000);
		final var originalKPI = 100;
		final var indexationRate_1 = BigDecimal.valueOf(1);
		final var indexationRate_05 = BigDecimal.valueOf(0.5);
		final var currentYearKPIEqual = BigDecimal.valueOf(100);
		final var currentYearKPIDouble = BigDecimal.valueOf(200);
		final var currentYearKPIHalf = BigDecimal.valueOf(50);

		return Stream.of(
			Arguments.of("Yearly interval with current index twice of begin index and indexationRate 1", YEARLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIDouble, BigDecimal.valueOf(24000)),
			Arguments.of("Yearly interval with same current index as begin index and indexationRate 1", YEARLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIEqual, BigDecimal.valueOf(12000)),
			Arguments.of("Yearly interval with current index half of begin index and indexationRate 1", YEARLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIHalf, BigDecimal.valueOf(6000)),

			Arguments.of("Half yearly interval with current index twice of begin index and indexationRate 1", HALF_YEARLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIDouble, BigDecimal.valueOf(12000)),
			Arguments.of("Half yearly interval with same current index as begin index and indexationRate 1", HALF_YEARLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIEqual, BigDecimal.valueOf(6000)),
			Arguments.of("Half yearly interval with current index half of begin index and indexationRate 1", HALF_YEARLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIHalf, BigDecimal.valueOf(3000)),

			Arguments.of("Quarterly interval with current index twice of begin index and indexationRate 1", QUARTERLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIDouble, BigDecimal.valueOf(6000)),
			Arguments.of("Quarterly interval with same current index as begin index and indexationRate 1", QUARTERLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIEqual, BigDecimal.valueOf(3000)),
			Arguments.of("Quarterly interval with current index half of begin index and indexationRate 1", QUARTERLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIHalf, BigDecimal.valueOf(1500)),

			Arguments.of("Monthly interval with current index twice of begin index and indexationRate 1", MONTHLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIDouble, BigDecimal.valueOf(2000)),
			Arguments.of("Monthly interval with same current index as begin index and indexationRate 1", MONTHLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIEqual, BigDecimal.valueOf(1000)),
			Arguments.of("Monthly interval with current index half of begin index and indexationRate 1", MONTHLY, yearlyFee, originalKPI, indexationRate_1, currentYearKPIHalf, BigDecimal.valueOf(500)),

			Arguments.of("Yearly interval with current index twice of begin index and indexationRate 0.5", YEARLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIDouble, BigDecimal.valueOf(18000)),
			Arguments.of("Yearly interval with same current index as begin index and indexationRate 0.5", YEARLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIEqual, BigDecimal.valueOf(12000)),
			Arguments.of("Yearly interval with current index half of begin index and indexationRate 0.5", YEARLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIHalf, BigDecimal.valueOf(9000)),

			Arguments.of("Half yearly interval with current index twice of begin index and indexationRate 0.5", HALF_YEARLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIDouble, BigDecimal.valueOf(9000)),
			Arguments.of("Half yearly interval with same current index as begin index and indexationRate 0.5", HALF_YEARLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIEqual, BigDecimal.valueOf(6000)),
			Arguments.of("Half yearly interval with current index half of begin index and indexationRate 0.5", HALF_YEARLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIHalf, BigDecimal.valueOf(4500)),

			Arguments.of("Quarterly interval with current index twice of begin index and indexationRate 0.5", QUARTERLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIDouble, BigDecimal.valueOf(4500)),
			Arguments.of("Quarterly interval with same current index as begin index and indexationRate 0.5", QUARTERLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIEqual, BigDecimal.valueOf(3000)),
			Arguments.of("Quarterly interval with current index half of begin index and indexationRate 0.5", QUARTERLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIHalf, BigDecimal.valueOf(2250)),

			Arguments.of("Monthly interval with current index twice of begin index and indexationRate 0.5", MONTHLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIDouble, BigDecimal.valueOf(1500)),
			Arguments.of("Monthly interval with same current index as begin index and indexationRate 0.5", MONTHLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIEqual, BigDecimal.valueOf(1000)),
			Arguments.of("Monthly interval with current index half of begin index and indexationRate 0.5", MONTHLY, yearlyFee, originalKPI, indexationRate_05, currentYearKPIHalf, BigDecimal.valueOf(750)));
	}

	@Test
	void calculateIndexedCostWithMissingInvoicing() {
		final var currentIndex = BigDecimal.valueOf(100);

		when(contractMock.getContractId()).thenReturn("contractId");

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateIndexedCost(contractMock, currentIndex));
		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating split factor");
	}

	@Test
	void calculateIndexedCostWithMissingInvoiceInterval() {
		final var currentIndex = BigDecimal.valueOf(100);

		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateIndexedCost(contractMock, currentIndex));
		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating split factor");
	}

	@Test
	void calculateIndexedCostWithMissingIndexationRate() {
		final var currentIndex = BigDecimal.valueOf(100);

		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(BigDecimal.valueOf(12000));
		when(feesMock.getIndexNumber()).thenReturn(100);
		when(invoicingMock.getInvoiceInterval()).thenReturn(YEARLY);

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateIndexedCost(contractMock, currentIndex));
		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating indexed cost");
	}

	@Test
	void calculateIndexedCostWithMissingIndexNumber() {
		final var currentIndex = BigDecimal.valueOf(100);

		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(BigDecimal.valueOf(12000));
		when(feesMock.getIndexationRate()).thenReturn(BigDecimal.valueOf(1));
		when(invoicingMock.getInvoiceInterval()).thenReturn(YEARLY);

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateIndexedCost(contractMock, currentIndex));
		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating indexed cost");
	}

	@Test
	void calculateIndexedCostWithMissingYearlyCost() {
		final var currentIndex = BigDecimal.valueOf(100);

		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(contractMock.getFees()).thenReturn(feesMock);
		when(invoicingMock.getInvoiceInterval()).thenReturn(YEARLY);

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateIndexedCost(contractMock, currentIndex));
		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating indexed cost");
	}

	@Test
	void calculateIndexedCostWithMissingFees() {
		final var currentIndex = BigDecimal.valueOf(100);

		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(invoicingMock.getInvoiceInterval()).thenReturn(YEARLY);

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateIndexedCost(contractMock, currentIndex));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating indexed cost");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("calculateNonIndexedCostArgumentProvider")
	void calculateNonIndexedCost(String description, IntervalType intervalType, BigDecimal yearlyFee, BigDecimal expectedValue) {
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearlyFee);

		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearlyFee);
		when(invoicingMock.getInvoiceInterval()).thenReturn(intervalType);

		assertThat(CalculationUtil.calculateNonIndexedCost(contractMock)).isEqualTo(expectedValue.setScale(2));
	}

	private static Stream<Arguments> calculateNonIndexedCostArgumentProvider() {
		return Stream.of(
			Arguments.of("Yearly interval", YEARLY, BigDecimal.valueOf(12000), BigDecimal.valueOf(12000)),
			Arguments.of("Half yearly interval", HALF_YEARLY, BigDecimal.valueOf(12000), BigDecimal.valueOf(6000)),
			Arguments.of("Quarterly interval", QUARTERLY, BigDecimal.valueOf(12000), BigDecimal.valueOf(3000)),
			Arguments.of("Monthly interval", MONTHLY, BigDecimal.valueOf(12000), BigDecimal.valueOf(1000))

		);
	}

	@Test
	void calculateNonIndexedCostWithMissingFees() {
		when(contractMock.getContractId()).thenReturn("contractId");

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateNonIndexedCost(contractMock));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating non indexed cost");
	}

	@Test
	void calculateNonIndexedCostWithMissingYearlyFee() {
		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getFees()).thenReturn(feesMock);

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateNonIndexedCost(contractMock));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating non indexed cost");
	}

	@Test
	void calculateNonIndexedCostWithMissingInvoicing() {
		when(contractMock.getContractId()).thenReturn("contractId");
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(BigDecimal.valueOf(10000));

		final var e = assertThrows(ThrowableProblem.class, () -> CalculationUtil.calculateNonIndexedCost(contractMock));

		assertThat(e.getStatus()).isEqualTo(Status.NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating split factor");
	}
}
