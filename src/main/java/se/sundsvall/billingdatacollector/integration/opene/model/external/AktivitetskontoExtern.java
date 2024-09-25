package se.sundsvall.billingdatacollector.integration.opene.model.external;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder(setterPrefix = "with")
@ToString
public class AktivitetskontoExtern {
	private String queryID;
	private String name;
	private String value;
}
