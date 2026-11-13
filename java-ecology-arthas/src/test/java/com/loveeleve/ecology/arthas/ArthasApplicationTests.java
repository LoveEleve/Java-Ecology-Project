package com.loveeleve.ecology.arthas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.arthas.enabled=false")
class ArthasApplicationTests {

    @Test
    void contextLoadsWithoutAttachingArthasAgent() {
    }
}
