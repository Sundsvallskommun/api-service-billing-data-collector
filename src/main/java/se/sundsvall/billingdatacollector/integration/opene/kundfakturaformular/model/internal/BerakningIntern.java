package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(setterPrefix = "with")
public class BerakningIntern {
	private String queryID;
	private String name;
	private String fakturatextIntern;
	private String antalIntern;
	private String aPrisIntern;

	// Bean matchers checks for the wrong method name for "aPrisExtern"
	// https://github.com/orien/bean-matchers/issues/7
	@Override
	public String toString() {
		return "BerakningIntern{" +
			"queryID='" + queryID + '\'' +
			", name='" + name + '\'' +
			", fakturatextIntern='" + fakturatextIntern + '\'' +
			", antalIntern='" + antalIntern + '\'' +
			", APrisIntern='" + aPrisIntern + '\'' +
			'}';
	}
}
