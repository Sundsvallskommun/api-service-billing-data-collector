package se.sundsvall.billingdatacollector.service.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.billingdatacollector.TestDataFactory;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;

@ExtendWith(MockitoExtension.class)
class KundfakturaformularBillingDecoratorTest {

	@Mock
	private PartyIntegration mockPartyIntegration;

	@Mock
	private OpenEIntegrationProperties mockProperties;

	@InjectMocks
	private KundfakturaformularBillingRecordDecorator kundfakturaformularDecorator;

	@Test
	void testDecorateExternalBillingRecord() {
		// Arrange
		final var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(false);
		final var uuid = UUID.randomUUID().toString();
		when(mockPartyIntegration.getPartyId(anyString(), anyString())).thenReturn(Optional.of(uuid));

		// Act
		kundfakturaformularDecorator.decorate(wrapper);

		// Assert
		assertThat(wrapper.getBillingRecord().getRecipient().getPartyId()).isEqualTo(uuid);
		verify(mockPartyIntegration).getPartyId(wrapper.getMunicipalityId(), wrapper.getLegalId());
		verifyNoMoreInteractions(mockPartyIntegration);
	}

	@Test
	void testDecorateInternalBillingRecord() {
		// Arrange
		final var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);

		// Act
		kundfakturaformularDecorator.decorate(wrapper);

		// Assert
		assertThat(wrapper.getBillingRecord().getRecipient().getPartyId()).isNull();
		verifyNoInteractions(mockPartyIntegration);
	}

	@Test
	void testDecorateCannotFindPartyId_shouldThrowException() {
		// Arrange
		final var wrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(false);
		when(mockPartyIntegration.getPartyId(anyString(), anyString())).thenReturn(Optional.empty());

		// Act & Assert
		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> kundfakturaformularDecorator.decorate(wrapper))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't find partyId for legalId " + wrapper.getLegalId());
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
			});

		verify(mockPartyIntegration).getPartyId(wrapper.getMunicipalityId(), wrapper.getLegalId());
		verifyNoMoreInteractions(mockPartyIntegration);
	}

	@Test
	void testGetSupportedFamilyId() {
		when(mockProperties.kundfakturaFormularFamilyId()).thenReturn("198");
		assertThat(kundfakturaformularDecorator.getSupportedFamilyId()).isEqualTo("198");
	}
}
