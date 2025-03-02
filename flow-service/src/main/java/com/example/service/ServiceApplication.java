package com.example.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * this is meant to be a stand-in for the main app that your team deploys.
 *
 * NB: there is no explicit mention of the plugins. they're simply on the classpath.
 */
@SpringBootApplication
public class ServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}

}
