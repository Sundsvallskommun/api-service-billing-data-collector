package se.sundsvall.billingdatacollector.integration.scb;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

class ScbMapperTest {

	@Test
	void toVariablesSelection() {
		final var yearMonth = YearMonth.now();
		final var result = ScbMapper.toVariablesSelection(yearMonth);

		assertThat(result).isNotNull();
		assertThat(result.getPlacement()).isNull();
		assertThat(result.getSelection()).hasSize(2).satisfiesExactlyInAnyOrder(selection -> {
			assertThat(selection.getVariableCode()).isEqualTo("ContentsCode");
			assertThat(selection.getValueCodes()).containsExactly("000004VU");
		}, selection -> {
			assertThat(selection.getVariableCode()).isEqualTo("Tid");
			assertThat(selection.getValueCodes()).containsExactly(DateTimeFormatter.ofPattern("yyyy'M'MM").format(yearMonth));
		});
	}

	@Test
	void toVariablesSelectionFromNull() {
		final var exception = assertThrows(ThrowableProblem.class, () -> ScbMapper.toVariablesSelection(null));

		assertThat(exception.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(exception.getDetail()).isEqualTo("Period must be provided");
	}
}
