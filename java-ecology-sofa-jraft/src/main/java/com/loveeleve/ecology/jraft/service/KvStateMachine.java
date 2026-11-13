package com.loveeleve.ecology.jraft.service;

import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.alipay.sofa.jraft.entity.Task;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loveeleve.ecology.jraft.model.KvCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KvStateMachine extends StateMachineAdapter {

    private static final Logger log = LoggerFactory.getLogger(KvStateMachine.class);
    private static final String SNAPSHOT_FILE = "kv.json";

    private final Map<String, String> data = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onApply(com.alipay.sofa.jraft.Iterator iterator) {
        while (iterator.hasNext()) {
            KvCommand command = null;
            KvClosure closure = null;
            if (iterator.done() instanceof KvClosure done) {
                closure = done;
                command = done.command();
            } else {
                try {
                    byte[] bytes = new byte[iterator.getData().remaining()];
                    iterator.getData().get(bytes);
                    command = objectMapper.readValue(bytes, KvCommand.class);
                } catch (IOException ex) {
                    log.error("failed to decode raft command", ex);
                }
            }
            if (command != null) {
                String result = apply(command);
                if (closure != null) {
                    closure.complete(result);
                }
            }
            iterator.next();
        }
    }

    private String apply(KvCommand command) {
        if ("PUT".equals(command.op())) {
            data.put(command.key(), command.value());
            return command.value();
        }
        if ("GET".equals(command.op())) {
            return data.get(command.key());
        }
        throw new IllegalArgumentException("unknown op: " + command.op());
    }

    public String get(String key) {
        return data.get(key);
    }

    public int size() {
        return data.size();
    }

    @Override
    public void onSnapshotSave(SnapshotWriter writer, com.alipay.sofa.jraft.Closure done) {
        try {
            Path file = Path.of(writer.getPath(), SNAPSHOT_FILE);
            Files.writeString(file, objectMapper.writeValueAsString(data));
            if (!writer.addFile(SNAPSHOT_FILE)) {
                done.run(new Status(com.alipay.sofa.jraft.error.RaftError.EIO, "add snapshot file failed"));
                return;
            }
            done.run(Status.OK());
        } catch (IOException ex) {
            done.run(new Status(com.alipay.sofa.jraft.error.RaftError.EIO, "snapshot save failed"));
        }
    }

    @Override
    public boolean onSnapshotLoad(SnapshotReader reader) {
        if (reader.getFileMeta(SNAPSHOT_FILE) == null) {
            return false;
        }
        try {
            byte[] bytes = Files.readAllBytes(Path.of(reader.getPath(), SNAPSHOT_FILE));
            Map<String, String> restored = objectMapper.readValue(bytes,
                    objectMapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, String.class));
            data.clear();
            data.putAll(restored);
            return true;
        } catch (IOException ex) {
            log.error("snapshot load failed", ex);
            return false;
        }
    }
}
