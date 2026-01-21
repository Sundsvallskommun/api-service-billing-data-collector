package se.sundsvall.billingdatacollector.integration.contract;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.billingdatacollector.integration.contract.ContractConfiguration.CLIENT_ID;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import se.sundsvall.billingdatacollector.integration.Oauth2;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@ExtendWith(MockitoExtension.class)
class ContractConfigurationTest {

	@Mock
	private ClientRegistration mockClientRegistration;

	@Spy
	private FeignMultiCustomizer spyFeignMultiCustomizer;

	@Mock
	private FeignBuilderCustomizer mockFeignBuilderCustomizer;

	@Mock
	private ContractProperties mockProperties;
	@Mock
	private Oauth2 mockOauth2;

	@Mock
	private ClientRegistration.Builder mockClientRegistrationBuilder;

	@Test
	void testFeignBuilderCustomizer() {
		final var tokenUrl = "someTokenUrl";
		final var clientId = "someClientId";
		final var clientSecret = "someClientSecret";
		final var authorizationGrantType = "client_credentials";

		final var configuration = new ContractConfiguration();

		when(mockProperties.oauth2()).thenReturn(mockOauth2);
		when(mockOauth2.tokenUrl()).thenReturn(tokenUrl);
		when(mockOauth2.clientId()).thenReturn(clientId);
		when(mockOauth2.clientSecret()).thenReturn(clientSecret);
		when(mockOauth2.authorizationGrantType()).thenReturn(authorizationGrantType);
		when(mockProperties.connectTimeout()).thenReturn(54);
		when(mockProperties.readTimeout()).thenReturn(32);
		when(spyFeignMultiCustomizer.composeCustomizersToOne()).thenReturn(mockFeignBuilderCustomizer);

		try (var mockFeignMultiCustomizer = mockStatic(FeignMultiCustomizer.class);
			var mockStaticClientRegistration = mockStatic(ClientRegistration.class)) {
			mockFeignMultiCustomizer.when(FeignMultiCustomizer::create).thenReturn(spyFeignMultiCustomizer);

			mockStaticClientRegistration.when(() -> ClientRegistration.withRegistrationId(CLIENT_ID))
				.thenReturn(mockClientRegistrationBuilder);

			when(mockClientRegistrationBuilder.tokenUri(tokenUrl)).thenReturn(mockClientRegistrationBuilder);
			when(mockClientRegistrationBuilder.clientId(clientId)).thenReturn(mockClientRegistrationBuilder);
			when(mockClientRegistrationBuilder.clientSecret(clientSecret)).thenReturn(mockClientRegistrationBuilder);
			when(mockClientRegistrationBuilder.authorizationGrantType(new AuthorizationGrantType(authorizationGrantType)))
				.thenReturn(mockClientRegistrationBuilder);
			when(mockClientRegistrationBuilder.build()).thenReturn(mockClientRegistration);

			final var customizer = configuration.feignBuilderCustomizer(mockProperties);

			final var errorDecoderCaptor = ArgumentCaptor.forClass(ProblemErrorDecoder.class);

			verify(mockProperties, times(4)).oauth2();
			verify(mockOauth2).tokenUrl();
			verify(mockOauth2).clientId();
			verify(mockOauth2).clientSecret();
			verify(mockOauth2).authorizationGrantType();
			verify(mockProperties).connectTimeout();
			verify(mockProperties).readTimeout();
			verify(spyFeignMultiCustomizer).withErrorDecoder(errorDecoderCaptor.capture());
			verify(spyFeignMultiCustomizer).withRetryableOAuth2InterceptorForClientRegistration(same(mockClientRegistration));
			verify(spyFeignMultiCustomizer).withRequestTimeoutsInSeconds(54, 32);
			verify(spyFeignMultiCustomizer).composeCustomizersToOne();

			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("bypassResponseCodes", List.of(NOT_FOUND.value()));
			assertThat(customizer).isSameAs(mockFeignBuilderCustomizer);
		}
	}
}
