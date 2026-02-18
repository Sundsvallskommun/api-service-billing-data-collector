package se.sundsvall.billingdatacollector.integration.scb.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear.KPI_2020;
import static se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear.KPI_80;

class KPIBaseYearTest {

	@Test
	void enums() {
		assertThat(KPIBaseYear.values()).containsExactlyInAnyOrder(KPI_80, KPI_2020);
	}

	@Test
	void enumValues() {
		assertThat(KPI_80.getTableIdReference()).isEqualTo("TAB5737");
		assertThat(KPI_2020.getTableIdReference()).isEqualTo("TAB6596");
	}
}
