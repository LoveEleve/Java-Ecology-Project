package com.loveeleve.ecology.sharding.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingValue;

import java.util.Collection;

public final class OrderTableHintAlgorithm implements HintShardingAlgorithm<Comparable<?>> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         HintShardingValue<Comparable<?>> shardingValue) {
        return shardingValue.getValues().stream()
                .map(value -> "t_order_hint_" + value)
                .filter(availableTargetNames::contains)
                .toList();
    }

    @Override
    public String getType() {
        return "ORDER_TABLE_HINT";
    }
}
