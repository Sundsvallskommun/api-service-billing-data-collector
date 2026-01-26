package se.sundsvall.billingdatacollector.integration.scb;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.billingdatacollector.integration.scb.ScbMapper.toVariablesSelection;

import generated.se.sundsvall.scb.Dataset;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;

@Component
public class ScbIntegration {
	private static final String CACHE_NAME = "kpiData";
	private static final String LANGUAGE = "sv";
	private static final String FORMAT = "json-stat2";

	private final ScbClient client;

	public ScbIntegration(ScbClient client) {
		this.client = client;
	}

	@Cacheable(CACHE_NAME)
	public BigDecimal getKPI(KPIBaseYear kpiBase, YearMonth period) {
		final var response = client.getKPI(kpiBase.getTableIdReference(), LANGUAGE, FORMAT, toVariablesSelection(period));

		return ofNullable(response)
			.map(Dataset::getValue).orElse(emptyList()).stream()
			.map(BigDecimal::valueOf)
			.map(bd -> bd.setScale(2, RoundingMode.HALF_EVEN))
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(NOT_FOUND, "KPI based on %s for period %s was not found".formatted(kpiBase.name(), period)));
	}
}
