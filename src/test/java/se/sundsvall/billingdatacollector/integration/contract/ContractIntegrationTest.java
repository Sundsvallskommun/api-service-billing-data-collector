package se.sundsvall.billingdatacollector.integration.contract;

import generated.se.sundsvall.contract.Contract;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractIntegrationTest {

	@Mock
	private ContractClient clientMock;

	@Mock
	private Contract contractMock;

	@InjectMocks
	private ContractIntegration integration;

	@Test
	void getContractWhenMatch() {
		// Arrange
		final var municipalityId = "municipalityId";
		final var contractId = "contractId";

		when(clientMock.getContract(municipalityId, contractId)).thenReturn(Optional.of(contractMock));

		// Act
		final var result = integration.getContract(municipalityId, contractId);

		// Assert & verify
		assertThat(result).isPresent().containsSame(contractMock);
		verify(clientMock).getContract(municipalityId, contractId);
		verifyNoMoreInteractions(clientMock);
		verifyNoInteractions(contractMock);
	}

	@Test
	void getContractWhenNoMatch() {
		// Arrange
		final var municipalityId = "municipalityId";
		final var contractId = "contractId";

		// Act
		final var result = integration.getContract(municipalityId, contractId);

		// Assert & verify
		assertThat(result).isNotPresent();
		verify(clientMock).getContract(municipalityId, contractId);
		verifyNoMoreInteractions(clientMock);
		verifyNoInteractions(contractMock);
	}
}
