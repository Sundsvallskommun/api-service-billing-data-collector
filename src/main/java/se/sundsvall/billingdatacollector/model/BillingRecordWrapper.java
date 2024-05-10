package se.sundsvall.billingdatacollector.model;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class BillingRecordWrapper {

	private BillingRecord billingRecord;
	private String legalId;
	private String familyId;
}
