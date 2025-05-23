package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(setterPrefix = "with")
public class BerakningarExtern {
	private String queryID;
	private String name;
	private String fakturatextExtern;
	private String antalExtern;
	private String aPrisExtern;

	// Bean matchers checks for the wrong method name for "aPrisExtern"
	// https://github.com/orien/bean-matchers/issues/7
	@Override
	public String toString() {
		return "BerakningarExtern{" +
			"queryID='" + queryID + '\'' +
			", name='" + name + '\'' +
			", fakturatextExtern='" + fakturatextExtern + '\'' +
			", antalExtern='" + antalExtern + '\'' +
			", APrisExtern='" + aPrisExtern + '\'' +
			'}';
	}
}
