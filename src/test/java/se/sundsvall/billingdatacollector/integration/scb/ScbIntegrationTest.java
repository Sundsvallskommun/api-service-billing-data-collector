package se.sundsvall.billingdatacollector.integration.scb;

import static java.math.RoundingMode.HALF_EVEN;
import static org.apache.commons.lang3.RandomUtils.secureStrong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.NOT_FOUND;

import generated.se.sundsvall.scb.Dataset;
import generated.se.sundsvall.scb.VariablesSelection;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;

@ExtendWith(MockitoExtension.class)
class ScbIntegrationTest {
	private static final String LANGUAGE = "sv";
	private static final String FORMAT = "json-stat2";

	@Mock
	private ScbClient scbClientMock;

	@Mock
	private KPIBaseYear kpiBaseYearMock;

	@Captor
	private ArgumentCaptor<VariablesSelection> variablesSelectionCaptor;

	@InjectMocks
	private ScbIntegration integration;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(scbClientMock, kpiBaseYearMock);
	}

	@Test
	void getKPIwhenFound() {
		// Arrange
		final var yearMonth = YearMonth.now();
		final var tableId = RandomStringUtils.secureStrong().next(10);
		final var value = secureStrong().randomDouble();
		final var dataSet = new Dataset()
			.addValueItem(value);

		when(kpiBaseYearMock.getTableIdReference()).thenReturn(tableId);
		when(scbClientMock.getKPI(eq(tableId), eq(LANGUAGE), eq(FORMAT), any(VariablesSelection.class))).thenReturn(dataSet);

		// Act
		final var result = integration.getKPI(kpiBaseYearMock, yearMonth);

		// Assert & verify
		verify(kpiBaseYearMock).getTableIdReference();
		verify(scbClientMock).getKPI(eq(tableId), eq(LANGUAGE), eq(FORMAT), variablesSelectionCaptor.capture());

		assertThat(result).isEqualTo(BigDecimal.valueOf(value).setScale(2, HALF_EVEN));
		assertThat(variablesSelectionCaptor.getValue()).satisfies(variableSelection -> {
			assertThat(variableSelection).hasAllNullFieldsOrPropertiesExcept("selection");
			assertThat(variableSelection.getSelection()).hasSize(2)
				.allSatisfy(item -> {
					assertThat(item.getCodelist()).isNull();
				})
				.satisfiesExactlyInAnyOrder(item -> {
					assertThat(item.getVariableCode()).isEqualTo("ContentsCode");
					assertThat(item.getValueCodes()).containsExactly("000004VU");
				}, item -> {
					assertThat(item.getVariableCode()).isEqualTo("Tid");
					assertThat(item.getValueCodes()).containsExactly(yearMonth.format(DateTimeFormatter.ofPattern("yyyy'M'MM")));
				});
		});
	}

	@Test
	void getKPIWhenNotFound() {
		// Arrange
		final var yearMonth = YearMonth.now();
		final var tableId = RandomStringUtils.secureStrong().next(10);
		final var name = RandomStringUtils.secureStrong().next(10);

		when(kpiBaseYearMock.getTableIdReference()).thenReturn(tableId);
		when(kpiBaseYearMock.name()).thenReturn(name);

		// Act
		final var e = assertThrows(ThrowableProblem.class, () -> integration.getKPI(kpiBaseYearMock, yearMonth));

		// Assert & verify
		verify(kpiBaseYearMock).getTableIdReference();
		verify(scbClientMock).getKPI(eq(tableId), eq(LANGUAGE), eq(FORMAT), variablesSelectionCaptor.capture());

		assertThat(e.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(e.getDetail()).isEqualTo("KPI based on %s for period %s was not found".formatted(name, yearMonth));
	}
}
