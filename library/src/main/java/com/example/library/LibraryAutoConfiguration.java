package com.example.library;

import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.util.Properties;

// todo in the starter for customers to use in their flow library contributions,
// use convention to automatically load the property file from the classpath given the flowName 


/*
 * there are two concerns in this class:
 *
 * 1.) we want to register a property source dynamically based on the name of the plugin
 * 2.) we want to tell graalvm about the property file, again, dynamically, based on the name of the plugin
 * */


@Configuration
//@EnableConfigurationProperties(ConfigurationProperties.class)
class LibraryAutoConfiguration {


    @Bean
    ApplicationRunner lateBoundPropertySourceModifyingThingy(
            ConfigurableListableBeanFactory beanFactory,
            Environment environment) {
        return args -> {
            var pluginName = findFlowPluginName(beanFactory);
            System.out.println("plugin name is " + pluginName);
            if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
                var properties = new Properties();
                try (var inp = new ClassPathResource("" + pluginName + ".properties").getInputStream()) {
                    properties.load(inp);
                }
                configurableEnvironment.getPropertySources().addFirst(new PropertiesPropertySource(pluginName + "", properties));
            }

        };
    }

    @Bean
    static MyBeanFactoryAotInitializationProcessor myBeanFactoryAotInitializationProcessor() {
        return new MyBeanFactoryAotInitializationProcessor();
    }


    private static String findFlowPluginName(ConfigurableListableBeanFactory beanFactory) {
        var beanDefinitionNames = beanFactory.getBeanNamesForAnnotation(Flow.class);
        for (var beanDefinitionName : beanDefinitionNames) {
            var clzz = beanFactory.getType(beanDefinitionName);
            var superclass = clzz.getSuperclass();
            var flow = superclass.getAnnotation(Flow.class);
            return flow.flowName();
        }
        return null;
    }

    static class MyBeanFactoryAotInitializationProcessor implements BeanFactoryInitializationAotProcessor {

        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
            return (generationContext, beanFactoryInitializationCode) -> {
                var runtimeHints = generationContext.getRuntimeHints();
                var flowName = findFlowPluginName(beanFactory);
                if (StringUtils.hasText(flowName)) {
                    var propertyFile = flowName + ".properties";
                    runtimeHints.resources().registerPattern(propertyFile);
                    System.out.println("registering flow properties for " + propertyFile + ".");
                } else {
                    System.out.println("no flow name found!!");
                }
            };
        }
    }

//
//    @Bean
//    ApplicationRunner applicationRunner(ConfigurationProperties properties) {
//        return args ->
//                System.out.println("this is a flow for the plugin called " + properties.flowName());
//    }
}


/*
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "something")
record ConfigurationProperties(String flowName) {
}*/
