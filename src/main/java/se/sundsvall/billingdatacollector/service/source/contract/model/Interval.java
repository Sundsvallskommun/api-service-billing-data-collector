package se.sundsvall.billingdatacollector.service.source.contract.model;

import static java.util.Objects.isNull;
import static org.zalando.problem.Status.NOT_FOUND;

import generated.se.sundsvall.contract.IntervalType;
import java.util.Objects;
import java.util.stream.Stream;
import org.zalando.problem.Problem;

public enum Interval {
	MONTHLY("N_1", 12),
	QUARTERLY("N_4", 4),
	HALF_YEARLY("N_6", 2),
	YEARLY("N_12", 1)

	;

	private Interval(String accuralKey, int splitFactor) {
		this.accuralKey = accuralKey;
		this.splitFactor = splitFactor;
	}

	private final String accuralKey;
	private final int splitFactor;

	public String getAccuralKey() {
		return accuralKey;
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
