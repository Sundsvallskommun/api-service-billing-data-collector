package se.sundsvall.billingdatacollector.apptest;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/CounterpartIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class CounterpartIT extends AbstractAppTest {

	private static final String MUNICIPALITY_ID = "2281";
	private static final String SERVICE_PATH = "/" + MUNICIPALITY_ID + "/counterpart";
	private static final String PARTY_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
	private static final String STAKEHOLDER_TYPE = "PERSON";

	@Test
	void test1_getCounterpart() {
		setupCall()
			.withServicePath(SERVICE_PATH + "?partyId=" + PARTY_ID + "&stakeholderType=" + STAKEHOLDER_TYPE)
			.withHttpMethod(GET)
			.withExpectedResponseStatus(OK)
			.withExpectedResponse("123")
			.withExpectedResponseHeader("Content-Type", List.of("text/plain;charset=UTF-8"))
			.sendRequestAndVerifyResponse();
	}
}
