package com.loveeleve.ecology.mybatisplus.advanced;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import com.loveeleve.ecology.mybatisplus.config.AdvancedMybatisPlusProperties;

public class DynamicTableNameHandlerImpl implements TableNameHandler {

    private final AdvancedMybatisPlusProperties properties;

    public DynamicTableNameHandlerImpl(AdvancedMybatisPlusProperties properties) {
        this.properties = properties;
    }

    @Override
    public String dynamicTableName(String sql, String tableName) {
        if (properties.getDynamicTableName().equalsIgnoreCase(tableName)) {
            return tableName + properties.getDynamicTableSuffix();
        }
        return tableName;
    }
}
