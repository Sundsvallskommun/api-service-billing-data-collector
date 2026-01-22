package se.sundsvall.billingdatacollector.service.source.contract;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.contract.Contract;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractMapperTest {
	private static final String CONTRACT_ID = "contractId";

	@Mock
	private Contract contractMock;

	@InjectMocks
	private ContractMapper mapper;

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"", " "
	})
	void toBillingRecord(String externalReferenceId) {
		// Arrange
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getExternalReferenceId()).thenReturn(externalReferenceId);

		// Act
		final var result = mapper.toBillingRecord(contractMock);

		// Assert & verify
		assertThat(result.getApprovedBy()).isEqualTo("CONTRACT-SERVICE");
		assertThat(result.getCategory()).isEqualTo("MEX_INVOICE");
		assertThat(result.getInvoice()).isNull();
		assertThat(result.getRecipient()).isNull();
		assertThat(result.getStatus()).isEqualTo(APPROVED);
		assertThat(result.getType()).isEqualTo(EXTERNAL);
		assertThat(result.getExtraParameters()).hasSize(2)
			.containsExactlyInAnyOrderEntriesOf(Map.of(
				"contractId", isBlank(externalReferenceId) ? CONTRACT_ID : "%s (%s)".formatted(CONTRACT_ID, externalReferenceId),
				"index", "TODO"));

		verify(contractMock).getContractId();
		verify(contractMock).getExternalReferenceId();
		verifyNoMoreInteractions(contractMock);
	}

	@Test
	void toBillingRecordFromNull() {
		assertThat(mapper.toBillingRecord(null)).isNull();
	}
}
