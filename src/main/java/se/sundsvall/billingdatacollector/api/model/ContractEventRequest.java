package se.sundsvall.billingdatacollector.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Contract event from contract service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class ContractEventRequest {

	@Schema(description = "Contract id", example = "2026-00001")
	private String id;

	@Schema(description = "Municipality id", example = "2281")
	private String municipalityId;

	@Schema(description = "Event type", example = "CONTRACT_CREATED")
	private ContractEventType eventType;
}
