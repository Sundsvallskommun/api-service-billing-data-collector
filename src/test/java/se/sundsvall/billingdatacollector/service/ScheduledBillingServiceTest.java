package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
import java.util.List;
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
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.InvoicedIn;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.integration.db.ScheduledBillingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.dept44.problem.ThrowableProblem;

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
	private static final LocalDate INITIAL_NEXT = LocalDate.of(2027, 12, 1);

	@Mock
	private ScheduledBillingRepository mockRepository;

	@InjectMocks
	private ScheduledBillingService service;

	@Test
	void testCreate_success() {
		var scheduledBilling = createScheduledBilling();
		var entity = createScheduledBillingEntity();

		when(mockRepository.existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(false);
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class)))
			.thenReturn(entity);

		var result = service.create(MUNICIPALITY_ID, scheduledBilling);

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
		var scheduledBilling = createScheduledBilling();

		when(mockRepository.existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(true);

		assertThatThrownBy(() -> service.create(MUNICIPALITY_ID, scheduledBilling))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Duplicate scheduled billing");

		verify(mockRepository).existsByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verify(mockRepository, never()).saveAndFlush(any());
		verifyNoMoreInteractions(mockRepository);
	}

	/**
	 * Admin-update changes the cadence and the {@code paused} flag but must
	 * <strong>preserve</strong> the existing {@code nextScheduledBilling} —
	 * resetting it on every PUT would silently break billing progression
	 * when an operator merely tweaks a setting.
	 */
	@Test
	void testUpdate_preservesNextScheduledBilling() {
		var existingNext = LocalDate.of(2026, 9, 1);
		var existing = createScheduledBillingEntity();
		existing.setNextScheduledBilling(existingNext);

		var dto = ScheduledBilling.builder()
			.withExternalId("ignored")
			.withSource(BillingSource.OPENE)
			.withBillingDaysOfMonth(Set.of(5, 20))
			.withBillingMonths(Set.of(3, 6, 9, 12))
			.withPaused(true)
			.build();

		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.of(existing));
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.update(MUNICIPALITY_ID, ID, dto);

		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		var saved = captor.getValue();

		assertThat(saved.getBillingDaysOfMonth()).isEqualTo(Set.of(5, 20));
		assertThat(saved.getBillingMonths()).isEqualTo(Set.of(3, 6, 9, 12));
		assertThat(saved.isPaused()).isTrue();
		assertThat(saved.getNextScheduledBilling()).isEqualTo(existingNext);

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testUpdate_notFound_throwsException() {
		var scheduledBilling = createScheduledBilling();

		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(MUNICIPALITY_ID, ID, scheduledBilling))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verify(mockRepository, never()).saveAndFlush(any());
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetAll_success() {
		var pageable = PageRequest.of(0, 10);
		var page = new PageImpl<>(List.of(createScheduledBillingEntity()), pageable, 1);

		when(mockRepository.findAllByMunicipalityId(MUNICIPALITY_ID, pageable)).thenReturn(page);

		var result = service.getAll(MUNICIPALITY_ID, pageable);

		assertThat(result).isNotNull();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().getId()).isEqualTo(ID);

		verify(mockRepository).findAllByMunicipalityId(MUNICIPALITY_ID, pageable);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetById_success() {
		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID))
			.thenReturn(Optional.of(createScheduledBillingEntity()));

		var result = service.getById(MUNICIPALITY_ID, ID);

		assertThat(result.getId()).isEqualTo(ID);
		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetById_notFound_throwsException() {
		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(MUNICIPALITY_ID, ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testDelete_success() {
		var entity = createScheduledBillingEntity();
		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.of(entity));

		service.delete(MUNICIPALITY_ID, ID);

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verify(mockRepository).delete(entity);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testDelete_notFound_throwsException() {
		when(mockRepository.findByMunicipalityIdAndId(MUNICIPALITY_ID, ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(MUNICIPALITY_ID, ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndId(MUNICIPALITY_ID, ID);
		verify(mockRepository, never()).delete(any());
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetByExternalId_success() {
		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(createScheduledBillingEntity()));

		var result = service.getByExternalId(MUNICIPALITY_ID, BillingSource.CONTRACT, EXTERNAL_ID);

		assertThat(result.getId()).isEqualTo(ID);
		verify(mockRepository).findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetByExternalId_notFound_throwsException() {
		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getByExternalId(MUNICIPALITY_ID, BillingSource.CONTRACT, EXTERNAL_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Scheduled billing not found");

		verify(mockRepository).findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testGetDueScheduledBillings_success() {
		var entity = createScheduledBillingEntity();
		var today = LocalDate.now();

		when(mockRepository.findAllByPausedFalseAndNextScheduledBillingLessThanEqual(today))
			.thenReturn(List.of(entity));

		var result = service.getDueScheduledBillings();

		assertThat(result).hasSize(1).containsExactly(entity);
		verify(mockRepository).findAllByPausedFalseAndNextScheduledBillingLessThanEqual(today);
		verifyNoMoreInteractions(mockRepository);
	}

	// ========== upsert ==========

	@Test
	void upsert_whenNew_callsSupplierAndCreates() {
		var billingMonths = Set.of(3, 6, 9, 12);
		var billingDaysOfMonth = Set.of(1);

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.upsert(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT,
			billingMonths, billingDaysOfMonth, InvoicedIn.ADVANCE, () -> INITIAL_NEXT);

		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		var saved = captor.getValue();

		assertThat(saved.getMunicipalityId()).isEqualTo(MUNICIPALITY_ID);
		assertThat(saved.getExternalId()).isEqualTo(EXTERNAL_ID);
		assertThat(saved.getBillingMonths()).isEqualTo(billingMonths);
		assertThat(saved.getBillingDaysOfMonth()).isEqualTo(billingDaysOfMonth);
		assertThat(saved.getInvoicedIn()).isEqualTo(InvoicedIn.ADVANCE);
		assertThat(saved.getNextScheduledBilling()).isEqualTo(INITIAL_NEXT);
	}

	@Test
	void upsert_whenExistingAndCadenceUnchanged_doesNotCallSupplier() {
		var existing = createScheduledBillingEntity();
		existing.setBillingMonths(Set.of(3, 6, 9, 12));
		existing.setInvoicedIn(InvoicedIn.ADVANCE);
		existing.setNextScheduledBilling(LocalDate.of(2026, 9, 1));

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(existing));
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.upsert(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT,
			Set.of(3, 6, 9, 12), Set.of(1), InvoicedIn.ADVANCE,
			() -> { throw new AssertionError("Supplier must not be invoked when cadence/direction unchanged"); });

		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getNextScheduledBilling()).isEqualTo(LocalDate.of(2026, 9, 1));
	}

	/**
	 * Cadence change (e.g. QUARTERLY → YEARLY) parks the existing entity on
	 * a slot that no longer belongs to the new cadence — recompute via the
	 * supplier.
	 */
	@Test
	void upsert_whenCadenceChanged_recomputesNextScheduledBilling() {
		var existing = createScheduledBillingEntity();
		existing.setBillingMonths(Set.of(3, 6, 9, 12));
		existing.setInvoicedIn(InvoicedIn.ADVANCE);
		existing.setNextScheduledBilling(LocalDate.of(2026, 9, 1));

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(existing));
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.upsert(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT,
			Set.of(12), Set.of(1), InvoicedIn.ADVANCE, () -> INITIAL_NEXT);

		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getBillingMonths()).isEqualTo(Set.of(12));
		assertThat(captor.getValue().getNextScheduledBilling()).isEqualTo(INITIAL_NEXT);
	}

	/**
	 * Switching ADVANCE↔ARREARS shifts which period the same slot covers,
	 * so {@code nextScheduledBilling} must be recomputed too.
	 */
	@Test
	void upsert_whenInvoicedInChanged_recomputesNextScheduledBilling() {
		var existing = createScheduledBillingEntity();
		existing.setBillingMonths(Set.of(3, 6, 9, 12));
		existing.setInvoicedIn(InvoicedIn.ADVANCE);
		existing.setNextScheduledBilling(LocalDate.of(2026, 9, 1));

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(existing));
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.upsert(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT,
			Set.of(3, 6, 9, 12), Set.of(1), InvoicedIn.ARREARS, () -> INITIAL_NEXT);

		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getInvoicedIn()).isEqualTo(InvoicedIn.ARREARS);
		assertThat(captor.getValue().getNextScheduledBilling()).isEqualTo(INITIAL_NEXT);
	}

	/**
	 * Pre-V1_5 rows have a {@code null invoicedIn}; we treat that as
	 * "unknown" and do not interpret a non-null incoming direction as a
	 * change (otherwise the very first event after the migration would
	 * always reset progression).
	 */
	@Test
	void upsert_whenExistingInvoicedInIsNull_doesNotRecomputeOnFirstSet() {
		var existing = createScheduledBillingEntity();
		existing.setBillingMonths(Set.of(3, 6, 9, 12));
		existing.setInvoicedIn(null);
		existing.setNextScheduledBilling(LocalDate.of(2026, 9, 1));

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(existing));
		when(mockRepository.saveAndFlush(any(ScheduledBillingEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		service.upsert(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT,
			Set.of(3, 6, 9, 12), Set.of(1), InvoicedIn.ADVANCE,
			() -> { throw new AssertionError("Supplier must not be invoked when previous invoicedIn is null"); });

		var captor = ArgumentCaptor.forClass(ScheduledBillingEntity.class);
		verify(mockRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getInvoicedIn()).isEqualTo(InvoicedIn.ADVANCE);
		assertThat(captor.getValue().getNextScheduledBilling()).isEqualTo(LocalDate.of(2026, 9, 1));
	}

	// ========== getNextScheduledBilling ==========

	@Test
	void testGetNextScheduledBilling_whenExisting_shouldReturnDate() {
		var entity = createScheduledBillingEntity();

		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(entity));

		var result = service.getNextScheduledBilling(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);

		assertThat(result).isPresent().contains(entity.getNextScheduledBilling());
	}

	@Test
	void testGetNextScheduledBilling_whenNotExisting_shouldReturnEmpty() {
		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());

		var result = service.getNextScheduledBilling(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);

		assertThat(result).isEmpty();
	}

	// ========== delete helpers ==========

	@Test
	void testDeleteScheduledBillingEntity_shouldDelete() {
		var entity = createScheduledBillingEntity();
		service.deleteScheduledBillingEntity(entity);
		verify(mockRepository).delete(entity);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testSaveScheduledBillingEntity_shouldFlush() {
		var entity = createScheduledBillingEntity();
		service.saveScheduledBillingEntity(entity);
		verify(mockRepository).saveAndFlush(entity);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void testDeleteByExternalId_whenExisting_shouldDelete() {
		var entity = createScheduledBillingEntity();
		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.of(entity));

		service.deleteByExternalId(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);

		verify(mockRepository).delete(entity);
	}

	@Test
	void testDeleteByExternalId_whenNotExisting_shouldDoNothing() {
		when(mockRepository.findByMunicipalityIdAndExternalIdAndSource(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT))
			.thenReturn(Optional.empty());

		service.deleteByExternalId(MUNICIPALITY_ID, EXTERNAL_ID, BillingSource.CONTRACT);

		verify(mockRepository, never()).delete(any());
	}

	// ========== fixtures ==========

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
