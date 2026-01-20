package se.sundsvall.billingdatacollector.service.source.contract;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;
import se.sundsvall.billingdatacollector.service.source.BillingSourceHandler;

@Component("contract")
public class ContractBillingHandler implements BillingSourceHandler {

	@Override
	public void sendBillingRecords(String municipalityId, String externalId) {
		// TODO: Fetch contract, map to billing record, send to billing pre processor
		throw new NotImplementedException();
	}
}
