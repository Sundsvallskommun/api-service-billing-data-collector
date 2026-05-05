package se.sundsvall.billingdatacollector.api.model;

/**
 * Direction of invoicing — mirrors the contract service's enum but kept as a
 * BDC-owned type so the persistence layer (and the {@code ScheduledBilling}
 * REST DTO) does not depend on the generated Contract model.
 */
public enum InvoicedIn {
	ADVANCE,
	ARREARS
}
