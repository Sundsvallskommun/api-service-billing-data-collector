package se.sundsvall.billingdatacollector.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Billing source event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class EventRequest {

	@Schema(description = "External id", example = "2026-00001", requiredMode = REQUIRED)
	@NotBlank
	private String id;

	@Schema(description = "Municipality id (taken from the path parameter when this event is delivered via the events endpoint)", example = "2281")
	private String municipalityId;

	@Schema(description = "Event type", example = "CREATED", requiredMode = REQUIRED)
	@NotNull
	private EventType eventType;
}
