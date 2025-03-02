package com.example.service;

import com.example.library.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

// this and github.properties would get in the auto configuration of the flow definition for a customer 
@Flow(flowName = "github")
class MyFlowConfig {

    @Bean
    ApplicationRunner messageRunner(
            @Value("${message}") String message,
            @Value("${flow.plugin.name}") String pluginName) {
        return _ ->
                System.out.println("message defined in the plugin [" + pluginName + "]" + " is [" + message + ']');
    }

}
