package se.sundsvall.billingdatacollector.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@UnitTest
class CollectorResourceTest {

	private static final String PATH = "/{municipalityId}/trigger";
	private static final String PATH_FLOW_INSTANCE_ID = "/{municipalityId}/trigger/{flowInstanceId}";
	private static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private CollectorService mockService;

	private static final LocalDate START_DATE = LocalDate.of(2023, 4, 25);
	private static final LocalDate END_DATE = LocalDate.of(2024, 4, 25);
	private static final Set<String> FAMILY_IDS = new HashSet<>(Arrays.asList("456", "789"));
	private static final List<String> PROCESSED_FAMILY_IDS = List.of("456", "789");

	@Test
	void testTriggerBilling() {
		// Arrange
		final var flowInstanceId = "123";
		doNothing().when(mockService).triggerBilling(flowInstanceId);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH_FLOW_INSTANCE_ID)
				.build(MUNICIPALITY_ID, flowInstanceId))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockService).triggerBilling(flowInstanceId);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingBetweenTwoValidDates() {
		// Arrange
		when(mockService.triggerBillingBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull())).thenReturn(PROCESSED_FAMILY_IDS);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", END_DATE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockService).triggerBillingBetweenDates(START_DATE, END_DATE, null);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingBetweenTwoEqualDates() {
		// Arrange
		when(mockService.triggerBillingBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull())).thenReturn(PROCESSED_FAMILY_IDS);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", START_DATE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockService).triggerBillingBetweenDates(START_DATE, START_DATE, null);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingWithDatesAndFamilyIds() {
		// Arrange
		when(mockService.triggerBillingBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull())).thenReturn(PROCESSED_FAMILY_IDS);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", END_DATE)
				.queryParam("familyIds", List.of(FAMILY_IDS.toArray()))
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockService).triggerBillingBetweenDates(START_DATE, END_DATE, FAMILY_IDS);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerWithInvalidMunicipalityId_shouldThrowException() {
		// Arrange & Act
		final var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", END_DATE)
				.build("invalid"))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getTitle()).isEqualTo("Constraint Violation");
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(tuple("triggerBilling.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(mockService);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBetweenFaultyDateInterval_shouldThrowException() {
		// Arrange & Act
		final var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", END_DATE)	// End date as start date and vice versa
				.queryParam("endDate", START_DATE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ThrowableProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getTitle()).isEqualTo("Invalid date range");
		assertThat(responseBody.getDetail()).isEqualTo("Start date must be before end date");

		verifyNoInteractions(mockService);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingWithInvalidDate_shouldThrowBadRequest() {
		// Arrange & Act
		final var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", "2024-13-01")
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ThrowableProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getTitle()).isEqualTo("Bad Request");

		verifyNoInteractions(mockService);
		verifyNoMoreInteractions(mockService);
	}
}
