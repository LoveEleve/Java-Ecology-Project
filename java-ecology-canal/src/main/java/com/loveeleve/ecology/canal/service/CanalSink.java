package com.loveeleve.ecology.canal.service;

import com.loveeleve.ecology.canal.model.CanalRowEvent;

import java.util.List;

public interface CanalSink {

    void write(List<CanalRowEvent> events) throws Exception;
}
