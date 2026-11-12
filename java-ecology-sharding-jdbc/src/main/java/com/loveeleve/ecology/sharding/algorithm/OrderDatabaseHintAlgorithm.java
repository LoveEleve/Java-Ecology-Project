package com.loveeleve.ecology.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingValue;

import java.util.Collection;
import java.util.List;

public final class OrderDatabaseHintAlgorithm implements HintShardingAlgorithm<Comparable<?>> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         HintShardingValue<Comparable<?>> shardingValue) {
        return shardingValue.getValues().stream()
                .map(value -> "ds_" + value)
                .filter(availableTargetNames::contains)
                .toList();
    }

    @Override
    public String getType() {
        return "ORDER_DATABASE_HINT";
    }
}
