package se.sundsvall.billingdatacollector.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@UnitTest
class CollectorResourceTest {

	@Autowired
	private WebTestClient webTestClient;

	@MockBean
	private CollectorService mockService;

	private static final LocalDate START_DATE = LocalDate.of(2023, 4, 25);
	private static final LocalDate END_DATE = LocalDate.of(2024, 4, 25);
	private static final Set<String> FAMILY_IDS = new HashSet<>(Arrays.asList("456", "789"));

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
		doNothing().when(mockService).triggerBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull());

		//Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", END_DATE)
				.build())
			.exchange()
			.expectStatus().isAccepted();

		//Assert
		verify(mockService).triggerBetweenDates(START_DATE, END_DATE, null);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingBetweenTwoEqualDates() {
		//Arrange
		doNothing().when(mockService).triggerBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull());

		//Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", START_DATE)
				.build())
			.exchange()
			.expectStatus().isAccepted();

		//Assert
		verify(mockService).triggerBetweenDates(START_DATE, START_DATE, null);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBillingWithDatesAndFamilyIds() {
		//Arrange
		doNothing().when(mockService).triggerBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), anySet());

		//Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", END_DATE)
				.queryParam("familyIds", List.of(FAMILY_IDS.toArray()))
				.build())
			.exchange()
			.expectStatus().isAccepted();

		//Assert
		verify(mockService).triggerBetweenDates(START_DATE, END_DATE, FAMILY_IDS);
		verifyNoMoreInteractions(mockService);
	}

	@Test
	void testTriggerBetweenFaultyDateInterval_shouldThrowException() {
		//Arrange & Act
		var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/trigger")
				.queryParam("startDate", END_DATE)	// End date as start date and vice versa
				.queryParam("endDate", START_DATE)
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
				.queryParam("startDate", START_DATE)
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
