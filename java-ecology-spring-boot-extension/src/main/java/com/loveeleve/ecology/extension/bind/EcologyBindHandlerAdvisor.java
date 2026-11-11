package com.loveeleve.ecology.extension.bind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindHandlerAdvisor;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class EcologyBindHandlerAdvisor implements ConfigurationPropertiesBindHandlerAdvisor, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(EcologyBindHandlerAdvisor.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public BindHandler apply(BindHandler bindHandler) {
        boolean enabled = environment == null
                || environment.getProperty("ecology.extension.bind-listener-enabled", Boolean.class, true);
        if (!enabled) {
            return bindHandler;
        }
        return new AbstractBindHandler(bindHandler) {
            @Override
            public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
                if (name.toString().startsWith("ecology.extension")) {
                    log.debug("binding ecology extension property: {}", name);
                }
                return super.onStart(name, target, context);
            }
        };
    }
}
