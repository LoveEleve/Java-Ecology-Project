package com.loveeleve.ecology.canal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.canal.enabled=false")
class CanalApplicationTests {

    @Test
    void contextLoadsWithoutConnectingToCanal() {
    }
}
