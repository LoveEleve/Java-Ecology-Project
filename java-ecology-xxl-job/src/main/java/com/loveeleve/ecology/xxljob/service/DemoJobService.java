package com.loveeleve.ecology.xxljob.service;

import com.loveeleve.ecology.xxljob.model.JobResult;
import org.springframework.stereotype.Service;

import java.util.stream.IntStream;

@Service
public class DemoJobService {

    public JobResult simple(String parameter) {
        if (parameter != null && parameter.contains("fail")) {
            return JobResult.failure("parameter requests failure: " + parameter);
        }
        return JobResult.success(1, "simple job done, parameter=" + parameter);
    }

    public JobResult sharding(int shardIndex, int shardTotal, int totalItems) {
        if (shardTotal <= 0 || shardIndex < 0 || shardIndex >= shardTotal) {
            return JobResult.failure("invalid shard index/total");
        }
        int processed = (int) IntStream.range(0, totalItems)
                .filter(item -> item % shardTotal == shardIndex)
                .count();
        return JobResult.success(processed, "shard " + shardIndex + "/" + shardTotal);
    }
}
