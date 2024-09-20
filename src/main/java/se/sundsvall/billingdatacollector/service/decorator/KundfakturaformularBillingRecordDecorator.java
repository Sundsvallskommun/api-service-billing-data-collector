package se.sundsvall.billingdatacollector.service.decorator;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import generated.se.sundsvall.billingpreprocessor.Type;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

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
			final var partyId = partyIntegration.getPartyId(wrapper.getMunicipalityId(), wrapper.getLegalId()).orElseThrow(() -> Problem.builder()
				.withTitle("Couldn't find partyId for legalId " + wrapper.getLegalId())
				.withStatus(INTERNAL_SERVER_ERROR)
				.build());

			wrapper.getBillingRecord().getRecipient().setPartyId(partyId);
		}
	}
}
