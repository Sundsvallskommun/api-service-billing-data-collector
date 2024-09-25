package se.sundsvall.billingdatacollector;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.sundsvall.dept44.ServiceApplication;

@ServiceApplication
@EnableFeignClients(basePackageClasses = Application.class)
@EnableCaching
@EnableFeignClients
@EnableScheduling
public class Application {
	public static void main(final String... args) {
		run(Application.class, args);
	}
}
