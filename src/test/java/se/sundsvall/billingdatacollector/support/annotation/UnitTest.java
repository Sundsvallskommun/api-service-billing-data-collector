package se.sundsvall.billingdatacollector.support.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.test.context.ActiveProfiles;

@Inherited
@Retention(RUNTIME)
@Target(TYPE)
@ActiveProfiles("junit")
public @interface UnitTest {

}
