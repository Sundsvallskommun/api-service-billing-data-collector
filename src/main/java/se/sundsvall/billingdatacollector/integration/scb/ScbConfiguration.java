package se.sundsvall.billingdatacollector.integration.scb;

import feign.Retryer;
import feign.Retryer.Default;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import se.sundsvall.dept44.configuration.feign.FeignConfiguration;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Import(FeignConfiguration.class)
@EnableConfigurationProperties(ScbProperties.class)
class ScbConfiguration {

	static final String CLIENT_ID = "SCB";

	@Bean
	FeignBuilderCustomizer feignBuilderCustomizer(final ScbProperties properties) {
		return FeignMultiCustomizer.create()
			.withErrorDecoder(new ProblemErrorDecoder(CLIENT_ID, List.of(NOT_FOUND.value())))
			.withRequestTimeoutsInSeconds(properties.connectTimeout(), properties.readTimeout())
			.composeCustomizersToOne();
	}

	@Bean
	Retryer retryer() {
		return new Default(SECONDS.toMillis(1), SECONDS.toMillis(5), 3);
	}
}
