package com.loveeleve.ecology.canal.handler;

import com.alibaba.otter.canal.protocol.Message;
import com.loveeleve.ecology.canal.model.CanalRowEvent;
import com.loveeleve.ecology.canal.service.CanalMessageParser;
import com.loveeleve.ecology.canal.service.CanalSink;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CanalEventHandler {

    private final CanalMessageParser parser;
    private final CanalSink sink;

    public CanalEventHandler(CanalMessageParser parser, CanalSink sink) {
        this.parser = parser;
        this.sink = sink;
    }

    public int handle(Message message, String destination) throws Exception {
        List<CanalRowEvent> events = parser.parse(message, destination);
        sink.write(events);
        return events.size();
    }
}
