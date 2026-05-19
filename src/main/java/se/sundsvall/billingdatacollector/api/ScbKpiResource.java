package se.sundsvall.billingdatacollector.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.billingdatacollector.api.model.ScbKpiResponse;
import se.sundsvall.billingdatacollector.integration.scb.ScbIntegration;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@Tag(name = "SCB KPI", description = "Statistics Sweden KPI lookup")
@RequestMapping(path = "/{municipalityId}/scb/kpi")
@ApiResponse(
	responseCode = "400",
	description = "Bad Request",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		})))
@ApiResponse(
	responseCode = "500",
	description = "Internal Server Error",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(implementation = Problem.class)))
@ApiResponse(
	responseCode = "502",
	description = "Bad Gateway",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(implementation = Problem.class)))
class ScbKpiResource {

	private final ScbIntegration scbIntegration;

	ScbKpiResource(ScbIntegration scbIntegration) {
		this.scbIntegration = scbIntegration;
	}

	@Operation(
		summary = "Look up an SCB KPI value for a given base year and period",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "Successful",
				useReturnTypeSchema = true),
			@ApiResponse(
				responseCode = "404",
				description = "Not Found",
				content = @Content(
					mediaType = APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = Problem.class)))
		})
	@GetMapping(produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ScbKpiResponse> getKpi(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "baseYear", description = "KPI base year", example = "KPI_80") @RequestParam final KPIBaseYear baseYear,
		@Parameter(name = "period", description = "Period (year-month) to look up", example = "2024-10") @RequestParam @DateTimeFormat(pattern = "yyyy-MM") final YearMonth period) {

		return ok(ScbKpiResponse.builder()
			.withBaseYear(baseYear)
			.withPeriod(period)
			.withValue(scbIntegration.getKPI(baseYear, period))
			.build());
	}
}
