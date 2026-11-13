package com.loveeleve.ecology.jraft.service;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.RaftGroupService;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.option.NodeOptions;
import com.alipay.sofa.jraft.rpc.RaftRpcServerFactory;
import com.alipay.sofa.jraft.rpc.RpcServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.jraft.model.KvCommand;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class JraftNode implements AutoCloseable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RaftGroupService raftGroupService;
    private final Node node;
    private final KvStateMachine stateMachine;

    public JraftNode(String dataPath, String groupId, String serverIdStr, String peers) throws IOException {
        PeerId serverId = new PeerId();
        if (!serverId.parse(serverIdStr)) {
            throw new IllegalArgumentException("invalid serverId: " + serverIdStr);
        }
        Configuration initialConf = new Configuration();
        if (!initialConf.parse(peers)) {
            throw new IllegalArgumentException("invalid peers: " + peers);
        }
        Path base = Path.of(dataPath);
        Files.createDirectories(base);
        this.stateMachine = new KvStateMachine();
        NodeOptions options = new NodeOptions();
        options.setElectionTimeoutMs(1000);
        options.setSnapshotIntervalSecs(30);
        options.setFsm(stateMachine);
        options.setInitialConf(initialConf);
        options.setLogUri(base.resolve("log").toString());
        options.setRaftMetaUri(base.resolve("raft_meta").toString());
        options.setSnapshotUri(base.resolve("snapshot").toString());
        RpcServer rpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint());
        this.raftGroupService = new RaftGroupService(groupId, serverId, options, rpcServer);
        this.node = raftGroupService.start();
        if (this.node == null) {
            throw new IllegalStateException("failed to start raft node " + serverIdStr);
        }
    }

    public String apply(KvCommand command) throws Exception {
        if (!node.isLeader()) {
            throw new IllegalStateException("node is not leader, current leader: " + node.getLeaderId());
        }
        KvClosure closure = new KvClosure(command);
        ByteBuffer data = ByteBuffer.wrap(OBJECT_MAPPER.writeValueAsBytes(command));
        node.apply(new Task(data, closure));
        return closure.result().get(5, TimeUnit.SECONDS);
    }

    public boolean isLeader() {
        return node.isLeader();
    }

    public PeerId leaderId() {
        return node.getLeaderId();
    }

    public KvStateMachine stateMachine() {
        return stateMachine;
    }

    @Override
    public void close() {
        raftGroupService.shutdown();
    }
}
