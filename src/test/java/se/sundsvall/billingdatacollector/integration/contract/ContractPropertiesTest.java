package se.sundsvall.billingdatacollector.integration.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@UnitTest
@SpringBootTest(classes = Application.class)
class ContractPropertiesTest {

	@Autowired
	private ContractProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.baseUrl()).isEqualTo("http://contract.nosuchhost.com");
		assertThat(properties.connectTimeout()).isEqualTo(31);
		assertThat(properties.readTimeout()).isEqualTo(30);
		assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
			assertThat(oauth2.tokenUrl()).isEqualTo("http://token.nosuchhost.com");
			assertThat(oauth2.clientId()).isEqualTo("someClientId");
			assertThat(oauth2.clientSecret()).isEqualTo("someClientSecret");
			assertThat(oauth2.authorizationGrantType()).isEqualTo("client_credentials");
		});
	}
}
