package se.sundsvall.billingdatacollector.integration.db.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;

import org.junit.jupiter.api.Test;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;
import jakarta.persistence.PersistenceException;

class BillingRecordWrapperConverterTest {

	private final BillingRecordWrapperConverter converter = new BillingRecordWrapperConverter();

	@Test
	void convertToDatabaseColumn() {
		var wrapper = converter.convertToEntityAttribute(recordAsJson);
		assertThat(wrapper).isNotNull();
		assertThat(wrapper.getFamilyId()).isEqualTo("358");
		assertThat(wrapper.getFlowInstanceId()).isEqualTo("12345");
		assertThat(wrapper.getLegalId()).isEqualTo("1234567890");
		assertThat(wrapper.getBillingRecord().getCategory()).isEqualTo("KUNDFAKTURA");

		var invoiceRow = wrapper.getBillingRecord().getInvoice().getInvoiceRows().getFirst();

		assertThat(invoiceRow.getAccountInformation().getActivity()).isEqualTo("4165");
		assertThat(invoiceRow.getAccountInformation().getArticle()).isEqualTo("3452000 - GULLGÅRDEN");
		assertThat(invoiceRow.getAccountInformation().getCostCenter()).isEqualTo("43200000");
		assertThat(invoiceRow.getAccountInformation().getCounterpart()).isEqualTo("86000000");
		assertThat(invoiceRow.getAccountInformation().getDepartment()).isEqualTo("315310");
		assertThat(invoiceRow.getAccountInformation().getSubaccount()).isEqualTo("345000");
		assertThat(invoiceRow.getCostPerUnit()).isEqualTo(700.0f);
		assertThat(invoiceRow.getDescriptions().getFirst()).isEqualTo("Julmarknad Norra Berget. 3 marknadsplatser");
		assertThat(invoiceRow.getQuantity()).isEqualTo(3);
		assertThat(invoiceRow.getTotalAmount()).isEqualTo(2100.0f);
		assertThat(invoiceRow.getVatCode()).isEqualTo("00");
		assertThat(wrapper.getBillingRecord().getRecipient().getAddressDetails().getCity()).isEqualTo("NJURUNDA");
		assertThat(wrapper.getBillingRecord().getRecipient().getAddressDetails().getPostalCode()).isEqualTo("862 96");
		assertThat(wrapper.getBillingRecord().getRecipient().getAddressDetails().getStreet()).isEqualTo("MYRBODARNA 150");
		assertThat(wrapper.getBillingRecord().getRecipient().getFirstName()).isEqualTo("Christina");
		assertThat(wrapper.getBillingRecord().getRecipient().getLastName()).isEqualTo("Näslund");
		assertThat(wrapper.getBillingRecord().getRecipient().getPartyId()).isEqualTo("fb2f0290-3820-11ed-a261-0242ac120002");
		assertThat(wrapper.getBillingRecord().getStatus()).isEqualTo(Status.APPROVED);
		assertThat(wrapper.getBillingRecord().getType()).isEqualTo(Type.EXTERNAL);
	}

	@Test
	void convertToEntityAttribute() {
		var wrapper = BillingRecordWrapper.builder()
			.withFamilyId("358")
			.withFlowInstanceId("12345")
			.withLegalId("1234567890")
			.withBillingRecord(BillingRecord.builder()
				.withCategory("KUNDFAKTURA")
				.withInvoice(Invoice.builder()
					.withInvoiceRows(List.of(InvoiceRow.builder()
						.withAccountInformation(AccountInformation.builder()
							.withActivity("4165")
							.withArticle("3452000 - GULLGÅRDEN")
							.withCostCenter("43200000")
							.withCounterpart("86000000")
							.withDepartment("315310")
							.withSubaccount("345000")
							.build())
						.withCostPerUnit(700.0f)
						.withDescriptions(List.of("Julmarknad Norra Berget. 3 marknadsplatser"))
						.withQuantity(3)
						.withTotalAmount(2100.0f)
						.withVatCode("00")
						.build()))
					.build())
				.withRecipient(Recipient.builder()
					.withAddressDetails(AddressDetails.builder()
						.withCity("NJURUNDA")
						.withPostalCode("862 96")
						.withStreet("MYRBODARNA 150")
						.build())
					.withFirstName("Christina")
					.withLastName("Näslund")
					.withPartyId("fb2f0290-3820-11ed-a261-0242ac120002")
					.build())
				.withStatus(Status.APPROVED)
				.withType(Type.EXTERNAL)
				.build())
			.build();

		var json = converter.convertToDatabaseColumn(wrapper);
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

	private final String recordAsJson = """
		{
		 	"billingRecord": {
		 		"id": null,
		 		"category": "KUNDFAKTURA",
		 		"type": "EXTERNAL",
		 		"status": "APPROVED",
		 		"approvedBy": null,
		 		"approved": null,
		 		"recipient": {
		 			"partyId": "fb2f0290-3820-11ed-a261-0242ac120002",
		 			"legalId": null,
		 			"organizationName": null,
		 			"firstName": "Christina",
		 			"lastName": "Näslund",
		 			"userId": null,
		 			"addressDetails": {
		 				"street": "MYRBODARNA 150",
		 				"careOf": null,
		 				"postalCode": "862 96",
		 				"city": "NJURUNDA"
		 			}
		 		},
		 		"invoice": {
		 			"customerId": null,
		 			"description": null,
		 			"ourReference": null,
		 			"customerReference": null,
		 			"referenceId": null,
		 			"date": null,
		 			"dueDate": null,
		 			"totalAmount": null,
		 			"invoiceRows": [
		 				{
		 					"descriptions": [
		 						"Julmarknad Norra Berget. 3 marknadsplatser"
		 					],
		 					"detailedDescriptions": null,
		 					"totalAmount": 2100.0,
		 					"vatCode": "00",
		 					"costPerUnit": 700.0,
		 					"quantity": 3,
		 					"accountInformation": {
		 						"costCenter": "43200000",
		 						"subaccount": "345000",
		 						"department": "315310",
		 						"accuralKey": null,
		 						"activity": "4165",
		 						"article": "3452000 - GULLGÅRDEN",
		 						"project": null,
		 						"counterpart": "86000000"
		 					}
		 				}
		 			]
		 		},
		 		"created": null,
		 		"modified": null
		 	},
		 	"familyId": "358",
		 	"flowInstanceId": "12345",
		 	"legalId": "1234567890"
		 }
		""";
}
