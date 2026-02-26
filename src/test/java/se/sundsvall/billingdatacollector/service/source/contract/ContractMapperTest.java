package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.contract.Address;
import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.Fees;
import generated.se.sundsvall.contract.IntervalType;
import generated.se.sundsvall.contract.Invoicing;
import generated.se.sundsvall.contract.Stakeholder;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
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
import se.sundsvall.billingdatacollector.integration.scb.ScbIntegration;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;
import se.sundsvall.dept44.problem.ThrowableProblem;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static generated.se.sundsvall.contract.StakeholderRole.LESSEE;
import static generated.se.sundsvall.contract.StakeholderRole.LESSOR;
import static generated.se.sundsvall.contract.StakeholderRole.PRIMARY_BILLING_PARTY;
import static generated.se.sundsvall.contract.StakeholderRole.PROPERTY_OWNER;
import static generated.se.sundsvall.contract.StakeholderType.PERSON;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
class ContractMapperTest {
	private static final String MUNICIPALITY_ID = "municipalityId";
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
	private ScbIntegration scbIntegrationMock;

	@Mock
	private SettingsProvider settingsProviderMock;

	@Mock
	private Contract contractMock;

	@Mock
	private Fees feesMock;

	@Mock
	private Invoicing invoicingMock;

	@Mock
	private CounterpartMappingService counterpartMappingServiceMock;

	@InjectMocks
	private ContractMapper mapper;

