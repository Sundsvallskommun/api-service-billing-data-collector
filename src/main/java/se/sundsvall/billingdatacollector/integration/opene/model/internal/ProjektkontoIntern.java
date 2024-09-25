package se.sundsvall.billingdatacollector.integration.opene.model.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder(setterPrefix = "with")
@ToString
public class ProjektkontoIntern {
	private String queryID;
	private String name;
	private String value;
}
