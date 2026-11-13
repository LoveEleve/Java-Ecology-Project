package com.loveeleve.ecology.jraft;

import com.loveeleve.ecology.jraft.service.JraftCluster;
import com.loveeleve.ecology.jraft.service.JraftNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JraftClusterTests {

    @Test
    @Timeout(40)
    void threeNodeClusterElectsLeaderAndReplicatesKv() throws Exception {
        List<String> peers = List.of(
                "127.0.0.1:" + freePort(),
                "127.0.0.1:" + freePort(),
                "127.0.0.1:" + freePort());
        Path data = Files.createTempDirectory("ecology-jraft-test");

        try (JraftCluster cluster = JraftCluster.start(data, "test-kv", peers)) {
            cluster.awaitLeader(TimeUnit.SECONDS.toMillis(15));
            JraftNode leader = cluster.leader();

            assertThat(leader.isLeader()).isTrue();
            assertThat(cluster.put("order-1", "PAID")).isEqualTo("PAID");
            assertThat(cluster.get("order-1")).isEqualTo("PAID");
            cluster.awaitReplication("order-1", "PAID", TimeUnit.SECONDS.toMillis(5));

            assertThat(cluster.nodes()).hasSize(3);
            assertThat(cluster.nodes()).allMatch(node ->
                    "PAID".equals(node.stateMachine().get("order-1")));
        }
    }

    private int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
