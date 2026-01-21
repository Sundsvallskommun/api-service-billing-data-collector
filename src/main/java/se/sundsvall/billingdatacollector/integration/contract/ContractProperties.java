package se.sundsvall.billingdatacollector.integration.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import se.sundsvall.billingdatacollector.integration.Oauth2;

@Validated
@ConfigurationProperties(prefix = "integration.contract")
record ContractProperties(
	@NotBlank String baseUrl,
	@DefaultValue("5") int connectTimeout,
	@DefaultValue("15") int readTimeout,
	@Valid @NotNull Oauth2 oauth2) {
}
