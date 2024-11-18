package se.sundsvall.billingdatacollector;

import static org.springframework.boot.SpringApplication.run;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import se.sundsvall.dept44.ServiceApplication;

@ServiceApplication
@EnableFeignClients
@EnableCaching
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class Application {
	public static void main(final String... args) {
		run(Application.class, args);
	}
}
