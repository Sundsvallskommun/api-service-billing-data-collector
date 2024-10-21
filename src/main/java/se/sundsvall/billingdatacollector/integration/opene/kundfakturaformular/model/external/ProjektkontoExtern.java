package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(setterPrefix = "with")
public class ProjektkontoExtern {
	private String queryID;
	private String name;
	private String value;
}
