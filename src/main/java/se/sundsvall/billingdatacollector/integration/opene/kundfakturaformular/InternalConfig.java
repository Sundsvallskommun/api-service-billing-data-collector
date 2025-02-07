package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.AktivitetskontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.AnsvarIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningarIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.ProjektkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.SummeringIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.UnderkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.VerksamhetIntern;

/**
 * Configuration class for internal OpenE objects
 * Every bean is exposed with the default name, the name of the method.
 * Each bean is of prototype scope, i.e. a new instance is created every time the bean is requested.
 */
@Configuration
class InternalConfig {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	AktivitetskontoIntern aktivitetskontoIntern() {
		return AktivitetskontoIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	AnsvarIntern ansvarIntern() {
		return AnsvarIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	BerakningarIntern berakningarIntern() {
		return BerakningarIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	BerakningIntern berakningIntern() {
		return BerakningIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	ProjektkontoIntern projektkontoIntern() {
		return ProjektkontoIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	SummeringIntern summeringIntern() {
		return SummeringIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	UnderkontoIntern underkontoIntern() {
		return UnderkontoIntern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	VerksamhetIntern verksamhetIntern() {
		return VerksamhetIntern.builder().build();
	}
}
