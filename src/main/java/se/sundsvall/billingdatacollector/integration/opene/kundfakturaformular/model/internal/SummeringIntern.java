package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(setterPrefix = "with")
public class SummeringIntern {
	private String queryID;
	private String name;
	private String totSummeringIntern;
}
