package se.sundsvall.billingdatacollector.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
import org.zalando.problem.Status;
import org.zalando.problem.violations.ConstraintViolationProblem;

import se.sundsvall.billingdatacollector.service.CollectorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;

@RestController
@Validated
@Tag(name = "Billing Data Collector", description = "Billing Data Collector resources")
@RequestMapping(path = "/")
@ApiResponse(
	responseCode = "400",
	description = "Bad Request",
	content = @Content(schema = @Schema(oneOf = {Problem.class, ConstraintViolationProblem.class})))
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
		}
	)
	@PostMapping(path = "/trigger/{flowInstanceId}", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	ResponseEntity<Void> triggerBilling(
		@Parameter(name = "flowInstanceId", description = "flowInstanceId to trigger billing for", example = "123")
		@NotEmpty @PathVariable("flowInstanceId") final String flowInstanceId) {

		collectorService.trigger(flowInstanceId);

		return ResponseEntity.accepted().build();
	}

	@Operation(
		summary = "Trigger billing for all flowInstanceId:s between two specific dates",
		responses = {
			@ApiResponse(
				responseCode = "202",
				description = "Accepted",
				useReturnTypeSchema = true)
		}
	)
	@PostMapping(path = "/trigger", produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	ResponseEntity<Void> triggerBilling(
		@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		@Parameter(example = "2024-01-01") final LocalDate startDate,

		@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		@Parameter(example = "2024-02-01") final LocalDate endDate,

		@RequestParam(name = "familyIds", required = false)
		@Parameter(example = "[\"123\", \"456\"]", description = "FamilyIds to trigger billing for. If not provided, billing will be triggered for all supported familyIds")
		final Set<String> familyIds) {

		validateStartDateIsBeforeOrEqualToEndDate(startDate, endDate);

		collectorService.triggerBetweenDates(startDate, endDate, familyIds);

		return ResponseEntity.accepted().build();
	}

	//Validate that the end date is after, or equal to, the start date
	private void validateStartDateIsBeforeOrEqualToEndDate(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			throw Problem.builder()
				.withStatus(Status.BAD_REQUEST)
				.withTitle("Invalid date range")
				.withDetail("Start date must be before end date")
				.build();
		}
	}
}
