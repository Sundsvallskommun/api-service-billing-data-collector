package se.sundsvall.billingdatacollector.integration.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class ScheduledBillingRepositoryTest {

	@Autowired
	private ScheduledBillingRepository repository;

	@Test
	void findReadyForBilling() {
		final var today = LocalDate.now();

		final var result = repository.findAllByPausedFalseAndNextScheduledBillingLessThanEqual(today);

		assertThat(result)
			.isNotNull()
			.isNotEmpty()
			.allMatch(entity -> !entity.isPaused())
			.allMatch(entity -> entity.getNextScheduledBilling().isBefore(today));
	}

	@Test
	void existsByMunicipalityIdAndExternalIdAndSource() {
		final var externalId = "66c57446-72e7-4cc5-af7c-053919ce904b";
		final var municipalityId = "2281";
		final var source = BillingSource.CONTRACT;

		final var exists = repository.existsByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source);

		assertThat(exists).isTrue();
	}

	@Test
	void existsByMunicipalityIdAndExternalIdAndSource_notFound() {
		final var exists = repository.existsByMunicipalityIdAndExternalIdAndSource("0000", "non-existing", BillingSource.CONTRACT);

		assertThat(exists).isFalse();
	}

	@Test
	void saveAndReadEntity() {
		final var externalId = UUID.randomUUID().toString();
		final var municipalityId = "1234";
		final var lastBilled = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
		final var nextScheduled = LocalDate.now().plusDays(5);
		final var daysOfMonth = Set.of(1, 15, 31);
		final var months = Set.of(3, 6, 9, 12);

		final var entity = ScheduledBillingEntity.builder()
			.withMunicipalityId(municipalityId)
			.withExternalId(externalId)
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(daysOfMonth)
			.withBillingMonths(months)
			.withLastBilled(lastBilled)
			.withNextScheduledBilling(nextScheduled)
			.withPaused(true)
			.build();

		final var saved = repository.saveAndFlush(entity);

		final var savedEntity = repository.getReferenceById(saved.getId());

		assertThat(savedEntity.getId()).isEqualTo(saved.getId());
		assertThat(savedEntity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(savedEntity.getExternalId()).isEqualTo(externalId);
		assertThat(savedEntity.getSource()).isEqualTo(BillingSource.CONTRACT);
		assertThat(savedEntity.isPaused()).isTrue();
		assertThat(savedEntity.getBillingDaysOfMonth())
			.containsExactlyInAnyOrderElementsOf(daysOfMonth);
		assertThat(savedEntity.getBillingMonths())
			.containsExactlyInAnyOrderElementsOf(months);
		assertThat(savedEntity.getNextScheduledBilling()).isEqualTo(nextScheduled);
		assertThat(savedEntity.getLastBilled()).isEqualTo(lastBilled);
	}

	@Test
	void getScheduledBillingById() {
		final var id = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";

		final var result = repository.findById(id);

		assertThat(result).isPresent();
		final var entity = result.get();

		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getMunicipalityId()).isEqualTo("2281");
		assertThat(entity.getExternalId()).isEqualTo("66c57446-72e7-4cc5-af7c-053919ce904b");
		assertThat(entity.getSource()).isEqualTo(BillingSource.CONTRACT);
		assertThat(entity.getBillingDaysOfMonth()).containsExactlyInAnyOrder(1, 15);
		assertThat(entity.getBillingMonths()).containsExactlyInAnyOrder(1, 4, 7, 10);
		assertThat(entity.getNextScheduledBilling()).isEqualTo(LocalDate.of(2020, 1, 1));
		assertThat(entity.isPaused()).isFalse();
		assertThat(entity.getLastBilled()).isNull();
	}

	@Test
	void findByMunicipalityIdAndExternalId() {
		final var municipalityId = "2281";
		final var externalId = "66c57446-72e7-4cc5-af7c-053919ce904b";
		final var source = BillingSource.CONTRACT;

		final var result = repository.findByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source);

		assertThat(result).isPresent();
		final var entity = result.get();

		assertThat(entity.getMunicipalityId()).isEqualTo(municipalityId);
		assertThat(entity.getExternalId()).isEqualTo(externalId);
		assertThat(entity.getSource()).isEqualTo(BillingSource.CONTRACT);
		assertThat(entity.getBillingDaysOfMonth()).containsExactlyInAnyOrder(1, 15);
		assertThat(entity.getBillingMonths()).containsExactlyInAnyOrder(1, 4, 7, 10);
		assertThat(entity.getNextScheduledBilling()).isEqualTo(LocalDate.of(2020, 1, 1));
		assertThat(entity.isPaused()).isFalse();
		assertThat(entity.getLastBilled()).isNull();
	}

	@Test
	void findByMunicipalityIdAndExternalId_notFound() {
		final var result = repository.findByMunicipalityIdAndExternalIdAndSource("non-existing", "non-existing", BillingSource.CONTRACT);

		assertThat(result).isEmpty();
	}

	@Test
	void findAllByMunicipalityId() {
		final var municipalityId = "2281";

		final var result = repository.findAllByMunicipalityId(municipalityId, Pageable.unpaged());

		assertThat(result).isNotNull();
		assertThat(result.getContent())
			.hasSize(3)
			.allMatch(entity -> entity.getMunicipalityId().equals(municipalityId));
	}

	@Test
	void findAllByMunicipalityId_notFound() {
		final var result = repository.findAllByMunicipalityId("0000", Pageable.unpaged());

		assertThat(result).isNotNull();
		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}

	@Test
	void findAllByMunicipalityId_withPagination() {
		final var municipalityId = "2281";
		final var pageRequest = PageRequest.of(0, 2, Sort.by("externalId").ascending());

		final var firstPage = repository.findAllByMunicipalityId(municipalityId, pageRequest);

		assertThat(firstPage).isNotNull();
		assertThat(firstPage.getContent()).hasSize(2);
		assertThat(firstPage.getTotalElements()).isEqualTo(3);
		assertThat(firstPage.getTotalPages()).isEqualTo(2);
		assertThat(firstPage.isFirst()).isTrue();
		assertThat(firstPage.isLast()).isFalse();

		final var secondPage = repository.findAllByMunicipalityId(municipalityId, pageRequest.next());

		assertThat(secondPage).isNotNull();
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.isFirst()).isFalse();
		assertThat(secondPage.isLast()).isTrue();
	}
}
