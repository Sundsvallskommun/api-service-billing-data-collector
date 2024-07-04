package se.sundsvall.billingdatacollector.integration.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import se.sundsvall.billingdatacollector.integration.Oauth2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "integration.messaging")
record MessagingIntegrationProperties(

        @NotBlank
        String baseUrl,

        @DefaultValue("5")
        int connectTimeout,

        @DefaultValue("30")
        int readTimeout,

        @Valid
        @NotNull
        Oauth2 oauth2) {
    }

