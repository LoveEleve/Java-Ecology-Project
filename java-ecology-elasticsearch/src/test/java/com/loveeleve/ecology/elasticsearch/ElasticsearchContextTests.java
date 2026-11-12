package com.loveeleve.ecology.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ElasticsearchContextTests {

    @Autowired
    private ElasticsearchClient client;

    @Test
    void clientBeanIsConfigured() {
        assertThat(client).isNotNull();
    }
}
