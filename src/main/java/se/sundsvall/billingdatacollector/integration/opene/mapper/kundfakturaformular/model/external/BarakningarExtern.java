package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder(setterPrefix = "with")
@ToString
public class BarakningarExtern {
	private String queryID;
	private String name;
	private String fakturatextExtern;
	private String antalExtern;
	private String aPrisExtern;
}