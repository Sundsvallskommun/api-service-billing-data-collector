package se.sundsvall.billingdatacollector.integration.scb;

import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import generated.se.sundsvall.scb.VariableSelection;
import generated.se.sundsvall.scb.VariablesSelection;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.zalando.problem.Problem;

public final class ScbMapper {
	private static final String CONTENTS_CODE = "ContentsCode";
	private static final String CONTENTS_CODE_VALUE = "000004VU";
	private static final String TIME = "Tid";
	private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy'M'MM");

	private ScbMapper() {
		// To prevent instansiation
	}

	public static VariablesSelection toVariablesSelection(YearMonth period) {
		return ofNullable(period)
			.map(p -> new VariablesSelection()
				.addSelectionItem(new VariableSelection().variableCode(TIME).addValueCodesItem(PERIOD_FORMAT.format(p)))
				.addSelectionItem(new VariableSelection().variableCode(CONTENTS_CODE).addValueCodesItem(CONTENTS_CODE_VALUE)))
			.orElseThrow(() -> Problem.valueOf(INTERNAL_SERVER_ERROR, "Period must be provided"));
	}
}
