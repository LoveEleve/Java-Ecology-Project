package com.loveeleve.ecology.extension.failure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class EcologyFailureAnalyzer extends AbstractFailureAnalyzer<EcologyExtensionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, EcologyExtensionException cause) {
        return new FailureAnalysis(
                "Ecology extension failed: " + cause.getMessage(),
                "Check ecology.extension.* properties and the extension auto-configuration conditions.",
                cause);
    }
}
