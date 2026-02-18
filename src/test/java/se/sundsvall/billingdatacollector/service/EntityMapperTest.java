package se.sundsvall.billingdatacollector.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import se.sundsvall.billingdatacollector.TestDataFactory;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {

	@Test
	void testMapToOpenEFalloutEntity() {
		final var falloutEntity = EntityMapper.mapToOpenEFalloutEntity("<åäöÅÄÖ>".getBytes(StandardCharsets.ISO_8859_1), "flowInstanceId", "familyId", "municipalityId", "message");

		assertThat(falloutEntity.getFlowInstanceId()).isEqualTo("flowInstanceId");
		assertThat(falloutEntity.getFamilyId()).isEqualTo("familyId");
		assertThat(falloutEntity.getMunicipalityId()).isEqualTo("municipalityId");
		assertThat(falloutEntity.getErrorMessage()).isEqualTo("message");
		assertThat(falloutEntity.getOpenEInstance()).isEqualTo("<åäöÅÄÖ>");
	}

	@Test
	void testMapToBillingRecordFalloutEntity() {
		final var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		final var falloutEntity = EntityMapper.mapToBillingRecordFalloutEntity(wrapper, "message");

		assertThat(falloutEntity.getBillingRecordWrapper()).isEqualTo(wrapper);
		assertThat(falloutEntity.getFamilyId()).isEqualTo(wrapper.getFamilyId());
		assertThat(falloutEntity.getFlowInstanceId()).isEqualTo(wrapper.getFlowInstanceId());
		assertThat(falloutEntity.getErrorMessage()).isEqualTo("message");
	}

	@Test
	void testMapToHistoryEntity() {
		final var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		final var historyEntity = EntityMapper.mapToHistoryEntity(wrapper, "location");

		assertThat(historyEntity.getBillingRecordWrapper()).isEqualTo(wrapper);
		assertThat(historyEntity.getFamilyId()).isEqualTo(wrapper.getFamilyId());
		assertThat(historyEntity.getFlowInstanceId()).isEqualTo(wrapper.getFlowInstanceId());
		assertThat(historyEntity.getLocation()).isEqualTo("location");
	}

	@Test
	void testMapToScheduledJobEntity() {
		final var fromDate = LocalDate.of(2024, 6, 14);
		final var toDate = LocalDate.of(2024, 6, 15);
		final var scheduledJobEntity = EntityMapper.mapToScheduledJobEntity(fromDate, toDate);

		assertThat(scheduledJobEntity.getFetchedStartDate()).isEqualTo(fromDate);
		assertThat(scheduledJobEntity.getFetchedEndDate()).isEqualTo(toDate);
	}

	@Test
	void testToScheduledBilling() {
		final var id = "test-id";
		final var externalId = "external-123";
		final var source = BillingSource.CONTRACT;
		final var billingDaysOfMonth = Set.of(1, 15);
		final var billingMonths = Set.of(3, 6, 9, 12);
		final var lastBilled = OffsetDateTime.of(2024, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC);
		final var nextScheduledBilling = LocalDate.of(2024, 9, 1);

		final var entity = ScheduledBillingEntity.builder()
			.withId(id)
			.withExternalId(externalId)
			.withSource(source)
			.withBillingDaysOfMonth(billingDaysOfMonth)
			.withBillingMonths(billingMonths)
			.withLastBilled(lastBilled)
			.withNextScheduledBilling(nextScheduledBilling)
			.withPaused(true)
			.build();

		final var result = EntityMapper.toScheduledBilling(entity);

		assertThat(result.getId()).isEqualTo(id);
		assertThat(result.getExternalId()).isEqualTo(externalId);
		assertThat(result.getSource()).isEqualTo(source);
		assertThat(result.getBillingDaysOfMonth()).isEqualTo(billingDaysOfMonth);
		assertThat(result.getBillingMonths()).isEqualTo(billingMonths);
		assertThat(result.getLastBilled()).isEqualTo(lastBilled);
		assertThat(result.getNextScheduledBilling()).isEqualTo(nextScheduledBilling);
		assertThat(result.getPaused()).isTrue();
	}

	@Test
	void testToScheduledBillingEntity() {
		final var municipalityId = "2281";
		final var externalId = "external-456";
		final var source = BillingSource.OPENE;
		final var billingDaysOfMonth = Set.of(5, 20);
		final var billingMonths = Set.of(1, 7);
		final var nextScheduledBilling = LocalDate.of(2024, 7, 5);

		final var dto = ScheduledBilling.builder()
			.withExternalId(externalId)
			.withSource(source)
			.withBillingDaysOfMonth(billingDaysOfMonth)
			.withBillingMonths(billingMonths)
			.withPaused(true)
			.build();

		final var result = EntityMapper.toScheduledBillingEntity(municipalityId, dto, nextScheduledBilling);

		assertThat(result.getId()).isNull();
		assertThat(result.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(result.getExternalId()).isEqualTo(externalId);
		assertThat(result.getSource()).isEqualTo(source);
		assertThat(result.getBillingDaysOfMonth()).isEqualTo(billingDaysOfMonth);
		assertThat(result.getBillingMonths()).isEqualTo(billingMonths);
		assertThat(result.getNextScheduledBilling()).isEqualTo(nextScheduledBilling);
		assertThat(result.isPaused()).isTrue();
		assertThat(result.getLastBilled()).isNull();
	}

	@Test
	void testToScheduledBillingEntity_withNullPaused() {
		final var municipalityId = "2281";
		final var nextScheduledBilling = LocalDate.of(2024, 7, 5);

		final var dto = ScheduledBilling.builder()
			.withExternalId("external-789")
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1))
			.withBillingMonths(Set.of(1))
			.withPaused(null)
			.build();

		final var result = EntityMapper.toScheduledBillingEntity(municipalityId, dto, nextScheduledBilling);

		assertThat(result.isPaused()).isFalse();
	}
}
