package com.loveeleve.ecology.extension;

import com.loveeleve.ecology.extension.autoconfigure.EcologyAutoConfigurationImportListener;
import com.loveeleve.ecology.extension.failure.EcologyFailureAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationImportListener;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.io.support.SpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;

class EcologySpringFactoriesTests {

    @Test
    void registersAutoConfigurationImportListener() {
        assertThat(SpringFactoriesLoader.loadFactories(
                AutoConfigurationImportListener.class, getClass().getClassLoader()))
                .anyMatch(EcologyAutoConfigurationImportListener.class::isInstance);
    }

    @Test
    void registersFailureAnalyzer() {
        assertThat(SpringFactoriesLoader.loadFactoryNames(
                FailureAnalyzer.class, getClass().getClassLoader()))
                .contains(EcologyFailureAnalyzer.class.getName());
    }
}
