package se.sundsvall.billingdatacollector.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.ThrowableProblem;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.service.CollectorService;
import se.sundsvall.billingdatacollector.service.ScheduledBillingService;
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
	private CollectorService mockCollectorService;

	@MockitoBean
	private ScheduledBillingService mockScheduledBillingService;

	private static final LocalDate START_DATE = LocalDate.of(2023, 4, 25);
	private static final LocalDate END_DATE = LocalDate.of(2024, 4, 25);
	private static final Set<String> FAMILY_IDS = new HashSet<>(Arrays.asList("456", "789"));
	private static final List<String> PROCESSED_FLOW_INSTANCE_IDS = List.of("123", "321");

	@Test
	void testTriggerBilling() {
		// Arrange
		final var flowInstanceId = "123";
		doNothing().when(mockCollectorService).triggerBilling(flowInstanceId);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH_FLOW_INSTANCE_ID)
				.build(MUNICIPALITY_ID, flowInstanceId))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockCollectorService).triggerBilling(flowInstanceId);
		verifyNoMoreInteractions(mockCollectorService);
	}

	@Test
	void testTriggerBillingBetweenTwoValidDates() {
		// Arrange
		when(mockCollectorService.triggerBillingBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull())).thenReturn(PROCESSED_FLOW_INSTANCE_IDS);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", END_DATE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockCollectorService).triggerBillingBetweenDates(START_DATE, END_DATE, null);
		verifyNoMoreInteractions(mockCollectorService);
	}

	@Test
	void testTriggerBillingBetweenTwoEqualDates() {
		// Arrange
		when(mockCollectorService.triggerBillingBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull())).thenReturn(PROCESSED_FLOW_INSTANCE_IDS);

		// Act
		webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", START_DATE)
				.queryParam("endDate", START_DATE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isAccepted();

		// Assert
		verify(mockCollectorService).triggerBillingBetweenDates(START_DATE, START_DATE, null);
		verifyNoMoreInteractions(mockCollectorService);
	}

	@Test
	void testTriggerBillingWithDatesAndFamilyIds() {
		// Arrange
		when(mockCollectorService.triggerBillingBetweenDates(Mockito.any(LocalDate.class), Mockito.any(LocalDate.class), isNull())).thenReturn(PROCESSED_FLOW_INSTANCE_IDS);

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
		verify(mockCollectorService).triggerBillingBetweenDates(START_DATE, END_DATE, FAMILY_IDS);
		verifyNoMoreInteractions(mockCollectorService);
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

		verifyNoInteractions(mockCollectorService);
		verifyNoMoreInteractions(mockCollectorService);
	}

	@Test
	void testTriggerBetweenFaultyDateInterval_shouldThrowException() {
		// Arrange & Act
		final var responseBody = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("startDate", END_DATE)    // End date as start date and vice versa
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

		verifyNoInteractions(mockCollectorService);
		verifyNoMoreInteractions(mockCollectorService);
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

		verifyNoInteractions(mockCollectorService);
		verifyNoMoreInteractions(mockCollectorService);
	}

	// ========== Scheduled Billing Tests ==========

	@Test
	void testAddScheduledBilling() {
		// Arrange
		var request = createScheduledBillingRequest();
		var response = createScheduledBillingResponse();

		when(mockScheduledBillingService.create(MUNICIPALITY_ID, request)).thenReturn(response);

		// Act
		var result = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing").build(MUNICIPALITY_ID))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isCreated()
			.expectHeader().location("/" + MUNICIPALITY_ID + "/scheduled-billing/" + response.getId())
			.expectBody(ScheduledBilling.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isEqualTo(response);

		verify(mockScheduledBillingService).create(MUNICIPALITY_ID, request);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void testAddScheduledBilling_invalidRequest_shouldThrowBadRequest() {
		// Arrange - missing required fields
		var request = ScheduledBilling.builder().build();

		// Act
		var result = webTestClient.post()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing").build(MUNICIPALITY_ID))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getTitle()).isEqualTo("Constraint Violation");
		assertThat(result.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(result.getViolations())
			.extracting(Violation::getField, Violation::getMessage)
			.containsExactlyInAnyOrder(
				tuple("externalId", "must not be blank"),
				tuple("source", "must not be null"),
				tuple("billingDaysOfMonth", "must not be null"),
				tuple("billingDaysOfMonth", "must not be empty"),
				tuple("billingMonths", "must not be null"),
				tuple("billingMonths", "must not be empty"));

		verifyNoInteractions(mockScheduledBillingService);
	}

	@Test
	void testUpdateScheduledBilling() {
		// Arrange
		var id = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
		var request = createScheduledBillingRequest();
		var response = createScheduledBillingResponse();

		when(mockScheduledBillingService.update(MUNICIPALITY_ID, id, request)).thenReturn(response);

		// Act
		var result = webTestClient.put()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing/{id}").build(MUNICIPALITY_ID, id))
			.contentType(APPLICATION_JSON)
			.bodyValue(request)
			.exchange()
			.expectStatus().isOk()
			.expectBody(ScheduledBilling.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isEqualTo(response);

		verify(mockScheduledBillingService).update(MUNICIPALITY_ID, id, request);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void testGetAllScheduledBillings() {
		// Arrange
		var response = createScheduledBillingResponse();
		var page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);

		when(mockScheduledBillingService.getAll(eq(MUNICIPALITY_ID), any(Pageable.class))).thenReturn(page);

		// Act
		webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing")
				.queryParam("page", 0)
				.queryParam("size", 20)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$.content").isArray()
			.jsonPath("$.content.length()").isEqualTo(1)
			.jsonPath("$.content[0].id").isEqualTo(response.getId())
			.jsonPath("$.content[0].externalId").isEqualTo(response.getExternalId())
			.jsonPath("$.totalElements").isEqualTo(1)
			.jsonPath("$.totalPages").isEqualTo(1);

		// Assert
		var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(mockScheduledBillingService).getAll(eq(MUNICIPALITY_ID), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
		assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void testGetScheduledBillingById() {
		// Arrange
		var id = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";
		var response = createScheduledBillingResponse();

		when(mockScheduledBillingService.getById(MUNICIPALITY_ID, id)).thenReturn(response);

		// Act
		var result = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing/{id}").build(MUNICIPALITY_ID, id))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ScheduledBilling.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isEqualTo(response);

		verify(mockScheduledBillingService).getById(MUNICIPALITY_ID, id);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void testDeleteScheduledBilling() {
		// Arrange
		var id = "f0882f1d-06bc-47fd-b017-1d8307f5ce95";

		doNothing().when(mockScheduledBillingService).delete(MUNICIPALITY_ID, id);

		// Act
		webTestClient.delete()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing/{id}").build(MUNICIPALITY_ID, id))
			.exchange()
			.expectStatus().isNoContent();

		// Assert
		verify(mockScheduledBillingService).delete(MUNICIPALITY_ID, id);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	@Test
	void testGetScheduledBillingByExternalId() {
		// Arrange
		var externalId = "66c57446-72e7-4cc5-af7c-053919ce904b";
		var source = BillingSource.CONTRACT;
		var response = createScheduledBillingResponse();

		when(mockScheduledBillingService.getByExternalId(MUNICIPALITY_ID, source, externalId)).thenReturn(response);

		// Act
		var result = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/{municipalityId}/scheduled-billing/external/{source}/{externalId}")
				.build(MUNICIPALITY_ID, source, externalId))
			.exchange()
			.expectStatus().isOk()
			.expectBody(ScheduledBilling.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isEqualTo(response);

		verify(mockScheduledBillingService).getByExternalId(MUNICIPALITY_ID, source, externalId);
		verifyNoMoreInteractions(mockScheduledBillingService);
	}

	private ScheduledBilling createScheduledBillingRequest() {
		return ScheduledBilling.builder()
			.withExternalId("66c57446-72e7-4cc5-af7c-053919ce904b")
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1, 15))
			.withBillingMonths(Set.of(1, 4, 7, 10))
			.withPaused(false)
			.build();
	}

	private ScheduledBilling createScheduledBillingResponse() {
		return ScheduledBilling.builder()
			.withId("f0882f1d-06bc-47fd-b017-1d8307f5ce95")
			.withExternalId("66c57446-72e7-4cc5-af7c-053919ce904b")
			.withSource(BillingSource.CONTRACT)
			.withBillingDaysOfMonth(Set.of(1, 15))
			.withBillingMonths(Set.of(1, 4, 7, 10))
			.withNextScheduledBilling(LocalDate.now().plusDays(10))
			.withPaused(false)
			.build();
	}
}
