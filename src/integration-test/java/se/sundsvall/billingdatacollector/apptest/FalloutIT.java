package se.sundsvall.billingdatacollector.apptest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.service.scheduling.fallout.FalloutJobHandler;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/FalloutIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class FalloutIT extends AbstractAppTest {

	private static final String FAMILY_ID = "358";
	private static final String FLOW_INSTANCE_ID = "185376";

	@BeforeEach
	void setup() {
		CommonStubs.stubAccessToken();
	}

	@Autowired
	private FalloutJobHandler falloutJobHandler;

	@Autowired
	private FalloutRepository falloutRepository;

	@Test
	void test1_sendFalloutEmail() {
		// Setup wiremock
		setupCall();

		// Trigger the "scheduled" job
		falloutJobHandler.handleFallout();

		// And check that we have a "reported" fallout, not verifying mapping, only that we have the correct one.
		var fallouts = falloutRepository.findAll();
		assertThat(fallouts.size()).isOne();
		assertThat(fallouts.getFirst().getFamilyId()).isEqualTo(FAMILY_ID);
		assertThat(fallouts.getFirst().getFlowInstanceId()).isEqualTo(FLOW_INSTANCE_ID);
		assertThat(fallouts.getFirst().isReported()).isTrue();
	}
}
