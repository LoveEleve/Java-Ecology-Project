package com.loveeleve.ecology.jraft;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.jraft.enabled=false")
class JraftApplicationTests {

    @Test
    void contextLoadsWithoutStartingRaftNode() {
    }
}
