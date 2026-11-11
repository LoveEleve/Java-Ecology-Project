package com.loveeleve.ecology.extension.autoconfigure;

import com.loveeleve.ecology.extension.EcologyMarker;
import com.loveeleve.ecology.extension.actuator.EcologyEndpoint;
import com.loveeleve.ecology.extension.actuator.EcologyHealthIndicator;
import com.loveeleve.ecology.extension.actuator.EcologyInfoContributor;
import com.loveeleve.ecology.extension.bind.EcologyBindHandlerAdvisor;
import com.loveeleve.ecology.extension.condition.ConditionalOnEcologyEnabled;
import com.loveeleve.ecology.extension.imports.EcologyImportBeanDefinitionRegistrar;
import com.loveeleve.ecology.extension.imports.EcologyImportSelector;
import com.loveeleve.ecology.extension.lifecycle.EcologyReadyListener;
import com.loveeleve.ecology.extension.lifecycle.EcologySmartLifecycle;
import com.loveeleve.ecology.extension.processor.EcologyBeanDefinitionRegistryPostProcessor;
import com.loveeleve.ecology.extension.processor.EcologyBeanPostProcessor;
import com.loveeleve.ecology.extension.properties.EcologyExtensionProperties;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnEcologyEnabled
@EnableConfigurationProperties(EcologyExtensionProperties.class)
@Import({EcologyImportSelector.class, EcologyImportBeanDefinitionRegistrar.class})
public class EcologyExtensionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EcologyMarker ecologyMarker() {
        return new EcologyMarker();
    }

    @Bean
    public static EcologyBeanDefinitionRegistryPostProcessor ecologyBeanDefinitionRegistryPostProcessor() {
        return new EcologyBeanDefinitionRegistryPostProcessor();
    }

    @Bean
    public static EcologyBeanPostProcessor ecologyBeanPostProcessor() {
        return new EcologyBeanPostProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public EcologySmartLifecycle ecologySmartLifecycle() {
        return new EcologySmartLifecycle();
    }

    @Bean
    @ConditionalOnMissingBean
    public EcologyReadyListener ecologyReadyListener(EcologyExtensionProperties properties) {
        return new EcologyReadyListener(properties);
    }

    @Bean
    @ConditionalOnClass(Endpoint.class)
    @ConditionalOnMissingBean
    public EcologyEndpoint ecologyEndpoint(EcologyExtensionProperties properties) {
        return new EcologyEndpoint(properties);
    }

    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnMissingBean(name = "ecologyHealthIndicator")
    public EcologyHealthIndicator ecologyHealthIndicator(EcologyExtensionProperties properties) {
        return new EcologyHealthIndicator(properties);
    }

    @Bean
    @ConditionalOnClass(InfoContributor.class)
    @ConditionalOnMissingBean(name = "ecologyInfoContributor")
    public EcologyInfoContributor ecologyInfoContributor(EcologyExtensionProperties properties) {
        return new EcologyInfoContributor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConfigurationPropertiesBindHandlerAdvisor ecologyBindHandlerAdvisor() {
        return new EcologyBindHandlerAdvisor();
    }
}
