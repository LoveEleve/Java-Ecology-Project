package com.loveeleve.ecology.sca.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.sentinel.enabled=false"
})
class ScaIntegrationApplicationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void routesToExistingServicesAreConfiguredWithoutStartingNacos() {
        assertThat(routeDefinitionLocator.getRouteDefinitions().collectList().block())
                .extracting(definition -> definition.getId())
                .contains("seata-order", "http-provider");
    }
}
