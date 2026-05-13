package se.sundsvall.billingdatacollector.apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/ScbKpiIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql"
})
class ScbKpiIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String SERVICE_PATH = "/" + MUNICIPALITY_ID + "/scb/kpi";
	private static final String RESPONSE_FILE = "response.json";

	@Test
	void test1_getKpi() {
		setupCall()
			.withServicePath(SERVICE_PATH + "?baseYear=KPI_80&period=2024-10")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}

	@Test
	void test2_getKpiNotFound() {
		setupCall()
			.withServicePath(SERVICE_PATH + "?baseYear=KPI_2020&period=2024-11")
			.withHttpMethod(GET)
			.withExpectedResponseStatus(NOT_FOUND)
			.withExpectedResponse(RESPONSE_FILE)
			.sendRequestAndVerifyResponse();
	}
}
