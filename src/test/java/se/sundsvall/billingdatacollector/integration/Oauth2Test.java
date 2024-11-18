package se.sundsvall.billingdatacollector.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Oauth2Test {

	@Test
	void constructorAndAccessors() {
		var tokenUrl = "tokenUrl";
		var clientId = "clientId";
		var clientSecret = "clientSecret";
		var authorizationGrantType = "authorizationGrantType";

		var oauth2 = new Oauth2(tokenUrl, clientId, clientSecret, authorizationGrantType);

		assertThat(oauth2.tokenUrl()).isEqualTo(tokenUrl);
		assertThat(oauth2.clientId()).isEqualTo(clientId);
		assertThat(oauth2.clientSecret()).isEqualTo(clientSecret);
		assertThat(oauth2.authorizationGrantType()).isEqualTo(authorizationGrantType);
	}
}
