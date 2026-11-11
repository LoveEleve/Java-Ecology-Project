package com.loveeleve.ecology.extension.imports;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class EcologyDeferredImportSelector implements DeferredImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{EcologyDeferredImportedConfiguration.class.getName()};
    }
}
