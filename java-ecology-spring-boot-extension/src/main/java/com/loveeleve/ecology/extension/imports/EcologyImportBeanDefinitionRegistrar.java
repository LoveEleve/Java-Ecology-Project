package com.loveeleve.ecology.extension.imports;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class EcologyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition("ecologyRegistrarMarker")) {
            registry.registerBeanDefinition("ecologyRegistrarMarker",
                    new RootBeanDefinition(EcologyRegistrarMarker.class));
        }
    }
}
