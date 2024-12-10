package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.billingdatacollector.integration.Oauth2;

@Validated
@ConfigurationProperties(prefix = "integration.billing-preprocessor")
record BillingPreprocessorIntegrationProperties(

	@NotBlank String baseUrl,

	@Valid @NotNull Oauth2 oauth2,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout) {}
