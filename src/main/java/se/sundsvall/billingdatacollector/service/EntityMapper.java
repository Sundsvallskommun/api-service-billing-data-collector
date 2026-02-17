package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.dept44.requestid.RequestId;

public final class EntityMapper {

	private static final String PARAMETER_KEY_CONTRACT_ID = "contractId";

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

	public static HistoryEntity mapToHistoryEntity(String municipalityId, BillingRecord billingRecord, String location) {
		return HistoryEntity.builder()
			.withBillingRecordWrapper(toBillingRecordWrapper(municipalityId, billingRecord))
			.withMunicipalityId(municipalityId)
			.withContractId(getContractId(billingRecord))
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

	public static ScheduledBilling toScheduledBilling(ScheduledBillingEntity entity) {
		return ScheduledBilling.builder()
			.withId(entity.getId())
			.withExternalId(entity.getExternalId())
			.withSource(entity.getSource())
			.withBillingDaysOfMonth(entity.getBillingDaysOfMonth())
			.withBillingMonths(entity.getBillingMonths())
			.withLastBilled(entity.getLastBilled())
			.withNextScheduledBilling(entity.getNextScheduledBilling())
			.withPaused(entity.isPaused())
			.build();
	}

	public static ScheduledBillingEntity toScheduledBillingEntity(String municipalityId, ScheduledBilling dto, LocalDate nextScheduledBilling) {
		return ScheduledBillingEntity.builder()
			.withMunicipalityId(municipalityId)
			.withExternalId(dto.getExternalId())
			.withSource(dto.getSource())
			.withBillingDaysOfMonth(dto.getBillingDaysOfMonth())
			.withBillingMonths(dto.getBillingMonths())
			.withNextScheduledBilling(nextScheduledBilling)
			.withPaused(Optional.ofNullable(dto.getPaused()).orElse(false))
			.build();
	}

	private static BillingRecordWrapper toBillingRecordWrapper(String municipalityId, BillingRecord billingRecord) {
		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withContractId(getContractId(billingRecord))
			.withMunicipalityId(municipalityId)
			.build();
	}

	private static String getContractId(BillingRecord billingRecord) {
		return Optional.ofNullable(billingRecord.getExtraParameters())
			.map(parameters -> parameters.get(PARAMETER_KEY_CONTRACT_ID))
			.orElse(null);
	}
}
