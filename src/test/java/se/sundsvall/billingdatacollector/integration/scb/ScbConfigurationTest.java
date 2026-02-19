package se.sundsvall.billingdatacollector.integration.scb;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import se.sundsvall.billingdatacollector.integration.Oauth2;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.billingdatacollector.integration.scb.ScbConfiguration.CLIENT_ID;

@ExtendWith(MockitoExtension.class)
class ScbConfigurationTest {

	@Mock
	private ClientRegistration mockClientRegistration;

	@Spy
	private FeignMultiCustomizer spyFeignMultiCustomizer;

	@Mock
	private FeignBuilderCustomizer mockFeignBuilderCustomizer;

	@Mock
	private ScbProperties mockProperties;
	@Mock
	private Oauth2 mockOauth2;

	@Mock
	private ClientRegistration.Builder mockClientRegistrationBuilder;

	@Captor
	private ArgumentCaptor<ProblemErrorDecoder> errorDecoderCaptor;

	@Test
	void testFeignBuilderCustomizer() {
		final var configuration = new ScbConfiguration();

		when(mockProperties.connectTimeout()).thenReturn(123);
		when(mockProperties.readTimeout()).thenReturn(456);
		when(spyFeignMultiCustomizer.composeCustomizersToOne()).thenReturn(mockFeignBuilderCustomizer);

		try (var mockFeignMultiCustomizer = mockStatic(FeignMultiCustomizer.class);
			var mockStaticClientRegistration = mockStatic(ClientRegistration.class)) {
			mockFeignMultiCustomizer.when(FeignMultiCustomizer::create).thenReturn(spyFeignMultiCustomizer);

			mockStaticClientRegistration.when(() -> ClientRegistration.withRegistrationId(CLIENT_ID))
				.thenReturn(mockClientRegistrationBuilder);

			final var customizer = configuration.feignBuilderCustomizer(mockProperties);

			verify(mockProperties).connectTimeout();
			verify(mockProperties).readTimeout();
			verify(spyFeignMultiCustomizer).withErrorDecoder(errorDecoderCaptor.capture());
			verify(spyFeignMultiCustomizer).withRequestTimeoutsInSeconds(123, 456);
			verify(spyFeignMultiCustomizer).composeCustomizersToOne();

			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("bypassResponseCodes", List.of(HttpStatus.NOT_FOUND.value()));
			assertThat(customizer).isSameAs(mockFeignBuilderCustomizer);
		}
	}
}
