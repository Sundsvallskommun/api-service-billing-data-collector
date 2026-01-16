package se.sundsvall.billingdatacollector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
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
	void testUpdate_wrongMunicipality_throwsException() {
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
	void testCalculateNextScheduledBilling_currentMonthValidDay() {
		// Arrange
		var today = LocalDate.now();
		var daysOfMonth = Set.of(28);
		var months = Set.of(today.getMonthValue());

		// Calculate expected date
		LocalDate expected;
		if (today.getDayOfMonth() <= 28) {
			expected = LocalDate.of(today.getYear(), today.getMonthValue(), 28);
		} else {
			// Day 28 has passed, next occurrence is next year
			expected = LocalDate.of(today.getYear() + 1, today.getMonthValue(), 28);
		}

		// Act
		var result = service.calculateNextScheduledBilling(daysOfMonth, months);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testCalculateNextScheduledBilling_nextMonth() {
		// Arrange
		var today = LocalDate.now();
		var daysOfMonth = Set.of(1);
		var nextMonthDate = today.plusMonths(1);
		var months = Set.of(nextMonthDate.getMonthValue());

		var expected = LocalDate.of(nextMonthDate.getYear(), nextMonthDate.getMonthValue(), 1);

		// Act
		var result = service.calculateNextScheduledBilling(daysOfMonth, months);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testCalculateNextScheduledBilling_dayAdjustedForShortMonth() {
		// Arrange - February with day 31 should adjust to last day of February
		var today = LocalDate.now();
		var daysOfMonth = Set.of(31);
		var months = Set.of(2); // February

		// Calculate next February
		var nextFeb = today.getMonthValue() <= 2 && (today.getMonthValue() < 2 || today.getDayOfMonth() <= today.withMonth(2).lengthOfMonth())
			? LocalDate.of(today.getYear(), 2, 1)
			: LocalDate.of(today.getYear() + 1, 2, 1);
		var expected = LocalDate.of(nextFeb.getYear(), 2, nextFeb.lengthOfMonth());

		// Act
		var result = service.calculateNextScheduledBilling(daysOfMonth, months);

		// Assert
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testCalculateNextScheduledBilling_emptyDaysOfMonth_throwsException() {
		// Arrange
		var daysOfMonth = Set.<Integer>of();
		var months = Set.of(1, 2, 3);

		// Act & Assert
		assertThatThrownBy(() -> service.calculateNextScheduledBilling(daysOfMonth, months))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billingDaysOfMonth must not be empty");
	}

	@Test
	void testCalculateNextScheduledBilling_nullDaysOfMonth_throwsException() {
		// Arrange
		var months = Set.of(1, 2, 3);

		// Act & Assert
		assertThatThrownBy(() -> service.calculateNextScheduledBilling(null, months))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billingDaysOfMonth must not be empty");
	}

	@Test
	void testCalculateNextScheduledBilling_emptyMonths_throwsException() {
		// Arrange
		var daysOfMonth = Set.of(1, 15);
		var months = Set.<Integer>of();

		// Act & Assert
		assertThatThrownBy(() -> service.calculateNextScheduledBilling(daysOfMonth, months))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billingMonths must not be empty");
	}

	@Test
	void testCalculateNextScheduledBilling_nullMonths_throwsException() {
		// Arrange
		var daysOfMonth = Set.of(1, 15);

		// Act & Assert
		assertThatThrownBy(() -> service.calculateNextScheduledBilling(daysOfMonth, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billingMonths must not be empty");
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
