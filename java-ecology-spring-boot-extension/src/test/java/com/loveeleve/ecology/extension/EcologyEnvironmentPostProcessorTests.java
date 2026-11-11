package com.loveeleve.ecology.extension;

import com.loveeleve.ecology.extension.env.EcologyEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class EcologyEnvironmentPostProcessorTests {

    @Test
    void loadsDefaultProperties() {
        StandardEnvironment environment = new StandardEnvironment();

        new EcologyEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("ecology.extension.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("ecology.extension.message")).isNotBlank();
    }
}
