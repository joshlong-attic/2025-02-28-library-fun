package com.example.service;

import com.example.library.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@Flow(flowName = "github")
class MyFlowConfig {
}

@SpringBootApplication
public class ServiceApplication {

    @Bean
    ApplicationRunner messageRunner(@Value("${message:oof}") String message) {
        return args -> System.out.println("message is [" + message + ']');
    }

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
