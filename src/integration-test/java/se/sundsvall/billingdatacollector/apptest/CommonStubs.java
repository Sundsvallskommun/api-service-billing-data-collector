package se.sundsvall.billingdatacollector.apptest;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class CommonStubs {

	public static void stubAccessToken() {
		stubAccessToken("/token");
	}

	public static void stubAccessToken(final String url) {
		stubFor(post(url)
			.willReturn(aResponse()
				.withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.withBody("{\"access_token\":\"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3\",\"not-before-policy\":0,\"session_state\":\"88bbf486\",\"token_type\": \"bearer\"}")));
	}
}
