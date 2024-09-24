package se.sundsvall.billingdatacollector.integration.db.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;
import jakarta.persistence.PersistenceException;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith(ResourceLoaderExtension.class)
class BillingRecordWrapperConverterTest {

	private final BillingRecordWrapperConverter converter = new BillingRecordWrapperConverter();

	@Test
	void convertToDatabaseColumn(@Load("/billingpreprocessor/billing-record.json") String recordAsJson) {
		final var wrapper = converter.convertToEntityAttribute(recordAsJson);
		assertThat(wrapper).isNotNull();
		assertThat(wrapper.getFamilyId()).isEqualTo("358");
		assertThat(wrapper.getFlowInstanceId()).isEqualTo("12345");
		assertThat(wrapper.getLegalId()).isEqualTo("1234567890");
		assertThat(wrapper.getBillingRecord().getCategory()).isEqualTo("KUNDFAKTURA");

		final var invoiceRow = wrapper.getBillingRecord().getInvoice().getInvoiceRows().getFirst();

		assertThat(invoiceRow.getAccountInformation().getActivity()).isEqualTo("4165");
		assertThat(invoiceRow.getAccountInformation().getArticle()).isEqualTo("3452000 - ANKEBORG");
		assertThat(invoiceRow.getAccountInformation().getCostCenter()).isEqualTo("43200000");
		assertThat(invoiceRow.getAccountInformation().getCounterpart()).isEqualTo("86000000");
		assertThat(invoiceRow.getAccountInformation().getDepartment()).isEqualTo("315310");
		assertThat(invoiceRow.getAccountInformation().getSubaccount()).isEqualTo("345000");
		assertThat(invoiceRow.getCostPerUnit()).isEqualTo(700.0f);
		assertThat(invoiceRow.getDescriptions().getFirst()).isEqualTo("Julmarknad Ankeborg. 3 marknadsplatser");
		assertThat(invoiceRow.getQuantity()).isEqualTo(3);
		assertThat(invoiceRow.getTotalAmount()).isEqualTo(2100.0f);
		assertThat(invoiceRow.getVatCode()).isEqualTo("00");
		assertThat(wrapper.getBillingRecord().getRecipient().getAddressDetails().getCity()).isEqualTo("ANKEBORG");
		assertThat(wrapper.getBillingRecord().getRecipient().getAddressDetails().getPostalCode()).isEqualTo("862 96");
		assertThat(wrapper.getBillingRecord().getRecipient().getAddressDetails().getStreet()).isEqualTo("Ankeborgsv 150");
		assertThat(wrapper.getBillingRecord().getRecipient().getFirstName()).isEqualTo("Kalle");
		assertThat(wrapper.getBillingRecord().getRecipient().getLastName()).isEqualTo("Anka");
		assertThat(wrapper.getBillingRecord().getRecipient().getPartyId()).isEqualTo("fb2f0290-3820-11ed-a261-0242ac120002");
		assertThat(wrapper.getBillingRecord().getStatus()).isEqualTo(Status.APPROVED);
		assertThat(wrapper.getBillingRecord().getType()).isEqualTo(Type.EXTERNAL);
	}

	@Test
	void convertToEntityAttribute(@Load("/billingpreprocessor/billing-record.json") String recordAsJson) {
		final var wrapper = BillingRecordWrapper.builder()
			.withMunicipalityId("2281")
			.withFamilyId("358")
			.withFlowInstanceId("12345")
			.withLegalId("1234567890")
			.withBillingRecord(new BillingRecord()
				.category("KUNDFAKTURA")
				.invoice(new Invoice()
					.invoiceRows(List.of(new InvoiceRow()
						.accountInformation(new AccountInformation()
							.activity("4165")
							.article("3452000 - ANKEBORG")
							.costCenter("43200000")
							.counterpart("86000000")
							.department("315310")
							.subaccount("345000"))
						.costPerUnit(700.0f)
						.descriptions(List.of("Julmarknad Ankeborg. 3 marknadsplatser"))
						.quantity(3f)
						.totalAmount(2100.0f)
						.vatCode("00"))))
				.recipient(new Recipient()
					.addressDetails(new AddressDetails()
						.city("ANKEBORG")
						.postalCode("862 96")
						.street("Ankeborgsv 150"))
					.firstName("Kalle")
					.lastName("Anka")
					.partyId("fb2f0290-3820-11ed-a261-0242ac120002"))
				.status(Status.APPROVED)
				.type(Type.EXTERNAL))
			.build();

		final var json = converter.convertToDatabaseColumn(wrapper);
		assertThat(json).isEqualToIgnoringWhitespace(recordAsJson);
	}

	@Test
	void testConvertToDatabaseColumn_shouldReturnNull_whenWrapperIsNull() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}

	@Test
	void testConvertToEntityAttribute_shouldReturnNull_whenJsonIsNull() {
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}

	@Test
	void testConvertToEntityAttribute_shouldThrowException_whenUnableToDeserialize() {
		assertThatExceptionOfType(PersistenceException.class)
			.isThrownBy(() -> converter.convertToEntityAttribute("invalid json"))
			.satisfies(exception -> {
				assertThat(exception.getCause()).isInstanceOf(Exception.class);
				assertThat(exception.getMessage()).contains("Unable to deserialize billing data wrapper");
			});
	}
}
