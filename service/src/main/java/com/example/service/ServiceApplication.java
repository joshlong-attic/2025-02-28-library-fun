package com.example.service;

import com.example.library.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


// this and github.properties would get in the auto configuration of the flow definition for a customer 
@Flow(flowName = "github")
class MyFlowConfig {
}


// this represents the flow service itself 
@SpringBootApplication
public class ServiceApplication {

    @Bean
    ApplicationRunner messageRunner(@Value("${message}") String message) {
        return args -> System.out.println("message is [" + message + ']');
    }

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
