package se.sundsvall.billingdatacollector.apptest;

import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;

@WireMockAppTestSuite(files = "classpath:/BillingRecordsIT/", classes = Application.class)
@Sql({
	"/db/truncate.sql",
	"/db/testdata-it.sql"
})
public class BillingCollectorAPIIT {
}
