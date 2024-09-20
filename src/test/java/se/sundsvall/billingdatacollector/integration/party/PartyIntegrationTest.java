package se.sundsvall.billingdatacollector.integration.party;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import generated.se.sundsvall.party.PartyType;

@ExtendWith(MockitoExtension.class)
class PartyIntegrationTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private PartyClient mockPartyClient;

	@InjectMocks
	private PartyIntegration partyIntegration;

	@Test
	void testGetPartyId() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.of("somePartyId"));

		final var partyId = partyIntegration.getPartyId(MUNICIPALITY_ID, "5505158888");

		assertThat(partyId).hasValue("somePartyId");

		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenNothingIsFound() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class)))
			.thenReturn(Optional.empty());

		final var partyId = partyIntegration.getPartyId(MUNICIPALITY_ID, "5505158888");

		assertThat(partyId).isEmpty();

		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}

	@Test
	void testGetPartyIdWhenExceptionIsThrown() {
		when(mockPartyClient.getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class)))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));

		final var partyId = partyIntegration.getPartyId(MUNICIPALITY_ID, "5505158888");

		assertThat(partyId).isEmpty();

		verify(mockPartyClient).getPartyId(eq(MUNICIPALITY_ID), eq(PartyType.PRIVATE), any(String.class));
		verifyNoMoreInteractions(mockPartyClient);
	}
}
