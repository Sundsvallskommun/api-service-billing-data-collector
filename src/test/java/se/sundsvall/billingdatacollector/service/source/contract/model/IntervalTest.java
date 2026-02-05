package se.sundsvall.billingdatacollector.service.source.contract.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.billingdatacollector.service.source.contract.model.Interval.HALF_YEARLY;
import static se.sundsvall.billingdatacollector.service.source.contract.model.Interval.MONTHLY;
import static se.sundsvall.billingdatacollector.service.source.contract.model.Interval.QUARTERLY;
import static se.sundsvall.billingdatacollector.service.source.contract.model.Interval.YEARLY;

import generated.se.sundsvall.contract.IntervalType;
import org.junit.jupiter.api.Test;

class IntervalTest {

	@Test
	void enums() {
		assertThat(Interval.values()).containsExactlyInAnyOrder(MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY);
	}

	@Test
	void accrualKey() {
		assertThat(MONTHLY.getAccrualKey()).isEqualTo("N_1");
		assertThat(QUARTERLY.getAccrualKey()).isEqualTo("N_4");
		assertThat(HALF_YEARLY.getAccrualKey()).isEqualTo("N_6");
		assertThat(YEARLY.getAccrualKey()).isEqualTo("N_12");
	}

	@Test
	void splitFactor() {
		assertThat(MONTHLY.getSplitFactor()).isEqualTo(12);
		assertThat(QUARTERLY.getSplitFactor()).isEqualTo(4);
		assertThat(HALF_YEARLY.getSplitFactor()).isEqualTo(2);
		assertThat(YEARLY.getSplitFactor()).isEqualTo(1);
	}

	@Test
	void test() {
		assertThat(Interval.getByIntervalType(null)).isNull();
		assertThat(Interval.getByIntervalType(IntervalType.MONTHLY)).isEqualTo(MONTHLY);
		assertThat(Interval.getByIntervalType(IntervalType.QUARTERLY)).isEqualTo(QUARTERLY);
		assertThat(Interval.getByIntervalType(IntervalType.HALF_YEARLY)).isEqualTo(HALF_YEARLY);
		assertThat(Interval.getByIntervalType(IntervalType.YEARLY)).isEqualTo(YEARLY);
	}
}
