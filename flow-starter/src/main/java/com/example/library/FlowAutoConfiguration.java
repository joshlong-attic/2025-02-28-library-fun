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
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@AutoConfiguration
class FlowAutoConfiguration {

    private static final AtomicReference<BeanFactory> BEAN_FACTORY_ATOMIC_REFERENCE = new AtomicReference<>();

    @Bean
    static LibraryBeanFactoryPostProcessor libraryBeanFactoryPostProcessor() {
        return new LibraryBeanFactoryPostProcessor();
    }

    @Bean
    static LibraryBeanFactoryInitializationAotProcessor libraryBeanFactoryInitializationAotProcessor() {
        return new LibraryBeanFactoryInitializationAotProcessor();
    }

    // this is registered in META-INF/spring.factories
    @SuppressWarnings("unused")
    static class LibraryEnvironmentPostProcessor implements EnvironmentPostProcessor {

        @Override
        public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
            environment.getPropertySources().addFirst(new LibraryPropertySource());
        }

        private static class LibraryPropertySource extends PropertySource<String> {

            private final AtomicReference<CompositePropertySource> delegate = new AtomicReference<>();

            LibraryPropertySource() {
                super("library");
            }

            @Override
            public Object getProperty(String name) {
                var beanFactoryIsNotNull = BEAN_FACTORY_ATOMIC_REFERENCE.get() != null;
                var butDelegateIsntInitializedYet = this.delegate.get() == null;
                if (beanFactoryIsNotNull && butDelegateIsntInitializedYet) {
                    this.initializeLazily();
                }
                return this.delegate.get() == null ? null : this.delegate.get().getProperty(name);
            }

            private void initializeLazily() {
                var plugins = findFlowPluginNames(
                        (ConfigurableListableBeanFactory) BEAN_FACTORY_ATOMIC_REFERENCE.get());
                delegate.set(new CompositePropertySource("flow"));
                for (var plugin : plugins) {
                    var properties = new Properties();
                    var pluginPropertyFile = plugin + ".properties";
                    var resource = new ClassPathResource(pluginPropertyFile);
                    try (var resourceInputStream = resource.getInputStream()) {
                        properties.load(resourceInputStream);
                        delegate.get().addPropertySource(new PropertiesPropertySource(plugin, properties));
                    } //
                    catch (Exception e) {
                        throw new IllegalStateException("couldn't load properties from " + pluginPropertyFile, e);
                    }
                }
            }

        }

    }

    // this exists only to capture a pointer to the BeanFactory as early as possible
    static class LibraryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            BEAN_FACTORY_ATOMIC_REFERENCE.set(beanFactory);
        }

    }

    static class LibraryBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

        @Override
        public BeanFactoryInitializationAotContribution processAheadOfTime(
                ConfigurableListableBeanFactory beanFactory) {
            return (generationContext, beanFactoryInitializationCode) -> {
                var runtimeHints = generationContext.getRuntimeHints();
                var flowNames = findFlowPluginNames(beanFactory);
                for (var flowName : flowNames) {
                    if (StringUtils.hasText(flowName)) {
                        var propertyFile = flowName + ".properties";
                        System.out.println("registering " + propertyFile);
                        runtimeHints.resources().registerPattern(propertyFile);
                    }
                }
            };
        }

    }

    /**
     * sift through all the beans in the {@link ConfigurableListableBeanFactory
     * beanFactory} and find those annotated with {@link Flow @Flow} and then extract from
     * it the flow name.
     */
    private static Set<String> findFlowPluginNames(ConfigurableListableBeanFactory beanFactory) {
        var pluginNames = new HashSet<String>();
        var beanDefinitionNames = beanFactory.getBeanNamesForAnnotation(Flow.class);
        for (var beanDefinitionName : beanDefinitionNames) {
            var clzz = beanFactory.getType(beanDefinitionName);
            if (null != clzz) {
                var superclass = clzz.getSuperclass();
                var flow = superclass.getAnnotation(Flow.class);
                pluginNames.add(flow.flowName());
            }
        }
        return pluginNames;
    }

}
