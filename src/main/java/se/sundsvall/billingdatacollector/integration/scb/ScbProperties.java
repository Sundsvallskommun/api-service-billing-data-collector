package se.sundsvall.billingdatacollector.integration.scb;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "integration.scb")
record ScbProperties(
	@NotBlank String baseUrl,
	@DefaultValue("5") int connectTimeout,
	@DefaultValue("15") int readTimeout) {
}
