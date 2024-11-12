package se.sundsvall.billingdatacollector.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.party.PartyType;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private PartyClient mockPartyClient;

	@InjectMocks
	private PartyIntegration partyIntegration;

	private static final String FAULTY_PERSONAL_NUMBER = "19900101238";
	private static final String PERSONAL_NUMBER = "199001012385";
	private static final String ORGANIZATION_NUMBER = "5505158888";

	@Test
	void testGetPartyIdForPrivateParty() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class)))
			.thenReturn(Optional.empty());
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.of("somePartyId"));

		final var partyId = partyIntegration.getPartyId(MUNICIPALITY_ID, PERSONAL_NUMBER);

		assertThat(partyId).isEqualTo("somePartyId");

		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class));
		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdForEnterpriseParty() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class)))
			.thenReturn(Optional.of("somePartyId"));

		final var partyId = partyIntegration.getPartyId(MUNICIPALITY_ID, ORGANIZATION_NUMBER);

		assertThat(partyId).isEqualTo("somePartyId");

		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenNothingIsFound() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.empty());
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class)))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> partyIntegration.getPartyId(MUNICIPALITY_ID, PERSONAL_NUMBER))
			.satisfies(problem -> {
				assertThat(problem.getTitle()).isEqualTo("Couldn't find partyId for legalId 199001012385");
				assertThat(problem.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			});

		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class));
		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenPersonalNumberIsInvalid() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.ENTERPRISE), any(String.class)))
			.thenReturn(Optional.empty());

		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> partyIntegration.getPartyId(MUNICIPALITY_ID, FAULTY_PERSONAL_NUMBER))
			.satisfies(problem -> {
				assertThat(problem.getTitle()).isEqualTo("Couldn't find partyId for legalId 19900101238");
				assertThat(problem.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
			});

		verifyNoMoreInteractions(mockPartyClient);

	}
}
