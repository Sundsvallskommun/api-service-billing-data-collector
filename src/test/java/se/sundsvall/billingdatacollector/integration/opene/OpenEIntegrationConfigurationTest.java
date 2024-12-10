package se.sundsvall.billingdatacollector.integration.opene;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationConfiguration.CLIENT_ID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import se.sundsvall.dept44.configuration.feign.FeignMultiCustomizer;
import se.sundsvall.dept44.configuration.feign.decoder.ProblemErrorDecoder;

@ExtendWith(MockitoExtension.class)
class OpenEIntegrationConfigurationTest {

	@Spy
	private FeignMultiCustomizer feignMultiCustomizerSpy;

	@Mock
	private FeignBuilderCustomizer mockFeignBuilderCustomizer;

	@Mock
	private OpenEIntegrationProperties mockProperties;

	@Test
	void testFeignBuilderCustomizer() {
		when(mockProperties.username()).thenReturn("someUsername");
		when(mockProperties.password()).thenReturn("somePassword");
		when(mockProperties.connectTimeout()).thenReturn(12);
		when(mockProperties.readTimeout()).thenReturn(34);
		when(feignMultiCustomizerSpy.composeCustomizersToOne()).thenReturn(mockFeignBuilderCustomizer);

		try (var mockFeignMultiCustomizer = mockStatic(FeignMultiCustomizer.class)) {
			mockFeignMultiCustomizer.when(FeignMultiCustomizer::create).thenReturn(feignMultiCustomizerSpy);

			var customizer = new OpenEIntegrationConfiguration().feignBuilderCustomizer(mockProperties);
			var errorDecoderCaptor = ArgumentCaptor.forClass(ProblemErrorDecoder.class);

			verify(feignMultiCustomizerSpy).withErrorDecoder(errorDecoderCaptor.capture());
			verify(mockProperties).username();
			verify(mockProperties).password();
			verify(mockProperties).connectTimeout();
			verify(mockProperties).readTimeout();
			verify(feignMultiCustomizerSpy).withRequestTimeoutsInSeconds(12, 34);
			verify(feignMultiCustomizerSpy).composeCustomizersToOne();

			assertThat(errorDecoderCaptor.getValue()).hasFieldOrPropertyWithValue("integrationName", CLIENT_ID);
			assertThat(customizer).isSameAs(mockFeignBuilderCustomizer);
		}
	}
}
