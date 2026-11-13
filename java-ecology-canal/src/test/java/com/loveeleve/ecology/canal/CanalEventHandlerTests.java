package com.loveeleve.ecology.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.loveeleve.ecology.canal.handler.CanalEventHandler;
import com.loveeleve.ecology.canal.model.CanalRowEvent;
import com.loveeleve.ecology.canal.service.CanalMessageParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanalEventHandlerTests {

    @Test
    void sendsParsedEventsToSink() throws Exception {
        List<CanalRowEvent> captured = new ArrayList<>();
        CanalEventHandler handler = new CanalEventHandler(new CanalMessageParser(), captured::addAll);

        int handled = handler.handle(message(), "example");

        assertThat(handled).isEqualTo(1);
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).eventType()).isEqualTo("INSERT");
    }

    private Message message() {
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
                .addAfterColumns(CanalEntry.Column.newBuilder().setName("id").setValue("1").setIsNull(false))
                .build();
        CanalEntry.RowChange change = CanalEntry.RowChange.newBuilder()
                .setEventType(CanalEntry.EventType.INSERT)
                .addRowDatas(rowData)
                .build();
        CanalEntry.Header header = CanalEntry.Header.newBuilder()
                .setSchemaName("demo").setTableName("orders").setExecuteTime(1L).setLogfileOffset(2L)
                .build();
        CanalEntry.Entry entry = CanalEntry.Entry.newBuilder()
                .setHeader(header).setEntryType(CanalEntry.EntryType.ROWDATA)
                .setStoreValue(change.toByteString()).build();
        return new Message(1L, List.of(entry));
    }
}
