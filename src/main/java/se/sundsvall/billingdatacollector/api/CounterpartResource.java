package se.sundsvall.billingdatacollector.api;

import generated.se.sundsvall.contract.StakeholderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.billingdatacollector.service.source.contract.CounterpartMappingService;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.common.validators.annotation.ValidUuid;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@Validated
@Tag(name = "Counterpart", description = "Counterpart resources")
@RequestMapping(path = "/{municipalityId}/counterpart")
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
class CounterpartResource {

	private final CounterpartMappingService counterpartMappingService;

	CounterpartResource(CounterpartMappingService counterpartMappingService) {
		this.counterpartMappingService = counterpartMappingService;
	}

	@Operation(
		summary = "Find counterpart by partyId and stakeholderType",
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
	@GetMapping(produces = TEXT_PLAIN_VALUE)
	ResponseEntity<String> getCounterpart(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "partyId", description = "Party id", example = "fb2f0290-3820-11ed-a261-0242ac120002") @ValidUuid @RequestParam final String partyId,
		@Parameter(name = "stakeholderType", description = "Stakeholder type", example = "PERSON") @RequestParam(required = false) final String stakeholderType) {

		validateStakeholderType(stakeholderType);

		return ok(counterpartMappingService.findCounterpart(municipalityId, partyId, stakeholderType));
	}

	private void validateStakeholderType(String stakeholderType) {

		if (stakeholderType != null) {
			try {
				StakeholderType.fromValue(stakeholderType);
			} catch (IllegalArgumentException _) {
				throw Problem.builder()
					.withStatus(BAD_REQUEST)
					.withTitle("Invalid stakeholder type")
					.withDetail("stakeholderType must be one of: " + Arrays.stream(StakeholderType.values())
						.map(StakeholderType::getValue)
						.collect(Collectors.joining(", ")))
					.build();
			}
		}
	}
}
