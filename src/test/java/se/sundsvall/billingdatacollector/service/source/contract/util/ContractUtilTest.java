package se.sundsvall.billingdatacollector.service.source.contract.util;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.ExtraParameterGroup;
import generated.se.sundsvall.contract.Fees;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.InvoicedIn;
import generated.se.sundsvall.contract.Invoicing;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;

@ExtendWith(MockitoExtension.class)
class ContractUtilTest {
	private static final String CONTRACT_ID_TEMPLATE = "%s (%s)";

	private static final String CONTRACT_ID = "contractId";

	@Mock
	private Contract contractMock;

	@Test
	void getContractIdFromNull() {
		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getContractId(null));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Parameter 'contract' can not be null");
	}

	@Test
	void getContractIdWhenExternalIdIsPresent() {
		final var externalReferenceId = "externalReferenceId";

		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getExternalReferenceId()).thenReturn(externalReferenceId);

		assertThat(ContractUtil.getContractId(contractMock)).isEqualTo(CONTRACT_ID_TEMPLATE.formatted(CONTRACT_ID, externalReferenceId));

		verify(contractMock).getContractId();
		verify(contractMock).getExternalReferenceId();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		" ", ""
	})
	@NullSource
	void getContractIdWhenExternalIdIsAbsent(String externalReferenceId) {
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getExternalReferenceId()).thenReturn(externalReferenceId);

		assertThat(ContractUtil.getContractId(contractMock)).isEqualTo(CONTRACT_ID);

		verify(contractMock).getContractId();
		verify(contractMock).getExternalReferenceId();
	}

	@Test
	void isIndexedFromNull() {
		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.isIndexed(null));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Parameter 'contract' can not be null");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("isIndexedArgumentProvider")
	void isIndexed(String description, Fees fees, boolean expectedValue) {
		when(contractMock.getFees()).thenReturn(fees);

		assertThat(ContractUtil.isIndexed(contractMock)).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> isIndexedArgumentProvider() {
		return Stream.of(
			Arguments.of("Fees is null", null, false),
			Arguments.of("IndexType is null", new Fees(), false),
			Arguments.of("IndexType is present but empty string", new Fees().indexType(""), false),
			Arguments.of("IndexType is present but blank string", new Fees().indexType(" "), false),
			Arguments.of("IndexType is present with value", new Fees().indexType("value"), true));
	}

	@Test
	void getKPIBaseYearFromNull() {
		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getKPIBaseYear(null));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Parameter 'contract' can not be null");
	}

	@Test
	void getKPIBaseYearWithFeesNull() {
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);

		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getKPIBaseYear(contractMock));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId has no defined index type");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"", " "
	})
	void getKPIBaseYearWithIndexTypeValue(String indexType) {
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getFees()).thenReturn(new Fees().indexType(indexType));

		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getKPIBaseYear(contractMock));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId has no defined index type");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getKPIBaseYearArgumentProvider")
	void getKPIBaseYear(String description, Fees fees, KPIBaseYear expectedValue) {
		when(contractMock.getFees()).thenReturn(fees);

		assertThat(ContractUtil.getKPIBaseYear(contractMock)).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getKPIBaseYearArgumentProvider() {
		return Stream.of(
			Arguments.of("IndexType is present with random value", new Fees().indexType(RandomStringUtils.secure().next(10)), KPIBaseYear.KPI_80),
			Arguments.of("IndexType is present with KPI 80 as upper case", new Fees().indexType("KPI 80"), KPIBaseYear.KPI_80),
			Arguments.of("IndexType is present with KPI 80 as lower case", new Fees().indexType("kpi 80"), KPIBaseYear.KPI_80),
			Arguments.of("IndexType is present with KPI 2020 as upper case", new Fees().indexType("KPI 2020"), KPIBaseYear.KPI_2020),
			Arguments.of("IndexType is present with KPI 2020 as lower case", new Fees().indexType("kpi 2020"), KPIBaseYear.KPI_2020));
	}

	@Test
	void getExtraParameterFromNull() {
		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getExtraParameter(null, null, null));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Parameter 'contract' can not be null");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getExtraParameterArgumentProvider")
	void getExtraParameter(String description, List<ExtraParameterGroup> extraParameterGroup, String parameterGroup, String key, String expectedValue) {
		when(contractMock.getExtraParameters()).thenReturn(extraParameterGroup);

		assertThat(ContractUtil.getExtraParameter(contractMock, parameterGroup, key)).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getExtraParameterArgumentProvider() {
		final var parameterGroup = "parameterGroup";
		final var key = "key";
		final var value = "value";

		return Stream.of(
			Arguments.of("ParameterGroup is null", null, parameterGroup, key, null),
			Arguments.of("ParameterGroup is empty", emptyList(), parameterGroup, key, null),
			Arguments.of("ParameterGroup has no matching group", List.of(new ExtraParameterGroup().name("nonMatchingGroupname").parameters(Map.of(key, value))), parameterGroup, key, null),
			Arguments.of("ParameterGroup has matching group but no parameters map", List.of(new ExtraParameterGroup().name(parameterGroup)), parameterGroup, key, null),
			Arguments.of("ParameterGroup has matching group and parameters map but no matching key", List.of(new ExtraParameterGroup().name(parameterGroup).parameters(Map.of("nonMatchingKey", value))), parameterGroup, key, null),
			Arguments.of("ParameterGroup has matching group and key", List.of(new ExtraParameterGroup().name(parameterGroup).parameters(Map.of(key, value))), parameterGroup, key, value));
	}

	@Test
	void getAccrualKeyFromNull() {
		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getAccrualKey(null));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Parameter 'contract' can not be null");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getAccrualKeyArgumentProvider")
	void getAccrualKey(String description, Invoicing invoicing, String expectedValue) {
		when(contractMock.getInvoicing()).thenReturn(invoicing);

		assertThat(ContractUtil.getAccrualKey(contractMock)).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getAccrualKeyArgumentProvider() {
		return Stream.of(
			Arguments.of("Invoicing is null", null, null),
			Arguments.of("Invoicing present but invoiced is null", new Invoicing(), null),
			Arguments.of("Invoicing present but invoiced in does not match ADVANCE", new Invoicing().invoicedIn(InvoicedIn.ARREARS), null),
			Arguments.of("Invoicing present and invoiced match but no invoice interval is present", new Invoicing().invoicedIn(InvoicedIn.ADVANCE), null),
			Arguments.of("Invoicing present and invoiced match with monthly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.MONTHLY), "N_1"),
			Arguments.of("Invoicing present and invoiced match with quarterly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.QUARTERLY), "N_4"),
			Arguments.of("Invoicing present and invoiced match with half-yearly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.HALF_YEARLY), "N_6"),
			Arguments.of("Invoicing present and invoiced match with yearly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.YEARLY), "N_12"));
	}

	@Test
	void getSplitFactorFromNull() {
		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getSplitFactor(null));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Parameter 'contract' can not be null");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getSplitFactorWithMissingInformationArgumentProvider")
	void getSplitFactorWithMissingInformation(String description, Invoicing invoicing) {
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getInvoicing()).thenReturn(invoicing);

		final var e = assertThrows(ThrowableProblem.class, () -> ContractUtil.getSplitFactor(contractMock));
		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("Contract contractId is missing crucial information for calculating split factor");
	}

	private static Stream<Arguments> getSplitFactorWithMissingInformationArgumentProvider() {
		return Stream.of(
			Arguments.of("Invoicing is null", null),
			Arguments.of("Invoicing present but invoiced is null", new Invoicing()),
			Arguments.of("Invoicing present but invoiced in does not match ADVANCE", new Invoicing().invoicedIn(InvoicedIn.ARREARS)),
			Arguments.of("Invoicing present and invoiced match but no invoice interval is present", new Invoicing().invoicedIn(InvoicedIn.ADVANCE)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("getSplitFactorArgumentProvider")
	void getSplitFactor(String description, Invoicing invoicing, int expectedValue) {
		when(contractMock.getInvoicing()).thenReturn(invoicing);

		assertThat(ContractUtil.getSplitFactor(contractMock)).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getSplitFactorArgumentProvider() {
		return Stream.of(
			Arguments.of("Invoicing present and invoiced match with monthly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.MONTHLY), 12),
			Arguments.of("Invoicing present and invoiced match with quarterly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.QUARTERLY), 4),
			Arguments.of("Invoicing present and invoiced match with half yearly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.HALF_YEARLY), 2),
			Arguments.of("Invoicing present and invoiced match with yearly invoice interval", new Invoicing().invoicedIn(InvoicedIn.ADVANCE).invoiceInterval(IntervalType.YEARLY), 1));
	}
}
