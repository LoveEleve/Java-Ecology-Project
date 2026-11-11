package com.loveeleve.ecology.extension;

import com.loveeleve.ecology.extension.actuator.EcologyEndpoint;
import com.loveeleve.ecology.extension.actuator.EcologyHealthIndicator;
import com.loveeleve.ecology.extension.actuator.EcologyInfoContributor;
import com.loveeleve.ecology.extension.autoconfigure.EcologyExtensionAutoConfiguration;
import com.loveeleve.ecology.extension.bind.EcologyBindHandlerAdvisor;
import com.loveeleve.ecology.extension.imports.EcologyDeferredImportedMarker;
import com.loveeleve.ecology.extension.imports.EcologyImportedMarker;
import com.loveeleve.ecology.extension.imports.EcologyRegistrarMarker;
import com.loveeleve.ecology.extension.lifecycle.EcologySmartLifecycle;
import com.loveeleve.ecology.extension.processor.EcologyBeanPostProcessor;
import com.loveeleve.ecology.extension.processor.EcologyRegistryMarker;
import com.loveeleve.ecology.extension.properties.EcologyExtensionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class EcologyExtensionAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EcologyExtensionAutoConfiguration.class));

    @Test
    void autoConfigurationCreatesExtensionBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EcologyMarker.class);
            assertThat(context).hasSingleBean(EcologyImportedMarker.class);
            assertThat(context).hasSingleBean(EcologyDeferredImportedMarker.class);
            assertThat(context).hasSingleBean(EcologyRegistrarMarker.class);
            assertThat(context).hasSingleBean(EcologyRegistryMarker.class);
            assertThat(context).hasSingleBean(EcologyBeanPostProcessor.class);
            assertThat(context).hasSingleBean(EcologySmartLifecycle.class);
            assertThat(context).hasSingleBean(EcologyEndpoint.class);
            assertThat(context).hasSingleBean(EcologyHealthIndicator.class);
            assertThat(context).hasSingleBean(EcologyInfoContributor.class);
            assertThat(context).hasSingleBean(EcologyBindHandlerAdvisor.class);
            assertThat(context).hasSingleBean(ApplicationRunner.class);
            assertThat(context).hasSingleBean(EcologyExtensionProperties.class);
            assertThat(context.getBean(EcologyExtensionProperties.class).getMessage()).isNotBlank();
        });
    }

    @Test
    void disabledPropertySkipsAutoConfiguration() {
        contextRunner.withPropertyValues("ecology.extension.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EcologyMarker.class);
                    assertThat(context).doesNotHaveBean(EcologyImportedMarker.class);
                    assertThat(context).doesNotHaveBean(EcologyEndpoint.class);
                });
    }
}
