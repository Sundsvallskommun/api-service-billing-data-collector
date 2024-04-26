package se.sundsvall.billingdatacollector.integration.messaging;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import se.sundsvall.billingdatacollector.integration.Oauth2;

@Validated
@ConfigurationProperties(prefix = "integration.messaging")
record MessagingIntegrationProperties(

        @NotBlank
        String baseUrl,

        @Valid
        @NotNull
        EmailProperties email,

        @Valid
        @NotNull
        SlackProperties slack,

        @DefaultValue("5")
        int connectTimeout,

        @DefaultValue("30")
        int readTimeout,

        @Valid
        @NotNull
        Oauth2 oauth2) {

    record EmailProperties(

            @NotBlank
            String subject,

            @NotEmpty
            List<@NotBlank String> recipients,

            @Valid
            Sender sender) {

        record Sender(

            @NotBlank
            String name,

            @NotBlank
            String emailAddress) {}
    }

    record SlackProperties(

        @NotBlank
        String token,

        @NotEmpty
        List<@NotBlank String> channels) { }
}