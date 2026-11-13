package com.loveeleve.ecology.jraft.service;

import com.alipay.sofa.jraft.entity.PeerId;
import com.loveeleve.ecology.jraft.model.KvCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class JraftCluster implements AutoCloseable {

    private final List<JraftNode> nodes = new ArrayList<>();

    private JraftCluster() {
    }

    public static JraftCluster start(Path baseDir, String groupId, List<String> peers) throws IOException {
        JraftCluster cluster = new JraftCluster();
        try {
            for (String peer : peers) {
                String safeName = peer.replace(':', '_');
                cluster.nodes.add(new JraftNode(baseDir.resolve(safeName).toString(), groupId, peer,
                        String.join(",", peers)));
            }
            cluster.awaitLeader(15_000);
            return cluster;
        } catch (Exception ex) {
            cluster.close();
            throw new IOException("failed to start jraft cluster", ex);
        }
    }

    public JraftNode leader() {
        for (JraftNode node : nodes) {
            if (node.isLeader()) {
                return node;
            }
        }
        throw new IllegalStateException("no leader elected");
    }

    public String put(String key, String value) throws Exception {
        return leader().apply(KvCommand.put(key, value));
    }

    public String get(String key) throws Exception {
        return leader().apply(KvCommand.get(key));
    }

    public List<JraftNode> nodes() {
        return List.copyOf(nodes);
    }

    public void awaitLeader(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            for (JraftNode node : nodes) {
                if (node.isLeader()) {
                    return;
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("leader election timeout");
    }

    public void awaitReplication(String key, String expected, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (nodes.stream().allMatch(node -> expected.equals(node.stateMachine().get(key)))) {
                return;
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("replication timeout for key " + key);
    }

    @Override
    public void close() {
        for (JraftNode node : nodes) {
            node.close();
        }
        nodes.clear();
    }
}
