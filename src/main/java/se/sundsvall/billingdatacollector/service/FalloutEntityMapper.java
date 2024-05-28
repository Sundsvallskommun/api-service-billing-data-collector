package se.sundsvall.billingdatacollector.service;

import java.nio.charset.StandardCharsets;

import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

final class FalloutEntityMapper {

	private FalloutEntityMapper() {
	}

	public static FalloutEntity mapToOpenEFalloutEntity(byte[] bytes, String flowInstanceId, String familyId, String message) {
		return FalloutEntity.builder()
			.withOpenEInstance(new String(bytes, StandardCharsets.ISO_8859_1))
			.withFlowInstanceId(flowInstanceId)
			.withFamilyId(familyId)
			.withErrorMessage(message)
			.build();
	}

	public static FalloutEntity mapToBillingRecordFalloutEntity(BillingRecordWrapper wrapper, String message) {
		return FalloutEntity.builder()
			.withBillingRecordWrapper(wrapper)
			.withFamilyId(wrapper.getFamilyId())
			.withFlowInstanceId(wrapper.getFlowInstanceId())
			.withErrorMessage(message)
			.build();
	}

}
