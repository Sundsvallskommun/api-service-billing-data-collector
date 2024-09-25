package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder(setterPrefix = "with")
@ToString
public class BerakningarIntern {
	private String queryID;
	private String name;
	private String fakturatextIntern;
	private String antalIntern;
	private String aPrisIntern;
}
