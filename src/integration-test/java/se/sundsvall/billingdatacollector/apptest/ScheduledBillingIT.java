package se.sundsvall.billingdatacollector.apptest;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/ScheduledBillingIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class ScheduledBillingIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String SERVICE_PATH = "/" + MUNICIPALITY_ID + "/scheduled-billing";
	private static final String EXISTING_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
	private static final String DELETE_ID = "d3e4f5a6-b7c8-9012-def0-123456789abc";
	private static final String NON_EXISTING_ID = "00000000-0000-0000-0000-000000000000";

	@Test
	void test1_createScheduledBilling() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(POST)
			.withRequest("request.json")
			.withExpectedResponseStatus(CREATED)
			.withExpectedResponseHeader("Location", List.of("[/]" + MUNICIPALITY_ID + "/scheduled-billing/[a-f0-9-]+"))
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test2_createScheduledBilling_duplicateReturns400() {
		setupCall()
			.withServicePath(SERVICE_PATH)
			.withHttpMethod(POST)
			.withRequest("request.json")
			.withExpectedResponseStatus(BAD_REQUEST)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test3_getAllScheduledBillings() {
		setupCall()
			.withServicePath(SERVICE_PATH + "?page=0&size=10")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test4_getScheduledBillingById() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/" + EXISTING_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test5_getScheduledBillingById_notFound() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/" + NON_EXISTING_ID)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test6_updateScheduledBilling() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/" + EXISTING_ID)
			.withHttpMethod(PUT)
			.withRequest("request.json")
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test7_updateScheduledBilling_notFound() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/" + NON_EXISTING_ID)
			.withHttpMethod(PUT)
			.withRequest("request.json")
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test8_deleteScheduledBilling() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/" + DELETE_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NO_CONTENT)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test9_deleteScheduledBilling_notFound() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/" + NON_EXISTING_ID)
			.withHttpMethod(DELETE)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test10_getScheduledBillingByExternalId() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/external/CONTRACT/external-id-for-get-test")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test11_getScheduledBillingByExternalId_notFound() {
		setupCall()
			.withServicePath(SERVICE_PATH + "/external/CONTRACT/non-existing-external-id")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse("response.json")
			.sendRequestAndVerifyResponse();
	}
}
