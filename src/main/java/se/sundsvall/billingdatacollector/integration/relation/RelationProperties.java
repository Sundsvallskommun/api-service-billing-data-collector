package se.sundsvall.billingdatacollector.integration.relation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import se.sundsvall.billingdatacollector.integration.Oauth2;

@ConfigurationProperties("integration.relation")
public record RelationProperties(

	@NotBlank String baseUrl,

	@DefaultValue("5") int connectTimeout,

	@DefaultValue("30") int readTimeout,

	@Valid @NotNull Oauth2 oauth2) {
}