	@AfterEach
	void verifyNoMoreMockInteractions() {
		verifyNoMoreInteractions(
			scbIntegrationMock,
			settingsProviderMock);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
		"", " "
	})
	void createBillingRecord_basicAttributes(String externalReferenceId) {
		// Arrange
		final var yearly = BigDecimal.valueOf(1000.49);
		final var intervalType = IntervalType.QUARTERLY;
		final var activity = "activity";
		final var costCenter = "costCenter";
		final var department = "department";
		final var subaccount = "subaccount";
		final var vatCode = "vatCode";

		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearly);
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(invoicingMock.getInvoiceInterval()).thenReturn(intervalType);
		when(settingsProviderMock.isLeaseTypeSettingsPresent(contractMock)).thenReturn(true);
		when(settingsProviderMock.getActivity(contractMock)).thenReturn(activity);
		when(settingsProviderMock.getCostCenter(contractMock)).thenReturn(costCenter);
		when(settingsProviderMock.getDepartment(contractMock)).thenReturn(department);
		when(settingsProviderMock.getSubaccount(contractMock)).thenReturn(subaccount);
		when(settingsProviderMock.getVatCode(contractMock)).thenReturn(vatCode);
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getExternalReferenceId()).thenReturn(externalReferenceId);

		// Act
		final var result = mapper.createBillingRecord(MUNICIPALITY_ID, contractMock);

		// Assert & verify
		assertThat(result.getApprovedBy()).isEqualTo("CONTRACT-SERVICE");
		assertThat(result.getCategory()).isEqualTo("MEX_INVOICE");
		assertThat(result.getInvoice()).isNotNull().hasAllNullFieldsOrPropertiesExcept("ourReference", "invoiceRows").satisfies(invoice -> {
			assertThat(invoice.getOurReference()).isEqualTo(CONTRACT_ID);
			assertThat(invoice.getInvoiceRows()).hasSize(1);

			final var invoiceRow = invoice.getInvoiceRows().getFirst();
			assertThat(invoiceRow.getDescriptions()).isEmpty();
			assertThat(invoiceRow.getDetailedDescriptions()).isEmpty();
			assertThat(invoiceRow.getQuantity()).isEqualTo(BigDecimal.ONE);
			assertThat(invoiceRow.getCostPerUnit()).isEqualTo(BigDecimal.valueOf(250.12));
			assertThat(invoiceRow.getVatCode()).isEqualTo(vatCode);
			assertThat(invoiceRow.getTotalAmount()).isNull();
			assertThat(invoiceRow.getAccountInformation()).hasSize(1);

			final var accountInfo = invoiceRow.getAccountInformation().getFirst();
			assertThat(accountInfo.getActivity()).isEqualTo(activity);
			assertThat(accountInfo.getAmount()).isEqualTo(BigDecimal.valueOf(250.12));
			assertThat(accountInfo.getCostCenter()).isEqualTo(costCenter);
			assertThat(accountInfo.getDepartment()).isEqualTo(department);
			assertThat(accountInfo.getSubaccount()).isEqualTo(subaccount);
			assertThat(accountInfo.getAccuralKey()).isNull();
			assertThat(accountInfo.getArticle()).isNull();
			assertThat(accountInfo.getCounterpart()).isNull();
			assertThat(accountInfo.getProject()).isNull();
		});
		assertThat(result.getRecipient()).isNull(); // Tested by other test methods
		assertThat(result.getStatus()).isEqualTo(APPROVED);
		assertThat(result.getType()).isEqualTo(EXTERNAL);
		assertThat(result.getExtraParameters())
			.hasSize(1)
			.containsExactly(Map.entry("contractId", isBlank(externalReferenceId) ? CONTRACT_ID : "%s (%s)".formatted(CONTRACT_ID, externalReferenceId)));

		verify(contractMock, times(2)).getContractId();
		verify(contractMock, times(2)).getExternalReferenceId();
		verify(contractMock, times(2)).getStakeholders();
		verify(settingsProviderMock).isLeaseTypeSettingsPresent(contractMock);
		verify(settingsProviderMock).getActivity(contractMock);
		verify(settingsProviderMock).getCostCenter(contractMock);
		verify(settingsProviderMock).getDepartment(contractMock);
		verify(settingsProviderMock).getSubaccount(contractMock);
		verify(settingsProviderMock).getVatCode(contractMock);
	}

	@Test
	void toBillingRecordFromNull() {
		assertThat(mapper.createBillingRecord(MUNICIPALITY_ID, null)).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recipientAttributesArgumentProvider")
	void createBillingRecord_recipientAttributes(String description, List<Stakeholder> stakeholders, boolean hasMatch) {
		// Arrange
		final var yearly = BigDecimal.valueOf(1000.49);
		final var intervalType = IntervalType.QUARTERLY;

		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getStakeholders()).thenReturn(stakeholders);
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearly);
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(invoicingMock.getInvoiceInterval()).thenReturn(intervalType);

		// Act
		final var result = mapper.createBillingRecord(MUNICIPALITY_ID, contractMock);

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

		verify(contractMock, times(2)).getContractId();
		verify(contractMock, times(2)).getExternalReferenceId();
		verify(contractMock).getStakeholders();
		verify(settingsProviderMock).isLeaseTypeSettingsPresent(contractMock);
		verify(settingsProviderMock).getVatCode(contractMock);
	}

	private static Stream<Arguments> recipientAttributesArgumentProvider() {
		return Stream.of(
			Arguments.of("Stakeholders list is null", null, false),
			Arguments.of("Stakeholders list is empty", emptyList(), false),
			Arguments.of("Stakeholders list only contains stakeholder where role list is null", List.of(generateStakeholder()), false),
			Arguments.of("Stakeholders list only contains stakeholder where role list is empty", List.of(generateStakeholder().roles(emptyList())), false),
			Arguments.of("Stakeholders list only contains stakeholder with non matching roles", List.of(generateStakeholder().addRolesItem(LESSOR).addRolesItem(PROPERTY_OWNER)), false),
			Arguments.of("Stakeholders list only contains stakeholder with matching role", List.of(generateStakeholder().addRolesItem(PRIMARY_BILLING_PARTY)), true),
			Arguments.of("Stakeholders list only contains stakeholder with multiple roles including matching role", List.of(generateStakeholder().addRolesItem(LESSEE).addRolesItem(PRIMARY_BILLING_PARTY)), true));
	}

	@Test
	void createBillingRecord_recipientAttributesWhenAddressIsMissingForBillingParty() {
		final var yearly = BigDecimal.valueOf(1000.49);
		final var intervalType = IntervalType.QUARTERLY;

		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(contractMock.getStakeholders()).thenReturn(List.of(generateStakeholder().addRolesItem(PRIMARY_BILLING_PARTY).address(null)));
		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearly);
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(invoicingMock.getInvoiceInterval()).thenReturn(intervalType);

		// Act
		final var e = assertThrows(ThrowableProblem.class, () -> mapper.createBillingRecord(MUNICIPALITY_ID, contractMock));

		// Assert & verify
		assertThat(e.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
		assertThat(e.getDetail()).isEqualTo("Mandatory address information not found for billing party with party id %s".formatted(PARTY_ID));
		verify(contractMock).getStakeholders();
		verify(settingsProviderMock).isLeaseTypeSettingsPresent(contractMock);
		verify(settingsProviderMock).getVatCode(contractMock);
	}

	@Test
	void createBillingRecord_withIndexedPricesAndCounterpart() {
		// Arrange
		final var yearly = BigDecimal.valueOf(10000);
		final var intervalType = IntervalType.YEARLY;
		final var activity = "activity";
		final var costCenter = "costCenter";
		final var department = "department";
		final var subaccount = "subaccount";
		final var counterpart = "counterpart";
		final var vatCode = "vatCode";
		final var indexType = "KPI 80";
		final var kpiIndex = BigDecimal.valueOf(415.51);
		final var billableStakeholder = generateStakeholder().addRolesItem(PRIMARY_BILLING_PARTY).roles(List.of(PRIMARY_BILLING_PARTY, LESSEE));

		when(contractMock.getFees()).thenReturn(feesMock);
		when(feesMock.getYearly()).thenReturn(yearly);
		when(feesMock.getIndexType()).thenReturn(indexType);
		when(feesMock.getIndexNumber()).thenReturn(409);
		when(feesMock.getIndexationRate()).thenReturn(BigDecimal.ONE);
		when(contractMock.getInvoicing()).thenReturn(invoicingMock);
		when(invoicingMock.getInvoiceInterval()).thenReturn(intervalType);
		when(settingsProviderMock.isLeaseTypeSettingsPresent(contractMock)).thenReturn(true);
		when(settingsProviderMock.getActivity(contractMock)).thenReturn(activity);
		when(settingsProviderMock.getCostCenter(contractMock)).thenReturn(costCenter);
		when(settingsProviderMock.getDepartment(contractMock)).thenReturn(department);
		when(settingsProviderMock.getSubaccount(contractMock)).thenReturn(subaccount);
		when(settingsProviderMock.getVatCode(contractMock)).thenReturn(vatCode);
		when(contractMock.getContractId()).thenReturn(CONTRACT_ID);
		when(scbIntegrationMock.getKPI(KPIBaseYear.KPI_80, YearMonth.now().withMonth(Month.OCTOBER.getValue()))).thenReturn(kpiIndex);
		when(contractMock.getStakeholders()).thenReturn(List.of(generateStakeholder().roles(List.of(PRIMARY_BILLING_PARTY, LESSEE))
			.type(PERSON)));
		when(counterpartMappingServiceMock.findCounterpart(MUNICIPALITY_ID, billableStakeholder.getPartyId(), PERSON.getValue())).thenReturn(counterpart);
		// Act
		final var result = mapper.createBillingRecord(MUNICIPALITY_ID, contractMock);

		// Assert & verify
		assertThat(result.getApprovedBy()).isEqualTo("CONTRACT-SERVICE");
		assertThat(result.getCategory()).isEqualTo("MEX_INVOICE");
		assertThat(result.getInvoice()).isNotNull().hasAllNullFieldsOrPropertiesExcept("ourReference", "invoiceRows").satisfies(invoice -> {
			assertThat(invoice.getOurReference()).isEqualTo(CONTRACT_ID);
			assertThat(invoice.getInvoiceRows()).hasSize(1);

			final var invoiceRow = invoice.getInvoiceRows().getFirst();
			assertThat(invoiceRow.getDescriptions()).isEmpty();
			assertThat(invoiceRow.getDetailedDescriptions()).isEmpty();
			assertThat(invoiceRow.getQuantity()).isEqualTo(BigDecimal.ONE);
			assertThat(invoiceRow.getCostPerUnit()).isEqualTo(BigDecimal.valueOf(10159.17));
			assertThat(invoiceRow.getVatCode()).isEqualTo(vatCode);
			assertThat(invoiceRow.getTotalAmount()).isNull();
			assertThat(invoiceRow.getAccountInformation()).hasSize(1);

			final var accountInfo = invoiceRow.getAccountInformation().getFirst();
			assertThat(accountInfo.getActivity()).isEqualTo(activity);
			assertThat(accountInfo.getAmount()).isEqualTo(BigDecimal.valueOf(10159.17));
			assertThat(accountInfo.getCostCenter()).isEqualTo(costCenter);
			assertThat(accountInfo.getDepartment()).isEqualTo(department);
			assertThat(accountInfo.getSubaccount()).isEqualTo(subaccount);
			assertThat(accountInfo.getAccuralKey()).isNull();
			assertThat(accountInfo.getArticle()).isNull();
			assertThat(accountInfo.getCounterpart()).isEqualTo(counterpart);
			assertThat(accountInfo.getProject()).isNull();
		});
		assertThat(result.getRecipient()).isNotNull(); // Tested by other test methods
		assertThat(result.getStatus()).isEqualTo(APPROVED);
		assertThat(result.getType()).isEqualTo(EXTERNAL);
		assertThat(result.getExtraParameters()).hasSize(2).containsExactlyInAnyOrderEntriesOf(Map.of(
			"contractId", CONTRACT_ID,
			"index", kpiIndex.toString()));

		verify(contractMock, times(2)).getContractId();
		verify(contractMock, times(2)).getExternalReferenceId();
		verify(contractMock, times(2)).getStakeholders();
		verify(settingsProviderMock).isLeaseTypeSettingsPresent(contractMock);
		verify(settingsProviderMock).getActivity(contractMock);
		verify(settingsProviderMock).getCostCenter(contractMock);
		verify(settingsProviderMock).getDepartment(contractMock);
		verify(settingsProviderMock).getSubaccount(contractMock);
		verify(settingsProviderMock).getVatCode(contractMock);

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
