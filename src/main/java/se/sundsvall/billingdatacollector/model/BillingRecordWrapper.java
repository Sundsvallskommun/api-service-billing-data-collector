package se.sundsvall.billingdatacollector.model;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class BillingRecordWrapper {

	private BillingRecord billingRecord;
	private String familyId;
	private String flowInstanceId;
	private String legalId;
	private String municipalityId;

	@ToString.Include(name = "recipientPrivatePerson")  // Makes hasValidBeanToString pass
	private boolean isRecipientPrivatePerson;
}
