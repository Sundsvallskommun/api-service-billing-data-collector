package se.sundsvall.billingdatacollector.service.source.contract.model;

import lombok.Builder;
import lombok.With;

@With
@Builder(setterPrefix = "with")
public record LeaseTypeSettings(
	String costCenter,
	String subAccount,
	String vatCode,
	String department,
	String activity) {
}
