package se.sundsvall.billingdatacollector.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.service.CollectorService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectorResourceTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private CollectorService mockService;

	@Test
	void testTriggerBilling() {
		//Arrange
		var flowInstanceId = "123";
		doNothing().when(mockService).trigger(flowInstanceId);

		//Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger/{flowInstanceId}")
				.build(flowInstanceId))
			.exchange()
			.expectStatus().isAccepted();

		//Assert
		verify(mockService).trigger(flowInstanceId);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingBetweenTwoValidDates() {
		//Arrange
		doNothing().when(mockService).triggerBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class));

		var startDate = LocalDate.of(2023, 4, 25);
		var endDate = LocalDate.of(2024, 4, 25);

		//Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", startDate.toString())
				.queryParam("endDate", endDate.toString())
				.build())
			.exchange()
			.expectStatus().isAccepted();

		//Assert
		verify(mockService).triggerBetweenDates(startDate, endDate);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingBetweenTwoEqualDates() {
		//Arrange
		doNothing().when(mockService).triggerBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class));

		var date = LocalDate.of(2024, 5, 17);

		//Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", date.toString())
				.queryParam("endDate", date.toString())
				.build())
			.exchange()
			.expectStatus().isAccepted();

		//Assert
		verify(mockService).triggerBetweenDates(date, date);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBetweenFaultyDateInterval_shouldThrowException() {
		//Arrange & Act
		var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", LocalDate.of(2025, 4, 25).toString())
				.queryParam("endDate", LocalDate.of(2024, 4, 24).toString())
				.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ThrowableProblem.class)
			.returnResult()
			.getResponseBody();

		//Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getTitle()).isEqualTo("Invalid date range");
		assertThat(responseBody.getDetail()).isEqualTo("Start date must be before end date");

		verifyNoInteractions(mockService);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingWithInvalidDate_shouldThrowBadRequest() {
		//Arrange & Act
		var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", LocalDate.of(2025, 4, 25).toString())
				.queryParam("endDate", "2024-13-01")
				.build())
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ThrowableProblem.class)
			.returnResult()
			.getResponseBody();

		//Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getTitle()).isEqualTo("Bad Request");

		verifyNoInteractions(mockService);
		verifyNoMoreInteractions(mockService);
	}
}
