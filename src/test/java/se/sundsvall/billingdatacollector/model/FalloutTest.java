package se.sundsvall.billingdatacollector.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FalloutTest {

	@Test
	void testCreationAndGetters() {
		// Arrange
		var familyId = "familyId";
		var flowInstanceId = "flowInstanceId";
		var requestId = "requestId";

		// Act
		var fallout = new Fallout(familyId, flowInstanceId, requestId);

		// Assert
		assertThat(fallout.familyId()).isEqualTo(familyId);
		assertThat(fallout.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(fallout.requestId()).isEqualTo(requestId);
	}
}
