package se.sundsvall.billingdatacollector.service.source.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.contract.Contract;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.contract.ContractIntegration;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;

@ExtendWith(MockitoExtension.class)
class ContractBillingHandlerTest {
	private static final String MUNICIPALITY_ID = "municipalityId";
	private static final String CONTRACT_ID = "contractId";

	@Mock
	private ContractIntegration contractIntegrationMock;

	@Mock
	private ContractMapper contractMapperMock;

	@Mock
	private BillingPreprocessorClient billingPreprocessorClientMock;

	@Mock
	private HistoryRepository historyRepositoryMock;

	@Mock
	private Contract contractMock;

	@Mock
	private BillingRecord billingRecordMock;

	@InjectMocks
	private ContractBillingHandler handler;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(
			contractIntegrationMock,
			contractMapperMock,
			billingPreprocessorClientMock,
			historyRepositoryMock);
	}

	@Test
	void sendBillingRecords_contractMatch() {
		// Arrange
		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contractMock));
		when(contractMapperMock.createBillingRecord(MUNICIPALITY_ID, contractMock)).thenReturn(billingRecordMock);
		when(billingPreprocessorClientMock.createBillingRecord(MUNICIPALITY_ID, billingRecordMock))
			.thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

		// Act
		handler.sendBillingRecords(MUNICIPALITY_ID, CONTRACT_ID);

		// Assert & verify
		verify(contractIntegrationMock).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(contractMapperMock).createBillingRecord(MUNICIPALITY_ID, contractMock);
		verify(billingPreprocessorClientMock).createBillingRecord(MUNICIPALITY_ID, billingRecordMock);
		verify(historyRepositoryMock).saveAndFlush(any());
	}

	@Test
	void sendBillingRecords_noContractMatch() {
		// Arrange
		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> handler.sendBillingRecords(MUNICIPALITY_ID, CONTRACT_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("No contract found");

		verify(contractIntegrationMock).getContract(MUNICIPALITY_ID, CONTRACT_ID);
	}

	@Test
	void sendBillingRecords_shouldThrowProblem_whenPreprocessorFails() {
		// Arrange
		when(contractIntegrationMock.getContract(MUNICIPALITY_ID, CONTRACT_ID)).thenReturn(Optional.of(contractMock));
		when(contractMapperMock.createBillingRecord(MUNICIPALITY_ID, contractMock)).thenReturn(billingRecordMock);
		when(billingPreprocessorClientMock.createBillingRecord(MUNICIPALITY_ID, billingRecordMock)).thenThrow(new RuntimeException("Preprocessor failure"));

		// Act & Assert
		assertThatThrownBy(() -> handler.sendBillingRecords(MUNICIPALITY_ID, CONTRACT_ID))
			.isInstanceOf(ThrowableProblem.class)
			.hasMessageContaining("Failed to send billing record to billing preprocessor");

		verify(contractIntegrationMock).getContract(MUNICIPALITY_ID, CONTRACT_ID);
		verify(contractMapperMock).createBillingRecord(MUNICIPALITY_ID, contractMock);
		verify(billingPreprocessorClientMock).createBillingRecord(MUNICIPALITY_ID, billingRecordMock);
	}
}
