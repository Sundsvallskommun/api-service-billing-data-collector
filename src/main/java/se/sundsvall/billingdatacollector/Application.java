package se.sundsvall.billingdatacollector;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import se.sundsvall.dept44.ServiceApplication;

@ServiceApplication
@EnableCaching
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class Application {
	public static void main(final String... args) {
		run(Application.class, args);
	}
}
