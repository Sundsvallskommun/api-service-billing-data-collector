package se.sundsvall.billingdatacollector.integration.party;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.billingdatacollector.integration.party.PartyIntegrationConfiguration.CLIENT_ID;

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
class PartyIntegrationConfigurationTests {

    @Mock
    private ClientRegistration mockClientRegistration;

    @Spy
    private FeignMultiCustomizer feignMultiCustomizerSpy;

    @Mock
    private FeignBuilderCustomizer mockFeignBuilderCustomizer;

    @Mock
    private PartyIntegrationProperties mockProperties;
    @Mock
    private Oauth2 mockOauth2;

    @Mock
    private ClientRegistration.Builder mockClientRegistrationBuilder;

    @Test
    void testFeignBuilderCustomizer() {
        var tokenUrl = "someTokenUrl";
        var clientId = "someClientId";
        var clientSecret = "someClientSecret";
        var authorizationGrantType = "client_credentials";

        var configuration = new PartyIntegrationConfiguration();

        when(mockProperties.oauth2()).thenReturn(mockOauth2);
        when(mockOauth2.tokenUrl()).thenReturn(tokenUrl);
        when(mockOauth2.clientId()).thenReturn(clientId);
        when(mockOauth2.clientSecret()).thenReturn(clientSecret);
        when(mockOauth2.authorizationGrantType()).thenReturn(authorizationGrantType);
        when(mockProperties.connectTimeout()).thenReturn(12);
        when(mockProperties.readTimeout()).thenReturn(34);
        when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(mockFeignBuilderCustomizer);

        try (var mockFeignMultiCustomizer = mockStatic(FeignMultiCustomizer.class);
                var mockStaticClientRegistration = mockStatic(ClientRegistration.class)) {
            mockFeignMultiCustomizer.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

            mockStaticClientRegistration.when(() -> ClientRegistration.withRegistrationId(CLIENT_ID))
                .thenReturn(mockClientRegistrationBuilder);

            when(mockClientRegistrationBuilder.tokenUri(tokenUrl)).thenReturn(mockClientRegistrationBuilder);
            when(mockClientRegistrationBuilder.clientId(clientId)).thenReturn(mockClientRegistrationBuilder);
            when(mockClientRegistrationBuilder.clientSecret(clientSecret)).thenReturn(mockClientRegistrationBuilder);
            when(mockClientRegistrationBuilder.authorizationGrantType(new AuthorizationGrantType(authorizationGrantType)))
                .thenReturn(mockClientRegistrationBuilder);
            when(mockClientRegistrationBuilder.build()).thenReturn(mockClientRegistration);

            var customizer = configuration.feignBuilderCustomizer(mockProperties);

            var errorDecoderCaptor = ArgumentCaptor.forClass(ProblemErrorDecoder.class);

            verify(mockProperties, times(4)).oauth2();
            verify(mockOauth2).tokenUrl();
            verify(mockOauth2).clientId();
            verify(mockOauth2).clientSecret();
            verify(mockOauth2).authorizationGrantType();
            verify(mockProperties).connectTimeout();
            verify(mockProperties).readTimeout();
            verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
            verify(feignMultiCustomizerSpy).withRetryableOAuth2InterceptorForClientRegistration(same(mockClientRegistration));
            verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(12, 34);
            verify(feignMultiCustomizerSpy).composeCustomizersToOne();

            assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
            assertThat(customizer).isSameAs(mockFeignBuilderCustomizer);
        }
    }
}
