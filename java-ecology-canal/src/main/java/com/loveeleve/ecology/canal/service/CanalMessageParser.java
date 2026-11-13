package com.loveeleve.ecology.canal.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.loveeleve.ecology.canal.model.CanalRowEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CanalMessageParser {

    public List<CanalRowEvent> parse(Message message, String destination) throws InvalidProtocolBufferException {
        List<CanalRowEvent> events = new ArrayList<>();
        if (message == null || message.getEntries() == null) {
            return events;
        }
        for (CanalEntry.Entry entry : message.getEntries()) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            CanalEntry.Header header = entry.getHeader();
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            if (rowChange.getIsDdl()) {
                events.add(new CanalRowEvent(destination, header.getSchemaName(), header.getTableName(),
                        "DDL", Map.of(), Map.of("sql", rowChange.getSql()),
                        header.getExecuteTime(), message.getId(), header.getLogfileOffset()));
                continue;
            }
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                events.add(new CanalRowEvent(
                        destination,
                        header.getSchemaName(),
                        header.getTableName(),
                        rowChange.getEventType().name(),
                        columns(rowData.getBeforeColumnsList()),
                        columns(rowData.getAfterColumnsList()),
                        header.getExecuteTime(),
                        message.getId(),
                        header.getLogfileOffset()));
            }
        }
        return events;
    }

    private Map<String, String> columns(List<CanalEntry.Column> columns) {
        Map<String, String> result = new LinkedHashMap<>();
        for (CanalEntry.Column column : columns) {
            result.put(column.getName(), column.getIsNull() ? null : column.getValue());
        }
        return result;
    }
}
