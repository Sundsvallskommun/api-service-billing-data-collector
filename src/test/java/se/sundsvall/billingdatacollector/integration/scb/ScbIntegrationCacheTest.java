package se.sundsvall.billingdatacollector.integration.scb;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.billingdatacollector.integration.scb.model.KPIBaseYear.KPI_80;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
class ScbIntegrationCacheTest {

	private static final BigDecimal KPI_VALUE = BigDecimal.TEN;

	private ScbIntegration scbIntegrationMock;

	@Autowired
	private ScbIntegration scbIntegration;

	// Provides a mock implementation for the integration and a cache manager
	@EnableCaching
	@Configuration
	public static class CachingTestConfig {

		@Bean
		ScbIntegration scbintegrationMockImplementation() {
			return mock(ScbIntegration.class);
		}

		@Bean
		CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("kpiData");
		}
	}

	@InjectMocks
	private ScbIntegration integration;

	@BeforeEach
	void setUp() {
		// ScbIntegration is a proxy around our mock. So, in order to use Mockito validations, we retrieve the actual mock
		// via AopTestUtils.getTargetObject
		scbIntegrationMock = AopTestUtils.getTargetObject(scbIntegration);

		// reset(mock) is called between each test because CachingTestConfig only loads once
		reset(scbIntegrationMock);

		when(scbIntegrationMock.getKPI(any(), any()))
			.thenReturn(KPI_VALUE) // On first call, return list
			.thenThrow(new RuntimeException("Result should be cached!")); // If any more calls are received, throw exception
	}

	@Test
	void testCaching() {
		// Arrange
		final var kpiBase = KPI_80;
		final var yearMonth = YearMonth.now();

		// First call should trigger logic in wrapped service class
		final var result1 = scbIntegration.getKPI(kpiBase, yearMonth);
		verify(scbIntegrationMock).getKPI(kpiBase, yearMonth);

		// Second call should go directly to cache and not reach mock
		final var result2 = scbIntegration.getKPI(kpiBase, yearMonth);
		verifyNoMoreInteractions(scbIntegrationMock);

		// Verify that the result is the same
		assertThat(result1).isSameAs(result2);
	}

}
