package se.sundsvall.billingdatacollector.service.source.contract;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static generated.se.sundsvall.contract.StakeholderRole.LESSEE;
import static generated.se.sundsvall.contract.StakeholderRole.LESSOR;
import static generated.se.sundsvall.contract.StakeholderRole.PRIMARY_BILLING_PARTY;
import static generated.se.sundsvall.contract.StakeholderRole.PROPERTY_OWNER;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.contract.Address;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Stakeholder;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;

@ExtendWith(MockitoExtension.class)
class ContractMapperTest {
	private static final String CARE_OF = "careOf";
	private static final String CONTRACT_ID = "contractId";
	private static final String FIRST_NAME = "firstName";
	private static final String LAST_NAME = "lastName";
	private static final String ORGANIZATION_NAME = "organizationName";
	private static final String STREET_ADDRESS = "streetAddress";
	private static final String TOWN = "town";
	private static final String PARTY_ID = "partyId";
	private static final String POSTAL_CODE = "postalCode";

	@Mock
	private Contract contractMock;

	@InjectMocks
	private ContractMapper mapper;

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"", " "
	})
	void toBillingRecord_basicAttributes(String externalReferenceId) {
		// Arrange
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getExternalReferenceId()).thenReturn(externalReferenceId);

		// Act
		final var result = mapper.toBillingRecord(contractMock);

		// Assert & verify
		assertThat(result.getApprovedBy()).isEqualTo("CONTRACT-SERVICE");
		assertThat(result.getCategory()).isEqualTo("MEX_INVOICE");
		assertThat(result.getInvoice()).isNull();
		assertThat(result.getRecipient()).isNull(); // Tested by other test methods
		assertThat(result.getStatus()).isEqualTo(APPROVED);
		assertThat(result.getType()).isEqualTo(EXTERNAL);
		assertThat(result.getExtraParameters()).hasSize(2)
			.containsExactlyInAnyOrderEntriesOf(Map.of(
				"contractId", isBlank(externalReferenceId) ? CONTRACT_ID : "%s (%s)".formatted(CONTRACT_ID, externalReferenceId),
				"index", "TODO"));

		verify(contractMock).getContractId();
		verify(contractMock).getExternalReferenceId();
		verify(contractMock).getStakeholders();
		verifyNoMoreInteractions(contractMock);
	}

	@Test
	void toBillingRecordFromNull() {
		assertThat(mapper.toBillingRecord(null)).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recipientAttributesAgrumentProvider")
	void toBilling_recipientAttributes(String description, List<Stakeholder> stakeholders, boolean hasMatch) {
		// Arrange
		when(contractMock.getStakeholders()).thenReturn(stakeholders);

		// Act
		final var result = mapper.toBillingRecord(contractMock);

		// Assert & verify
		if (hasMatch) {
			assertThat(result.getRecipient()).isNotNull().satisfies(recipient -> {
				assertThat(recipient.getAddressDetails()).isNotNull();
				assertThat(recipient.getAddressDetails().getCareOf()).isEqualTo(CARE_OF);
				assertThat(recipient.getAddressDetails().getCity()).isEqualTo(TOWN);
				assertThat(recipient.getAddressDetails().getPostalCode()).isEqualTo(POSTAL_CODE);
				assertThat(recipient.getAddressDetails().getStreet()).isEqualTo(STREET_ADDRESS);
				assertThat(recipient.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(recipient.getLastName()).isEqualTo(LAST_NAME);
				assertThat(recipient.getOrganizationName()).isEqualTo(ORGANIZATION_NAME);
				assertThat(recipient.getPartyId()).isEqualTo(PARTY_ID);
				assertThat(recipient.getLegalId()).isNull();
				assertThat(recipient.getUserId()).isNull();
			});
		} else {
			assertThat(result.getRecipient()).isNull();
		}

		verify(contractMock).getContractId();
		verify(contractMock).getExternalReferenceId();
		verify(contractMock).getStakeholders();
		verifyNoMoreInteractions(contractMock);
	}

	private static Stream<Arguments> recipientAttributesAgrumentProvider() {
		return Stream.of(
			Arguments.of("Stakeholders list is null", null, false),
			Arguments.of("Stakeholders list is emtpy", emptyList(), false),
			Arguments.of("Stakeholders list only contains stakeholder where role list is null", List.of(generateStakeholder()), false),
			Arguments.of("Stakeholders list only contains stakeholder where role list is empty", List.of(generateStakeholder().roles(emptyList())), false),
			Arguments.of("Stakeholders list only contains stakeholder with non matching roles", List.of(generateStakeholder().addRolesItem(LESSOR).addRolesItem(PROPERTY_OWNER)), false),
			Arguments.of("Stakeholders list only contains stakeholder with matching role", List.of(generateStakeholder().addRolesItem(PRIMARY_BILLING_PARTY)), true),
			Arguments.of("Stakeholders list only contains stakeholder with multiple roles including matching role", List.of(generateStakeholder().addRolesItem(LESSEE).addRolesItem(PRIMARY_BILLING_PARTY)), true));
	}

	@Test
	void toBilling_recipientAttributesWhenAddressIsMissingForBillingParty() {
		// Arrange
		when(contractMock.getStakeholders()).thenReturn(List.of(generateStakeholder().addRolesItem(PRIMARY_BILLING_PARTY).address(null)));

		// Act
		final var e = assertThrows(ThrowableProblem.class, () -> mapper.toBillingRecord(contractMock));

		// Assert & verify
		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getDetail()).isEqualTo("Manadatory address information not found for billing party with party id %s".formatted(PARTY_ID));
		verify(contractMock).getStakeholders();
		verifyNoMoreInteractions(contractMock);
	}

	private static Stakeholder generateStakeholder() {
		return new Stakeholder()
			.address(new Address()
				.careOf(CARE_OF)
				.streetAddress(STREET_ADDRESS)
				.town(TOWN)
				.postalCode(POSTAL_CODE))
			.firstName(FIRST_NAME)
			.lastName(LAST_NAME)
			.organizationName(ORGANIZATION_NAME)
			.partyId(PARTY_ID);
	}
}
