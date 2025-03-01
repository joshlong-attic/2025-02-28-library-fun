package com.example.library;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WARNING!! this code works if and only if there is one and only one class with {@link  Flow} in 
 * the application context! 
 * 
 * need to revisit this design..
 */

@AutoConfiguration
class LibraryAutoConfiguration {

    private static final AtomicReference<BeanFactory> BEAN_FACTORY_ATOMIC_REFERENCE = new AtomicReference<>();

    @Bean
    static LibraryBeanFactoryPostProcessor libraryBeanFactoryPostProcessor() {
        return new LibraryBeanFactoryPostProcessor();
    }

    @Bean
    static LibraryBeanFactoryInitializationAotProcessor myBeanFactoryAotInitializationProcessor() {
        return new LibraryBeanFactoryInitializationAotProcessor();
    }

    
    static class FlowEnvironmentPostProcessor implements EnvironmentPostProcessor {

        @Override
        public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
            environment.getPropertySources().addFirst(new FlowPropertySource());
        }

        private static class FlowPropertySource extends PropertySource<String> {

            private final AtomicReference<PropertySource<?>> delegate = new AtomicReference<>();

            FlowPropertySource() {
                super("library");
            }

            @Override
            public Object getProperty(String name) {
                var beanFactoryIsNotNull = BEAN_FACTORY_ATOMIC_REFERENCE.get() != null;
                var butIsntInitialized = delegate.get() == null;
                if (beanFactoryIsNotNull && butIsntInitialized) {
                    this.doInitialization();
                }
                
                return delegate.get() != null ? delegate.get().getProperty(name) : null;
            }

            private void doInitialization() {
                var plugin = findFlowPluginName((ConfigurableListableBeanFactory) BEAN_FACTORY_ATOMIC_REFERENCE.get());
                Assert.hasText(plugin, "No flow plugin name found!");
                var properties = new Properties();
                var pluginPropertyFile = plugin + ".properties";
                var resource = new ClassPathResource(pluginPropertyFile);
                try (var resourceInputStream = resource.getInputStream()) {
                    properties.load(resourceInputStream);
                    properties.setProperty("flow.plugin.name", plugin);
                    var propertiesPropertySource = new PropertiesPropertySource(plugin, properties);
                    delegate.set(propertiesPropertySource);
                }//  
                catch (Exception e) {
                    throw new IllegalStateException("couldn't load properties from " +
                            pluginPropertyFile, e);
                }
            }


        }


    }

    /**
     * this exists only to capture a pointer to the 
     * {@link BeanFactory beanfactory} as early as possible.
     */
    static class LibraryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            BEAN_FACTORY_ATOMIC_REFERENCE.set(beanFactory);
        }
    }
   

    static class LibraryBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

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

    /**
     * sift through all the beans in the application context and find those annotated with {@link Flow @Flow} 
     * and then extract its flow name.
     * @param beanFactory bean factory
     * @return the name of the plugin 
     */
    private static String findFlowPluginName(ConfigurableListableBeanFactory beanFactory) {
        var beanDefinitionNames = beanFactory.getBeanNamesForAnnotation(Flow.class);
        for (var beanDefinitionName : beanDefinitionNames) {
            var clzz = beanFactory.getType(beanDefinitionName);
            if (null != clzz) {
                var superclass = clzz.getSuperclass();
                var flow = superclass.getAnnotation(Flow.class);
                return flow.flowName();
            }
        }
        return null;
    }

}

 