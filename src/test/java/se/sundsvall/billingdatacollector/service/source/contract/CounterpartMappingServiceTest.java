package se.sundsvall.billingdatacollector.service.source.contract;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.billingdatacollector.integration.db.CounterpartMappingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.CounterpartMappingEntity;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class CounterpartMappingServiceTest {

	private static final String LEGAL_ID = "11223344-5566";
	private static final String PARTY_ID = "partyId";
	private static final String STAKEHOLDER_TYPE = "ORGANIZATION";
	private static final String COUNTERPART = "COUNTERPART";
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Mock
	private CounterpartMappingRepository mockRepository;

	@Mock
	private PartyIntegration mockPartyIntegration;

	@InjectMocks
	private CounterpartMappingService service;

	@Test
	void findCounterpart_patternExactMatch() {
		var patternEntity1 = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalIdPattern("1122")
			.withCounterpart("NOT_MATCHING")
			.build();
		var patternEntity2 = CounterpartMappingEntity.builder()
			.withId("id-2")
			.withLegalIdPattern("2233")
			.withCounterpart("NOT_MATCHING")
			.build();
		var patternEntity3 = CounterpartMappingEntity.builder()
			.withId("id-3")
			.withLegalIdPattern("112233445566")
			.withCounterpart(COUNTERPART)
			.build();

		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenReturn(LEGAL_ID);
		when(mockRepository.findAll()).thenReturn(List.of(patternEntity1, patternEntity2, patternEntity3));

		var result = service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verify(mockRepository).findAll();
		verifyNoMoreInteractions(mockRepository, mockPartyIntegration);
	}

	@Test
	void findCounterpart_patternMatch() {
		var patternEntity1 = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalIdPattern("1122")
			.withCounterpart(COUNTERPART)
			.build();
		var patternEntity2 = CounterpartMappingEntity.builder()
			.withId("id-2")
			.withLegalIdPattern("2233")
			.withCounterpart("NOT_MATCHING")
			.build();
		var patternEntity3 = CounterpartMappingEntity.builder()
			.withId("id-3")
			.withLegalIdPattern("2233445566")
			.withCounterpart("NOT_MATCHING")
			.build();

		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenReturn(LEGAL_ID);
		when(mockRepository.findAll()).thenReturn(List.of(patternEntity1, patternEntity2, patternEntity3));

		var result = service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verify(mockRepository).findAll();
		verifyNoMoreInteractions(mockRepository, mockPartyIntegration);
	}

	@Test
	void findCounterpart_stakeholderTypeFallback() {
		var typeEntity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withStakeholderType(STAKEHOLDER_TYPE)
			.withCounterpart(COUNTERPART)
			.build();

		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenReturn(LEGAL_ID);
		when(mockRepository.findAll()).thenReturn(List.of());
		when(mockRepository.findByStakeholderType(STAKEHOLDER_TYPE)).thenReturn(Optional.of(typeEntity));

		var result = service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verify(mockRepository).findAll();
		verify(mockRepository).findByStakeholderType(STAKEHOLDER_TYPE);
		verifyNoMoreInteractions(mockRepository, mockPartyIntegration);
	}

	@Test
	void findCounterpart_noMatch_throwsNotFound() {
		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenReturn(LEGAL_ID);
		when(mockRepository.findAll()).thenReturn(List.of());
		when(mockRepository.findByStakeholderType(STAKEHOLDER_TYPE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND)
			.hasMessage("No counterpart found for partyId: " + PARTY_ID + " or stakeholderType: " + STAKEHOLDER_TYPE);

		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verify(mockRepository).findAll();
		verify(mockRepository).findByStakeholderType(STAKEHOLDER_TYPE);
		verifyNoMoreInteractions(mockRepository);
	}

	@Test
	void findCounterpart_noMatchWithNullStakeholderType_throwsNotFound() {
		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenReturn(LEGAL_ID);
		when(mockRepository.findAll()).thenReturn(List.of());

		assertThatThrownBy(() -> service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, null))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", NOT_FOUND);

		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verify(mockRepository).findAll();
		verifyNoMoreInteractions(mockRepository, mockPartyIntegration);
	}

	@Test
	void findCounterpart_patternDoesNotMatch_fallsBackToStakeholderType() {
		var patternEntity = CounterpartMappingEntity.builder()
			.withId("id-1")
			.withLegalIdPattern("9999")
			.withCounterpart("COUNTERPART_PATTERN")
			.build();
		var typeEntity = CounterpartMappingEntity.builder()
			.withId("id-2")
			.withStakeholderType(STAKEHOLDER_TYPE)
			.withCounterpart(COUNTERPART)
			.build();

		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenReturn(LEGAL_ID);
		when(mockRepository.findAll()).thenReturn(List.of(patternEntity));
		when(mockRepository.findByStakeholderType(STAKEHOLDER_TYPE)).thenReturn(Optional.of(typeEntity));

		var result = service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE);

		assertThat(result).isEqualTo(COUNTERPART);
		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verify(mockRepository).findAll();
		verify(mockRepository).findByStakeholderType(STAKEHOLDER_TYPE);
		verifyNoMoreInteractions(mockRepository, mockPartyIntegration);
	}

	@Test
	void findCounterpart_throwsExceptionWhenGetLegalId() {
		when(mockPartyIntegration.getLegalId(MUNICIPALITY_ID, PARTY_ID)).thenThrow(Problem.builder()
			.withTitle("Error fetching legalId")
			.withStatus(INTERNAL_SERVER_ERROR)
			.build());

		assertThatThrownBy(() -> service.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE))
			.isInstanceOf(ThrowableProblem.class)
			.hasFieldOrPropertyWithValue("status", INTERNAL_SERVER_ERROR);

		verify(mockPartyIntegration).getLegalId(MUNICIPALITY_ID, PARTY_ID);
		verifyNoMoreInteractions(mockRepository, mockPartyIntegration);
	}
}
