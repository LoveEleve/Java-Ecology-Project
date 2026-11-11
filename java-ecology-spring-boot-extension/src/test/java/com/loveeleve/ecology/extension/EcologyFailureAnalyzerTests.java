package com.loveeleve.ecology.extension;

import com.loveeleve.ecology.extension.failure.EcologyExtensionException;
import com.loveeleve.ecology.extension.failure.EcologyFailureAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

class EcologyFailureAnalyzerTests {

    @Test
    void analyzesEcologyException() {
        FailureAnalysis analysis = new EcologyFailureAnalyzer()
                .analyze(new EcologyExtensionException("bad configuration"));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("bad configuration");
        assertThat(analysis.getAction()).contains("ecology.extension");
    }
}
