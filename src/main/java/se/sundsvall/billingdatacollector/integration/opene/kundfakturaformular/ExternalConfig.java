package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.AktivitetskontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.AnsvarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BarakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.MomssatsExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.ObjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.ProjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.SummeringExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.UnderkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.VerksamhetExtern;

/**
 * Configuration class for external OpenE objects
 * Every bean is exposed with the default name, the name of the method.
 * Each bean is of prototype scope, i.e. a new instance is created every time the bean is requested.
 */
@Configuration
public class ExternalConfig {

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public AktivitetskontoExtern aktivitetskontoExtern() {
		return AktivitetskontoExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public AnsvarExtern ansvarExtern() {
		return AnsvarExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public BarakningarExtern barakningarExtern() {
		return BarakningarExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public BerakningarExtern berakningarExtern() {
		return BerakningarExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public BerakningExtern berakningExtern() {
		return BerakningExtern.builder().build();
	}
	
	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public MomssatsExtern momssatsExtern() {
		return MomssatsExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ObjektkontoExtern objektkontoExtern() {
		return ObjektkontoExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ProjektkontoExtern projektkontoExtern() {
		return ProjektkontoExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public SummeringExtern summeringExtern() {
		return SummeringExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public UnderkontoExtern underkontoExtern() {
		return UnderkontoExtern.builder().build();
	}

	@Bean
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public VerksamhetExtern verksamhetExtern() {
		return VerksamhetExtern.builder().build();
	}
}
