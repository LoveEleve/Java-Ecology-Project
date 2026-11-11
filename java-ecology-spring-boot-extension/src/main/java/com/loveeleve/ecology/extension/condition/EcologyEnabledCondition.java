package com.loveeleve.ecology.extension.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class EcologyEnabledCondition extends SpringBootCondition {

    static final String ENABLED_PROPERTY = "ecology.extension.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = context.getEnvironment().getProperty(ENABLED_PROPERTY, Boolean.class, true);
        return enabled
                ? ConditionOutcome.match(ENABLED_PROPERTY + " is true")
                : ConditionOutcome.noMatch(ENABLED_PROPERTY + " is false");
    }
}
