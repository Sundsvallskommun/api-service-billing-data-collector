package se.sundsvall.billingdatacollector.api;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.api.model.ScbKpiResponse;
import se.sundsvall.billingdatacollector.integration.scb.ScbIntegration;
import se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.dept44.problem.violations.Violation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class ScbKpiResourceTest {

	private static final String PATH = "/{municipalityId}/scb/kpi";
	private static final String MUNICIPALITY_ID = "2281";
	private static final KPIBaseYear BASE_YEAR = KPIBaseYear.KPI_80;
	private static final YearMonth PERIOD = YearMonth.of(2024, 10);

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private ScbIntegration mockScbIntegration;

	@Test
	void testGetKpi() {
		// Arrange
		final var value = new BigDecimal("355.91");
		when(mockScbIntegration.getKPI(BASE_YEAR, PERIOD)).thenReturn(value);

		// Act
		final var result = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", BASE_YEAR.name())
				.queryParam("period", PERIOD.toString())
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ScbKpiResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getBaseYear()).isEqualTo(BASE_YEAR);
		assertThat(result.getPeriod()).isEqualTo(PERIOD);
		assertThat(result.getValue()).isEqualByComparingTo(value);

		verify(mockScbIntegration).getKPI(BASE_YEAR, PERIOD);
		verifyNoMoreInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiNotFound() {
		// Arrange
		when(mockScbIntegration.getKPI(BASE_YEAR, PERIOD))
			.thenThrow(Problem.valueOf(NOT_FOUND, "KPI based on %s for period %s was not found".formatted(BASE_YEAR.name(), PERIOD)));

		// Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", BASE_YEAR.name())
				.queryParam("period", PERIOD.toString())
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isNotFound()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(NOT_FOUND);
		assertThat(responseBody.getTitle()).isEqualTo(NOT_FOUND.getReasonPhrase());
		assertThat(responseBody.getDetail()).contains(BASE_YEAR.name(), PERIOD.toString());

		verify(mockScbIntegration).getKPI(BASE_YEAR, PERIOD);
		verifyNoMoreInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiWithInvalidMunicipalityId() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", BASE_YEAR.name())
				.queryParam("period", PERIOD.toString())
				.build("invalid"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getTitle()).isEqualTo("Constraint Violation");
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getViolations())
			.extracting(Violation::field, Violation::message)
			.containsExactlyInAnyOrder(tuple("getKpi.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiWithInvalidBaseYear() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", "NOPE")
				.queryParam("period", PERIOD.toString())
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);

		verifyNoInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiWithInvalidPeriod() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", BASE_YEAR.name())
				.queryParam("period", "not-a-date")
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);

		verifyNoInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiMissingBaseYear() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("period", PERIOD.toString())
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getDetail()).isEqualTo("Required parameter 'baseYear' is not present.");

		verifyNoInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiMissingPeriod() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", BASE_YEAR.name())
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getDetail()).isEqualTo("Required parameter 'period' is not present.");

		verifyNoInteractions(mockScbIntegration);
	}

	@Test
	void testGetKpiPassesAlternateBaseYear() {
		// Arrange — make sure both enum values bind correctly
		final var altPeriod = YearMonth.of(2024, 11);
		final var value = new BigDecimal("123.45");
		when(mockScbIntegration.getKPI(KPIBaseYear.KPI_2020, altPeriod)).thenReturn(value);

		// Act
		final var result = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("baseYear", KPIBaseYear.KPI_2020.name())
				.queryParam("period", altPeriod.toString())
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ScbKpiResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getBaseYear()).isEqualTo(KPIBaseYear.KPI_2020);
		assertThat(result.getPeriod()).isEqualTo(altPeriod);
		assertThat(result.getValue()).isEqualByComparingTo(value);

		verify(mockScbIntegration).getKPI(KPIBaseYear.KPI_2020, altPeriod);
		verifyNoMoreInteractions(mockScbIntegration);
	}
}
