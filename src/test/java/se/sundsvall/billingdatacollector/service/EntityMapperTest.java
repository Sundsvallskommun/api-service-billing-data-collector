package se.sundsvall.billingdatacollector.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import se.sundsvall.billingdatacollector.TestDataFactory;

class EntityMapperTest {

	@Test
	void testMapToOpenEFalloutEntity() {
		var falloutEntity = EntityMapper.mapToOpenEFalloutEntity("<åäöÅÄÖ>".getBytes(StandardCharsets.ISO_8859_1), "flowInstanceId", "familyId", "message");

		assertThat(falloutEntity.getFlowInstanceId()).isEqualTo("flowInstanceId");
		assertThat(falloutEntity.getFamilyId()).isEqualTo("familyId");
		assertThat(falloutEntity.getErrorMessage()).isEqualTo("message");
		assertThat(falloutEntity.getOpenEInstance()).isEqualTo("<åäöÅÄÖ>");
	}

	@Test
	void testMapToBillingRecordFalloutEntity() {
		var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		var falloutEntity = EntityMapper.mapToBillingRecordFalloutEntity(wrapper, "message");

		assertThat(falloutEntity.getBillingRecordWrapper()).isEqualTo(wrapper);
		assertThat(falloutEntity.getFamilyId()).isEqualTo(wrapper.getFamilyId());
		assertThat(falloutEntity.getFlowInstanceId()).isEqualTo(wrapper.getFlowInstanceId());
		assertThat(falloutEntity.getErrorMessage()).isEqualTo("message");
	}

	@Test
	void testMapToHistoryEntity() {
		var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		var historyEntity = EntityMapper.mapToHistoryEntity(wrapper, "location");

		assertThat(historyEntity.getBillingRecordWrapper()).isEqualTo(wrapper);
		assertThat(historyEntity.getFamilyId()).isEqualTo(wrapper.getFamilyId());
		assertThat(historyEntity.getFlowInstanceId()).isEqualTo(wrapper.getFlowInstanceId());
		assertThat(historyEntity.getLocation()).isEqualTo("location");
	}

	@Test
	void testMapToScheduledJobEntity() {
		var fromDate = LocalDate.of(2024, 6, 14);
		var toDate = LocalDate.of(2024, 6, 15);
		var scheduledJobEntity = EntityMapper.mapToScheduledJobEntity(fromDate, toDate);

		assertThat(scheduledJobEntity.getFetchedStartDate()).isEqualTo(fromDate);
		assertThat(scheduledJobEntity.getFetchedEndDate()).isEqualTo(toDate);
	}
}
