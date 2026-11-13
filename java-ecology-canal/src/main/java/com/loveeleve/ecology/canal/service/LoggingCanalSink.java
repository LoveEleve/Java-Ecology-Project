package com.loveeleve.ecology.canal.service;

import com.loveeleve.ecology.canal.model.CanalRowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LoggingCanalSink implements CanalSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingCanalSink.class);

    @Override
    public void write(List<CanalRowEvent> events) {
        for (CanalRowEvent event : events) {
            log.info("canal event destination={} db={} table={} type={} batchId={} offset={}",
                    event.destination(), event.database(), event.table(), event.eventType(),
                    event.batchId(), event.entryId());
        }
    }
}
