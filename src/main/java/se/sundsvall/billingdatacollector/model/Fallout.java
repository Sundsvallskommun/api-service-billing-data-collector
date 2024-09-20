package se.sundsvall.billingdatacollector.model;

public record Fallout(
	String familyId,
	String flowInstanceId,
	String municipalityId,
	String requestId) {
}
