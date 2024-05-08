package se.sundsvall.billingdatacollector.service.decorator;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.KUNDFAKTURA_FORMULAR_FAMILY_ID;

import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.Type;

@Component
public class KundfakturaformularBillingRecordDecorator implements BillingRecordDecorator {

	private final PartyIntegration partyIntegration;

	public KundfakturaformularBillingRecordDecorator(PartyIntegration partyIntegration) {
		this.partyIntegration = partyIntegration;
	}

	@Override
	public String getSupportedFamilyId() {
		return KUNDFAKTURA_FORMULAR_FAMILY_ID;
	}

	@Override
	public void decorate(BillingRecordWrapper wrapper) {
		//We only need to set partyId if it's EXTERNAL billing.
		if (wrapper.getBillingRecord().getType().equals(Type.EXTERNAL)) {
			var partyId = partyIntegration.getPartyId(wrapper.getLegalId()).orElseThrow(() -> Problem.builder()
				.withTitle("Couldn't find partyId for legalId " + wrapper.getLegalId())
				.withStatus(INTERNAL_SERVER_ERROR)
				.build());

			wrapper.getBillingRecord().getRecipient().setPartyId(partyId);
		}
	}
}
