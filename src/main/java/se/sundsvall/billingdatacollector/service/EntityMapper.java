package se.sundsvall.billingdatacollector.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.dept44.requestid.RequestId;

final class EntityMapper {

	private EntityMapper() {}

	public static FalloutEntity mapToOpenEFalloutEntity(byte[] bytes, String flowInstanceId, String familyId, String municipalityId, String message) {
		return FalloutEntity.builder()
			.withMunicipalityId(municipalityId)
			.withOpenEInstance(new String(bytes, StandardCharsets.ISO_8859_1))
			.withFlowInstanceId(flowInstanceId)
			.withFamilyId(familyId)
			.withErrorMessage(message)
			.withRequestId(RequestId.get())
			.build();
	}

	public static FalloutEntity mapToBillingRecordFalloutEntity(BillingRecordWrapper wrapper, String message) {
		return FalloutEntity.builder()
			.withBillingRecordWrapper(wrapper)
			.withFamilyId(wrapper.getFamilyId())
			.withMunicipalityId(wrapper.getMunicipalityId())
			.withFlowInstanceId(wrapper.getFlowInstanceId())
			.withErrorMessage(message)
			.withRequestId(RequestId.get())
			.build();
	}

	public static HistoryEntity mapToHistoryEntity(BillingRecordWrapper wrapper, String location) {
		return HistoryEntity.builder()
			.withBillingRecordWrapper(wrapper)
			.withFamilyId(wrapper.getFamilyId())
			.withMunicipalityId(wrapper.getMunicipalityId())
			.withFlowInstanceId(wrapper.getFlowInstanceId())
			.withLocation(location)
			.withRequestId(RequestId.get())
			.build();
	}

	public static ScheduledJobEntity mapToScheduledJobEntity(LocalDate startDate, LocalDate endDate) {
		return ScheduledJobEntity.builder()
			.withMunicipalityId(BillingRecordConstants.SUNDSVALLS_MUNICIPALITY_ID)
			.withFetchedStartDate(startDate)
			.withFetchedEndDate(endDate)
			.build();
	}
}
