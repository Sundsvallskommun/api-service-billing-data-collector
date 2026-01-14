package se.sundsvall.billingdatacollector.api;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.accepted;
import static org.zalando.problem.Status.BAD_REQUEST;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;

@RestController
@Validated
@Tag(name = "Billing Data Collector", description = "Billing Data Collector resources")
@RequestMapping(path = "/{municipalityId}/")
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
	@PostMapping(path = "/trigger/{flowInstanceId}", produces = ALL_VALUE)
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
	@PostMapping(path = "/trigger", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<String>> triggerBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2024-01-01") final LocalDate startDate,
		@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2024-02-01") final LocalDate endDate,
		@RequestParam(name = "familyIds", required = false) @Parameter(example = "[\"123\", \"456\"]", description = "FamilyIds to trigger billing for. If not provided, billing will be triggered for all supported familyIds") final Set<String> familyIds) {

		validateStartDateIsBeforeOrEqualToEndDate(startDate, endDate);

		final var processedFlowInstanceIds = collectorService.triggerBillingBetweenDates(startDate, endDate, familyIds);

		return accepted().body(processedFlowInstanceIds);
	}

	@PostMapping(path = "/scheduled-billing", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Adds scheduled billing for source data", responses = {
		@ApiResponse(responseCode = "201", description = "Created", useReturnTypeSchema = true)
	})
	ResponseEntity<ScheduledBilling> addScheduledBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @NotNull @RequestBody ScheduledBilling scheduledBilling) {
		return null;
	}

	@PutMapping(path = "/scheduled-billing/{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Updates scheduled billing", responses = {
		@ApiResponse(responseCode = "200", description = "Successful", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", useReturnTypeSchema = true, content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ScheduledBilling> updateScheduledBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "id of scheduled billing", example = "b82bd8ac-1507-4d9a-958d-369261eecc14") @PathVariable final String id,
		@Valid @NotNull @RequestBody ScheduledBilling scheduledBilling) {
		return null;
	}

	@GetMapping(path = "/scheduled-billing", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get all scheduled billing for source data", responses = {
		@ApiResponse(responseCode = "200", description = "Successful", useReturnTypeSchema = true)
	})
	ResponseEntity<Page<ScheduledBilling>> getScheduledBillings(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@ParameterObject final Pageable pageable) {
		return null;
	}

	@GetMapping(path = "/scheduled-billing/{id}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get scheduled billing by id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", useReturnTypeSchema = true, content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ScheduledBilling> getScheduledBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "id of scheduled billing", example = "b82bd8ac-1507-4d9a-958d-369261eecc14") @PathVariable final String id) {
		return null;
	}

	@DeleteMapping(path = "/scheduled-billing/{id}", produces = ALL_VALUE)
	@Operation(summary = "Get scheduled billing by id", responses = {
		@ApiResponse(responseCode = "204", description = "Successful deletion", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", useReturnTypeSchema = true, content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<Void> deleteScheduledBilling(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "id of scheduled billing", example = "b82bd8ac-1507-4d9a-958d-369261eecc14") @PathVariable final String id) {
		return null;
	}

	@GetMapping(path = "/scheduled-billing/external/{source}/{externalId}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Get scheduled billing by external id", responses = {
		@ApiResponse(responseCode = "200", description = "Successful", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", useReturnTypeSchema = true, content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	ResponseEntity<ScheduledBilling> getScheduledBillingExternalId(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "source", description = "Source system where data is collected", example = "CONTRACT") @PathVariable final BillingSource billingSource,
		@Parameter(name = "externalId", description = "externalId of scheduled billing", example = "b82bd8ac-1507-4d9a-958d-369261eecc14") @PathVariable final String externalId) {
		return null;
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
