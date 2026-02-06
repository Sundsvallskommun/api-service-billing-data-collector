package se.sundsvall.billingdatacollector.service.source.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.billingdatacollector.integration.db.CounterpartMappingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.CounterpartMappingEntity;

@ExtendWith(MockitoExtension.class)
class CounterpartMappingServiceTest {

	private static final String LEGAL_ID = "112233445566";
	private static final String STAKEHOLDER_TYPE = "ORGANIZATION";
	private static final String COUNTERPART = "COUNTERPART_A";

	@Mock
	private CounterpartMappingRepository mockRepository;

	@InjectMocks
	private CounterpartMappingService service;

	@Test
	void findCounterpartByLegalId_exactMatch() {
		var entity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalId(LEGAL_ID)
			.withCounterpart(COUNTERPART)
			.build();

		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.of(entity));

		var result = service.findCounterpartByLegalId(LEGAL_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockRepository).findByLegalId(LEGAL_ID);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void findCounterpartByLegalId_patternMatch() {
		var patternEntity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalIdPattern("^1122.*")
			.withCounterpart(COUNTERPART)
			.build();

		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.empty());
		when(mockRepository.findAll()).thenReturn(List.of(patternEntity));

		var result = service.findCounterpartByLegalId(LEGAL_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockRepository).findByLegalId(LEGAL_ID);
		verify(mockRepository).findAll();
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void findCounterpartByLegalId_stakeholderTypeFallback() {
		var typeEntity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withStakeholderType(STAKEHOLDER_TYPE)
			.withCounterpart(COUNTERPART)
			.build();

		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.empty());
		when(mockRepository.findAll()).thenReturn(List.of());
		when(mockRepository.findByStakeholderType(STAKEHOLDER_TYPE)).thenReturn(Optional.of(typeEntity));

		var result = service.findCounterpartByLegalId(LEGAL_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockRepository).findByLegalId(LEGAL_ID);
		verify(mockRepository).findAll();
		verify(mockRepository).findByStakeholderType(STAKEHOLDER_TYPE);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void findCounterpartByLegalId_noMatch_throwsNotFound() {
		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.empty());
		when(mockRepository.findAll()).thenReturn(List.of());
		when(mockRepository.findByStakeholderType(STAKEHOLDER_TYPE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findCounterpartByLegalId(LEGAL_ID, STAKEHOLDER_TYPE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", Status.NOT_FOUND)
			.hasMessage("No counterpart found for legalId: " + LEGAL_ID + " or stakeholderType: " + STAKEHOLDER_TYPE);

		verify(mockRepository).findByLegalId(LEGAL_ID);
		verify(mockRepository).findAll();
		verify(mockRepository).findByStakeholderType(STAKEHOLDER_TYPE);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void findCounterpartByLegalId_noMatchWithNullStakeholderType_throwsNotFound() {
		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.empty());
		when(mockRepository.findAll()).thenReturn(List.of());

		assertThatThrownBy(() -> service.findCounterpartByLegalId(LEGAL_ID, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", Status.NOT_FOUND);

		verify(mockRepository).findByLegalId(LEGAL_ID);
		verify(mockRepository).findAll();
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void findCounterpartByLegalId_invalidRegexPattern_skipped() {
		var invalidPatternEntity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalIdPattern("[invalid(regex")
			.withCounterpart("COUNTERPART_INVALID")
			.build();
		var validPatternEntity = CounterpartMappingEntity.builder()
			.withId("id-2")
			.withLegalIdPattern("^1122.*")
			.withCounterpart(COUNTERPART)
			.build();

		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.empty());
		when(mockRepository.findAll()).thenReturn(List.of(invalidPatternEntity, validPatternEntity));

		var result = service.findCounterpartByLegalId(LEGAL_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
	}

	@Test
	void findCounterpartByLegalId_patternDoesNotMatch_fallsBackToStakeholderType() {
		var patternEntity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalIdPattern("^9999.*")
			.withCounterpart("COUNTERPART_PATTERN")
			.build();
		var typeEntity = CounterpartMappingEntity.builder()
			.withId("id-2")
			.withStakeholderType(STAKEHOLDER_TYPE)
			.withCounterpart(COUNTERPART)
			.build();

		when(mockRepository.findByLegalId(LEGAL_ID)).thenReturn(Optional.empty());
		when(mockRepository.findAll()).thenReturn(List.of(patternEntity));
		when(mockRepository.findByStakeholderType(STAKEHOLDER_TYPE)).thenReturn(Optional.of(typeEntity));

		var result = service.findCounterpartByLegalId(LEGAL_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
	}
}
