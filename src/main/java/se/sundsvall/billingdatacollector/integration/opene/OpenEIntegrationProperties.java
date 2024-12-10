package se.sundsvall.billingdatacollector.integration.opene;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "integration.open-e")
public record OpenEIntegrationProperties(

	@NotBlank String baseUrl,

	@NotBlank String username,

	@NotBlank String password,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout,

	@NotBlank String kundfakturaFormularFamilyId) {}
