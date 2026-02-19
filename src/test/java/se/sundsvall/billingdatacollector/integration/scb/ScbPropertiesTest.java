package se.sundsvall.billingdatacollector.integration.scb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.billingdatacollector.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class ScbPropertiesTest {

	@Autowired
	private ScbProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.baseUrl()).isEqualTo("http://scb.nosuchhost.com");
		assertThat(properties.connectTimeout()).isEqualTo(29);
		assertThat(properties.readTimeout()).isEqualTo(28);
	}
}
