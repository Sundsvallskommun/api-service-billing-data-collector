package se.sundsvall.billingdatacollector.service.decorator;

import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.Type;

@Component
public class KundfakturaformularBillingRecordDecorator implements BillingRecordDecorator {

	private final PartyIntegration partyIntegration;
	private final OpenEIntegrationProperties properties;

	public KundfakturaformularBillingRecordDecorator(PartyIntegration partyIntegration, OpenEIntegrationProperties properties) {
		this.partyIntegration = partyIntegration;
		this.properties = properties;
	}

	@Override
	public String getSupportedFamilyId() {
		return properties.kundfakturaFormularFamilyId();
	}

	@Override
	public void decorate(BillingRecordWrapper wrapper) {
		// We only need to set partyId if it's EXTERNAL billing.
		if (Type.EXTERNAL.equals(wrapper.getBillingRecord().getType())) {
			final var partyId = partyIntegration.getPartyId(wrapper.getMunicipalityId(), wrapper.getLegalId());
			wrapper.getBillingRecord().getRecipient().setPartyId(partyId);
		}
	}
}
