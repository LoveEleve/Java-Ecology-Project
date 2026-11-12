package com.loveeleve.ecology.seata.storage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"seata.enabled=false", "spring.cloud.nacos.discovery.enabled=false"})
class SeataStorageApplicationTests {

    @Test
    void contextLoads() {
    }
}
