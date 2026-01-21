package se.sundsvall.billingdatacollector.service.source.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

class ContractBillingHandlerTest {

	private final ContractBillingHandler handler = new ContractBillingHandler();

	@Test
	void sendBillingRecords_shouldThrowNotImplementedException() {
		// Act & Assert
		assertThatThrownBy(() -> handler.sendBillingRecords("2281", "test-external-id"))
			.isInstanceOf(NotImplementedException.class);
	}
}
