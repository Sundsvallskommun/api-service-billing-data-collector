package se.sundsvall.billingdatacollector.integration.scb.model;

public enum KPIBaseYear {

	KPI_80("TAB5737"),
	KPI_2020("TAB6596");

	private final String tableIdReference;

	KPIBaseYear(String tableIdReference) {
		this.tableIdReference = tableIdReference;
	}

	public String getTableIdReference() {
		return tableIdReference;
	}
}
