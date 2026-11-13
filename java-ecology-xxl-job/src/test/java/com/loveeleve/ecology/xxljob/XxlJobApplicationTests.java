package com.loveeleve.ecology.xxljob;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.xxl-job.enabled=false")
class XxlJobApplicationTests {

    @Test
    void contextLoadsWithoutStartingXxlJobExecutor() {
    }
}
