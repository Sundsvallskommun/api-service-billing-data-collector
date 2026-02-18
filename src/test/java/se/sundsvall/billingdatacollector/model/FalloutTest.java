package se.sundsvall.billingdatacollector.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FalloutTest {

	@Test
	void testCreationAndGetters() {
		// Arrange
		final var familyId = "familyId";
		final var flowInstanceId = "flowInstanceId";
		final var municipalityId = "municipalityId";
		final var requestId = "requestId";

		// Act
		final var fallout = new Fallout(familyId, flowInstanceId, municipalityId, requestId);

		// Assert
		assertThat(fallout.familyId()).isEqualTo(familyId);
		assertThat(fallout.flowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(fallout.municipalityId()).isEqualTo(municipalityId);
		assertThat(fallout.requestId()).isEqualTo(requestId);
	}
}
