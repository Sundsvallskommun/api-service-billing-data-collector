package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(setterPrefix = "with")
@ToString
public class OrganizationInformation {
	private String organizationNumber;
	private String name;
	private String streetAddress;
	private String zipCode;
	private String city;
	private String careOf;
	private String motpart;
}
