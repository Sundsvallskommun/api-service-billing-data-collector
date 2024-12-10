package se.sundsvall.billingdatacollector.integration.opene;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@UnitTest
@SpringBootTest(classes = Application.class)
class OpenEIntegrationPropertiesTest {

	@Autowired
	private OpenEIntegrationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.baseUrl()).isEqualTo("http://open-e.nosuchhost.com");
		assertThat(properties.username()).isEqualTo("user");
		assertThat(properties.password()).isEqualTo("p4ssw0rd");
		assertThat(properties.connectTimeout()).isEqualTo(12);
		assertThat(properties.readTimeout()).isEqualTo(34);
		assertThat(properties.kundfakturaFormularFamilyId()).isEqualTo("198");
	}
}
