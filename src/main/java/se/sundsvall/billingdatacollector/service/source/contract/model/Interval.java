package se.sundsvall.billingdatacollector.service.source.contract.model;

import generated.se.sundsvall.contract.IntervalType;
import java.util.Objects;
import java.util.stream.Stream;
import se.sundsvall.dept44.problem.Problem;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.NOT_FOUND;

public enum Interval {
	MONTHLY("N_1", 12),
	QUARTERLY("N_4", 4),
	HALF_YEARLY("N_6", 2),
	YEARLY("N_12", 1)

	;

	private Interval(String accrualKey, int splitFactor) {
		this.accrualKey = accrualKey;
		this.splitFactor = splitFactor;
	}

	private final String accrualKey;
	private final int splitFactor;

	public String getAccrualKey() {
		return accrualKey;
	}

	public int getSplitFactor() {
		return splitFactor;
	}

	public static Interval getByIntervalType(IntervalType intervalType) {
		if (isNull(intervalType)) {
			return null;
		}

		return Stream.of(Interval.values())
			.filter(interval -> Objects.equals(interval.name(), intervalType.name()))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "The interval type %s has no corresponding member in the Interval enum".formatted(intervalType.name())));
	}
}
