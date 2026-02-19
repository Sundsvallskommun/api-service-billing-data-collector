package se.sundsvall.billingdatacollector.service.decorator;

import generated.se.sundsvall.billingpreprocessor.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.party.PartyIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

@Component
public class KundfakturaformularBillingRecordDecorator implements BillingRecordDecorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(KundfakturaformularBillingRecordDecorator.class);

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

	/**
	 * If it's an external billing record, decorate it with partyId.
	 * If no partyId is found, the legalId will be set to BPP, provided it's not a private person.
	 * 
	 * @param wrapper The billing record wrapper to decorate.
	 */
	@Override
	public void decorate(BillingRecordWrapper wrapper) {
		// We only need to set partyId if it's EXTERNAL billing.
		if (Type.EXTERNAL.equals(wrapper.getBillingRecord().getType())) {
			try {
				var partyId = partyIntegration.getPartyId(wrapper.getMunicipalityId(), wrapper.getLegalId());
				ofNullable(wrapper.getBillingRecord().getRecipient())
					.ifPresent(recipient -> recipient.setPartyId(partyId));
			} catch (Exception e) {
				LOGGER.info("Couldn't get partyId, will send legalId: {} to bpp", wrapper.getLegalId());
				setLegalIdIfNotPrivatePerson(wrapper);
			}
		}
	}

	/**
	 * If the recipient is not a private person, set the legalId as the recipient's legalId.
	 * If the recipient is a private person, log a warning and throw a problem since we cannot proceed.
	 * 
	 * @param wrapper The billing record wrapper to set the legalId on.
	 */
	private void setLegalIdIfNotPrivatePerson(BillingRecordWrapper wrapper) {
		if (!wrapper.isRecipientPrivatePerson()) {
			ofNullable(wrapper.getBillingRecord().getRecipient())
				.ifPresent(recipient -> recipient.setLegalId(wrapper.getLegalId()));
		} else {
			LOGGER.warn("Couldn't get partyId for private person, will be sent to fallout");
			throw Problem.builder()
				.withTitle("Couldn't find partyId for legalId " + wrapper.getLegalId())
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}
}
