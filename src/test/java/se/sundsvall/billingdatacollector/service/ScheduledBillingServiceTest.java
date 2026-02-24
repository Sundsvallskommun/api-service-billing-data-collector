package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.integration.db.ScheduledBillingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledBillingServiceTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String ID = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
	private static final String EXTERNAL_ID = "66c57446-72e7-4cc5-af7c-053919ce904b";

	@Mock
	private ScheduledBillingRepository mockRepository;

	@InjectMocks
	private ScheduledBillingService service;

	@Test
	void testCreate_success() {
		// Arrange
		var scheduledBilling = createScheduledBilling();
		var entity = createScheduledBillingEntity();

		when(mockRepository.existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(false);
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class)))
			.thenReturn(entity);

		// Act
		var result = service.create(MUNICIPALITY_ID, scheduledBilling);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(result.getSource()).isEqualTo(BillingSource.CONTRACT);

		verify(mockRepository).existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verify(mockRepository).saveAndFlush(any(ScheduledBillingEntity.class));
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testCreate_duplicate_throwsException() {
		// Arrange
		var scheduledBilling = createScheduledBilling();

		when(mockRepository.existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(true);

		// Act & Assert
		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, scheduledBilling))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Duplicate scheduled billing");

		verify(mockRepository).existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verify(mockRepository, never()).saveAndFlush(any());
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testUpdate_success() {
		// Arrange - create update request with different values, including externalId/source that should be ignored
		var updatedDaysOfMonth = Set.of(5, 20);
		var updatedMonths = Set.of(3, 6, 9, 12);
		var scheduledBilling = ScheduledBilling.builder()
			.withExternalId("different-external-id") // Should be ignored
			.withSource(BillingSource.OPENE) // Should be ignored
			.withBillingDaysOfMonth(updatedDaysOfMonth)
			.withBillingMonths(updatedMonths)
			.withPaused(true)
			.build();

		var existingEntity = createScheduledBillingEntity();
		var originalExternalId = existingEntity.getExternalId();
		var originalSource = existingEntity.getSource();

		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.of(existingEntity));
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		var result = service.update(MUNICIPALITY_ID, ID, scheduledBilling);

		// Assert - verify updated fields
		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		var savedEntity = captor.getValue();

		assertThat(savedEntity.getBillingDaysOfMonth()).isEqualTo(updatedDaysOfMonth);
		assertThat(savedEntity.getBillingMonths()).isEqualTo(updatedMonths);
		assertThat(savedEntity.isPaused()).isTrue();
		assertThat(savedEntity.getNextScheduledBilling()).isNotNull();

		// Assert - verify externalId and source are NOT updated
		assertThat(savedEntity.getExternalId()).isEqualTo(originalExternalId);
		assertThat(savedEntity.getSource()).isEqualTo(originalSource);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ID);

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testUpdate_notFound_throwsException() {
		// Arrange
		var scheduledBilling = createScheduledBilling();

		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, ID, scheduledBilling))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verify(mockRepository, never()).saveAndFlush(any());
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetAll_success() {
		// Arrange
		var pageable = PageRequest.of(0, 10);
		var entities = java.util.List.of(createScheduledBillingEntity());
		var page = new PageImpl<>(entities, pageable, 1);

		when(mockRepository.findAllByMunicipalityId(MUNICIPALITY_ID, pageable)).thenReturn(page);

		// Act
		var result = service.getAll(MUNICIPALITY_ID, pageable);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().getId()).isEqualTo(ID);

		verify(mockRepository).findAllByMunicipalityId(MUNICIPALITY_ID, pageable);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetById_success() {
		// Arrange
		var entity = createScheduledBillingEntity();

		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.of(entity));

		// Act
		var result = service.getById(MUNICIPALITY_ID, ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getExternalId()).isEqualTo(EXTERNAL_ID);

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetById_notFound_throwsException() {
		// Arrange
		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.getById(MUNICIPALITY_ID, ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testDelete_success() {
		// Arrange
		var entity = createScheduledBillingEntity();

		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.of(entity));

		// Act
		service.delete(MUNICIPALITY_ID, ID);

		// Assert
		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verify(mockRepository).delete(entity);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testDelete_notFound_throwsException() {
		// Arrange
		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verify(mockRepository, never()).delete(any());
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetByExternalId_success() {
		// Arrange
		var entity = createScheduledBillingEntity();

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(entity));

		// Act
		var result = service.getByExternalId(MUNICIPALITY_ID, BillingSource.CONTRACT, EXTERNAL_ID);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(ID);
		assertThat(result.getExternalId()).isEqualTo(EXTERNAL_ID);

		verify(mockRepository).findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetByExternalId_notFound_throwsException() {
		// Arrange
		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.getByExternalId(MUNICIPALITY_ID, BillingSource.CONTRACT, EXTERNAL_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetDueScheduledBillings_success() {
		// Arrange
		var scheduledBilling = createScheduledBillingEntity();
		var entities = List.of(scheduledBilling);
		// TODO: Change back to LocalDate.now() when temporary date in service is removed
		var localDate = LocalDate.of(2026, 3, 1);

		when(mockRepository.findAllByPausedFalseAndNextScheduledBillingLessThanEqual(localDate))
			.thenReturn(entities);

		// Act
		var result = service.getDueScheduledBillings();

		// Assert
		assertThat(result).isNotNull().hasSize(1);
		assertThat(result.getFirst()).isSameAs(scheduledBilling);

		verify(mockRepository).findAllByPausedFalseAndNextScheduledBillingLessThanEqual(localDate);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testUpdateNextScheduledBilling_success() {
		// Arrange - entity with billing every day of every month
		var entity = createScheduledBillingEntity();
		entity.setNextScheduledBilling(LocalDate.now());
		entity.setBillingDaysOfMonth(IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toSet()));
		entity.setBillingMonths(IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toSet()));
		var originalNextBilling = entity.getNextScheduledBilling();

		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		service.updateNextScheduledBilling(entity);

		// Assert - next billing should be tomorrow (not today) to prevent multiple runs on same day
		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		var savedEntity = captor.getValue();

		assertThat(savedEntity.getNextScheduledBilling())
			.isNotNull()
			.isNotEqualTo(originalNextBilling)
			.isEqualTo(LocalDate.now().plusDays(1));

		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testUpdateNextScheduledBillingInPast_success() {
		// Arrange - entity with billing every day of every month
		var entity = createScheduledBillingEntity();
		entity.setNextScheduledBilling(LocalDate.of(2025, 9, 15)); // A date in the past
		entity.setBillingDaysOfMonth(Set.of(15));
		entity.setBillingMonths(Set.of(3, 6, 9, 12));
		var originalNextBilling = entity.getNextScheduledBilling();

		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// Act
		service.updateNextScheduledBilling(entity);

		// Assert - next billing should be tomorrow (not today) to prevent multiple runs on same day
		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		var savedEntity = captor.getValue();

		assertThat(savedEntity.getNextScheduledBilling())
			.isNotNull()
			.isNotEqualTo(originalNextBilling)
			.isEqualTo(LocalDate.of(2025, 12, 15));

		verifyNoMoreInteractions(mockRepository);
	}

	private ScheduledBilling createScheduledBilling() {
		return ScheduledBilling.builder()
			.withExternalId(EXTERNAL_ID)
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1, 15))
			.withBillingMonths(Set.of(1, 4, 7, 10))
			.withPaused(false)
			.build();
	}

	private ScheduledBillingEntity createScheduledBillingEntity() {
		return ScheduledBillingEntity.builder()
			.withId(ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.withExternalId(EXTERNAL_ID)
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1, 15))
			.withBillingMonths(Set.of(1, 4, 7, 10))
			.withNextScheduledBilling(LocalDate.now().plusDays(10))
			.withPaused(false)
			.build();
	}
}
