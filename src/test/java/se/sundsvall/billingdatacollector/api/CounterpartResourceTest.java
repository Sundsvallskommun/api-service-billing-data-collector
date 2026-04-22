package se.sundsvall.billingdatacollector.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.service.source.contract.CounterpartMappingService;
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
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class CounterpartResourceTest {

	private static final String PATH = "/{municipalityId}/counterpart";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String PARTY_ID = "fb2f0290-3820-11ed-a261-0242ac120002";
	private static final String STAKEHOLDER_TYPE = "PERSON";

	@Autowired
	private WebTestClient webTestClient;

	@MockitoBean
	private CounterpartMappingService mockCounterpartMappingService;

	@Test
	void testGetCounterpart() {
		// Arrange
		final var counterpart = "123";
		when(mockCounterpartMappingService.findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE)).thenReturn(counterpart);

		// Act
		final var result = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("partyId", PARTY_ID)
				.queryParam("stakeholderType", STAKEHOLDER_TYPE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isEqualTo(counterpart);

		verify(mockCounterpartMappingService).findCounterpart(MUNICIPALITY_ID, PARTY_ID, STAKEHOLDER_TYPE);
		verifyNoMoreInteractions(mockCounterpartMappingService);
	}

	@Test
	void testGetCounterpartWithInvalidMunicipalityId() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("partyId", PARTY_ID)
				.queryParam("stakeholderType", STAKEHOLDER_TYPE)
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
			.containsExactlyInAnyOrder(tuple("getCounterpart.municipalityId", "not a valid municipality ID"));

		verifyNoInteractions(mockCounterpartMappingService);
	}

	@Test
	void testGetCounterpartWithInvalidPartyId() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("partyId", "invalid")
				.queryParam("stakeholderType", STAKEHOLDER_TYPE)
				.build(MUNICIPALITY_ID))
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
			.containsExactlyInAnyOrder(tuple("getCounterpart.partyId", "not a valid UUID"));

		verifyNoInteractions(mockCounterpartMappingService);
	}

	@Test
	void testGetCounterpartWithInvalidStakeholderType() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("partyId", PARTY_ID)
				.queryParam("stakeholderType", "INVALID_TYPE")
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(Problem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getTitle()).isEqualTo("Invalid stakeholder type");
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getDetail()).contains("PERSON", "ORGANIZATION", "ASSOCIATION", "MUNICIPALITY", "REGION", "OTHER");

		verifyNoInteractions(mockCounterpartMappingService);
	}

	@Test
	void testGetCounterpartWithOnlyPartyId() {
		// Arrange
		final var counterpart = "123";
		when(mockCounterpartMappingService.findCounterpart(MUNICIPALITY_ID, PARTY_ID, null)).thenReturn(counterpart);

		// Act
		final var result = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("partyId", PARTY_ID)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(result).isEqualTo(counterpart);

		verify(mockCounterpartMappingService).findCounterpart(MUNICIPALITY_ID, PARTY_ID, null);
		verifyNoMoreInteractions(mockCounterpartMappingService);
	}

	@Test
	void testGetCounterpartWithOnlyStakeholderType() {
		// Arrange
		final var counterpart = "456";
		when(mockCounterpartMappingService.findCounterpart(MUNICIPALITY_ID, null, STAKEHOLDER_TYPE)).thenReturn(counterpart);

		// Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
				.queryParam("stakeholderType", STAKEHOLDER_TYPE)
				.build(MUNICIPALITY_ID))
			.exchange()
			.expectStatus().isBadRequest()
			.expectHeader().contentType(APPLICATION_PROBLEM_JSON)
			.expectBody(ConstraintViolationProblem.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(responseBody).isNotNull();
		assertThat(responseBody.getStatus()).isEqualTo(BAD_REQUEST);
		assertThat(responseBody.getDetail()).isEqualTo("Required parameter 'partyId' is not present.");
		verifyNoMoreInteractions(mockCounterpartMappingService);
	}

	@Test
	void testGetCounterpartWithNoParameters() {
		// Arrange & Act
		final var responseBody = webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path(PATH)
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
		assertThat(responseBody.getDetail()).isEqualTo("Required parameter 'partyId' is not present.");
		verifyNoInteractions(mockCounterpartMappingService);
	}
}
