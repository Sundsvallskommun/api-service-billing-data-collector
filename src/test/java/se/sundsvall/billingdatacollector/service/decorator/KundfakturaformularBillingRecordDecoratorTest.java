package se.sundsvall.billingdatacollector.service.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Type;

@ExtendWith(MockitoExtension.class)
class KundfakturaformularBillingRecordDecoratorTest {

	@Mock
	private PartyIntegration mockPartyIntegration;

	@Mock
	private OpenEIntegrationProperties mockProperties;

	@InjectMocks
	private KundfakturaformularBillingRecordDecorator decorator;

	private static final String FAMILY_ID = "familyId";
	private static final String LEGAL_ID = "legalId";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "partyId";

	@Test
	void testDecorateExternalBillingRecord_shouldDecorateWithPartyId() {
		when(mockPartyIntegration.getPartyId(MUNICIPALITY_ID, LEGAL_ID)).thenReturn(PARTY_ID);
		var billingRecordWrapper = createBillingRecordWrapper(Type.EXTERNAL);

		decorator.decorate(billingRecordWrapper);

		assertThat(billingRecordWrapper.getBillingRecord().getRecipient().getPartyId()).isEqualTo(PARTY_ID);
		verify(mockPartyIntegration).getPartyId(MUNICIPALITY_ID, LEGAL_ID);
		verifyNoMoreInteractions(mockPartyIntegration);
	}

	@Test
	void testDecorateInternalBillingRecord_shouldNotDecorate() {
		var billingRecordWrapper = createBillingRecordWrapper(Type.INTERNAL);
		decorator.decorate(billingRecordWrapper);

		assertThat(billingRecordWrapper.getBillingRecord().getRecipient().getPartyId()).isNull();
		verify(mockPartyIntegration, never()).getPartyId(MUNICIPALITY_ID, LEGAL_ID);
		verifyNoMoreInteractions(mockPartyIntegration);
	}

	@Test
	void testGetSupportedFamilyId() {
		when(mockProperties.kundfakturaFormularFamilyId()).thenReturn(FAMILY_ID);

		var supportedFamilyId = decorator.getSupportedFamilyId();

		assertThat(supportedFamilyId).isEqualTo(FAMILY_ID);
		verify(mockProperties).kundfakturaFormularFamilyId();
		verifyNoMoreInteractions(mockProperties);
	}

	private BillingRecordWrapper createBillingRecordWrapper(Type type) {
		return BillingRecordWrapper.builder()
			.withBillingRecord(createBillingRecord(type))
			.withLegalId(LEGAL_ID)
			.withMunicipalityId(MUNICIPALITY_ID)
			.build();
	}

	private BillingRecord createBillingRecord(Type type) {
		var billingRecord = new BillingRecord();
		billingRecord.setType(type);
		billingRecord.setRecipient(new Recipient());

		return billingRecord;
	}
}
