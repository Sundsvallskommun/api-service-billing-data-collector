package se.sundsvall.billingdatacollector.integration.messaging;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@Import(FeignConfiguration.class)
@EnableConfigurationProperties(MessagingIntegrationProperties.class)
class MessagingIntegrationConfiguration {

    static final String CLIENT_ID = "messaging";

    @Bean
    FeignBuilderCustomizer feignBuilderCustomizer(final MessagingIntegrationProperties properties) {
        return FeignMultiCustomizer.create()
            .withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value())))
            .withRetryableOAuth2InterceptorForClientRegistration(ClientRegistration
                .withRegistrationId(CLIENT_ID)
                .tokenUri(properties.oauth2().tokenUrl())
                .clientId(properties.oauth2().clientId())
                .clientSecret(properties.oauth2().clientSecret())
                .authorizationGrantType(new AuthorizationGrantType(properties.oauth2().authorizationGrantType()))
                .build())
            .withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
            .composeCustomizersToOne();
    }
}
