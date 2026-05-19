package se.sundsvall.billingdatacollector.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "SCB KPI lookup response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class ScbKpiResponse {

	@Schema(description = "KPI base year", example = "KPI_80", requiredMode = REQUIRED)
	private KPIBaseYear baseYear;

	@Schema(description = "Period (year-month) the KPI value applies to", example = "2024-10", requiredMode = REQUIRED, type = "string")
	private YearMonth period;

	@Schema(description = "KPI value, rounded to two decimals", example = "355.91", requiredMode = REQUIRED)
	private BigDecimal value;
}
