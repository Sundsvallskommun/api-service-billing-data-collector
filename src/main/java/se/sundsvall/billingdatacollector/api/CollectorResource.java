package se.sundsvall.billingdatacollector.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

@RestController
@Validated
@Tag(name = "Billing Data Collector", description = "Billing Data Collector resources")
@RequestMapping(path = "/{municipalityId}/")
@ApiResponse(
	responseCode = "400",
	description = "Bad Request",
	content = @Content(schema = @Schema(oneOf = { Problem.class, ConstraintViolationProblem.class })))
@ApiResponse(
	responseCode = "500",
	description = "Internal Server Error",
	content = @Content(schema = @Schema(implementation = Problem.class)))
@ApiResponse(
	responseCode = "502",
	description = "Bad Gateway",
	content = @Content(schema = @Schema(implementation = Problem.class)))
class CollectorResource {

	private final CollectorService collectorService;

	CollectorResource(CollectorService collectorService) {
		this.collectorService = collectorService;
	}

	@Operation(
		summary = "Trigger billing for a flowInstanceId",
		responses = {
			@ApiResponse(
				responseCode = "202",
				description = "Accepted",
				useReturnTypeSchema = true)
		})
	@PostMapping(path = "/trigger/{flowInstanceId}", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	ResponseEntity<Void> triggerBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "flowInstanceId", description = "flowInstanceId to trigger billing for", example = "123") @NotEmpty @PathVariable("flowInstanceId") final String flowInstanceId) {

		collectorService.triggerBilling(flowInstanceId);

		return accepted()
			.header(CONTENT_TYPE, ALL_VALUE)
			.build();
	}

	@Operation(
		summary = "Trigger billing for all flowInstanceId:s between two specific dates. Will return a list of all triggered flowInstanceId:s",
		responses = {
			@ApiResponse(
				responseCode = "202",
				description = "Accepted",
				useReturnTypeSchema = true)
		})
	@PostMapping(path = "/trigger", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	ResponseEntity<List<String>> triggerBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2024-01-01") final LocalDate startDate,
		@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2024-02-01") final LocalDate endDate,
		@RequestParam(name = "familyIds", required = false) @Parameter(example = "[\"123\", \"456\"]", description = "FamilyIds to trigger billing for. If not provided, billing will be triggered for all supported familyIds") final Set<String> familyIds) {

		validateStartDateIsBeforeOrEqualToEndDate(startDate, endDate);

		final var processedFlowInstanceIds = collectorService.triggerBillingBetweenDates(startDate, endDate, familyIds);

		return accepted().body(processedFlowInstanceIds);
	}

	// Validate that the end date is after, or equal to, the start date
	private void validateStartDateIsBeforeOrEqualToEndDate(LocalDate startDate, LocalDate endDate) {
		if (startDate.isAfter(endDate)) {
			throw Problem.builder()
				.withStatus(BAD_REQUEST)
				.withTitle("Invalid date range")
				.withDetail("Start date must be before end date")
				.build();
		}
	}
}
