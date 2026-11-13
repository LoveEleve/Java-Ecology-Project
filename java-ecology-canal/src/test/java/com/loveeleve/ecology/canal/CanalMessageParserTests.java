package com.loveeleve.ecology.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.loveeleve.ecology.canal.model.CanalRowEvent;
import com.loveeleve.ecology.canal.service.CanalMessageParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanalMessageParserTests {

    private final CanalMessageParser parser = new CanalMessageParser();

    @Test
    void parsesInsertRowWithAfterColumns() throws Exception {
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
                .addAfterColumns(column("id", "1"))
                .addAfterColumns(column("amount", "100.00"))
                .build();
        CanalEntry.RowChange change = CanalEntry.RowChange.newBuilder()
                .setEventType(CanalEntry.EventType.INSERT)
                .addRowDatas(rowData)
                .build();

        List<CanalRowEvent> events = parser.parse(message(change), "example");

        assertThat(events).hasSize(1);
        CanalRowEvent event = events.get(0);
        assertThat(event.eventType()).isEqualTo("INSERT");
        assertThat(event.database()).isEqualTo("demo");
        assertThat(event.table()).isEqualTo("orders");
        assertThat(event.after()).containsEntry("id", "1").containsEntry("amount", "100.00");
        assertThat(event.before()).isEmpty();
        assertThat(event.batchId()).isEqualTo(7);
    }

    @Test
    void parsesUpdateBeforeAndAfter() throws Exception {
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
                .addBeforeColumns(column("amount", "100.00"))
                .addAfterColumns(column("amount", "120.00"))
                .build();
        CanalEntry.RowChange change = CanalEntry.RowChange.newBuilder()
                .setEventType(CanalEntry.EventType.UPDATE)
                .addRowDatas(rowData)
                .build();

        CanalRowEvent event = parser.parse(message(change), "example").get(0);

        assertThat(event.eventType()).isEqualTo("UPDATE");
        assertThat(event.before()).containsEntry("amount", "100.00");
        assertThat(event.after()).containsEntry("amount", "120.00");
    }

    @Test
    void parsesDdlAsSchemaEvent() throws Exception {
        CanalEntry.RowChange change = CanalEntry.RowChange.newBuilder()
                .setIsDdl(true)
                .setSql("ALTER TABLE orders ADD COLUMN note VARCHAR(64)")
                .build();

        CanalRowEvent event = parser.parse(message(change), "example").get(0);

        assertThat(event.eventType()).isEqualTo("DDL");
        assertThat(event.after()).containsEntry("sql",
                "ALTER TABLE orders ADD COLUMN note VARCHAR(64)");
    }

    private CanalEntry.Column column(String name, String value) {
        return CanalEntry.Column.newBuilder()
                .setName(name)
                .setValue(value)
                .setIsNull(false)
                .build();
    }

    private Message message(CanalEntry.RowChange change) {
        CanalEntry.Header header = CanalEntry.Header.newBuilder()
                .setSchemaName("demo")
                .setTableName("orders")
                .setExecuteTime(123456789L)
                .setLogfileOffset(42L)
                .build();
        CanalEntry.Entry entry = CanalEntry.Entry.newBuilder()
                .setHeader(header)
                .setEntryType(CanalEntry.EntryType.ROWDATA)
                .setStoreValue(change.toByteString())
                .build();
        return new Message(7L, List.of(entry));
    }
}
