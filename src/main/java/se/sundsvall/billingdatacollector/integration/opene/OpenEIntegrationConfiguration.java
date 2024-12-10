package se.sundsvall.billingdatacollector.integration.opene;

import feign.auth.BasicAuthRequestInterceptor;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@Import(FeignConfiguration.class)
class OpenEIntegrationConfiguration {

	static final String CLIENT_ID = "open-e";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final OpenEIntegrationProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.withRequestInterceptor(new BasicAuthRequestInterceptor(properties.username(), properties.password()))
			.composeCustomizersToOne();
	}
}
