package com.example.library;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface Flow {

	/* required */
	String flowName();

	@AliasFor(annotation = Component.class)
	String value() default "";

}
