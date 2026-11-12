package com.loveeleve.ecology.mybatisplus.advanced;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.loveeleve.ecology.mybatisplus.config.AdvancedMybatisPlusProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

public class TenantLineHandlerImpl implements TenantLineHandler {

    private final AdvancedMybatisPlusProperties properties;

    public TenantLineHandlerImpl(AdvancedMybatisPlusProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getTenantId() {
        return new LongValue(properties.getTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        return properties.getTenantColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !"ecology_user".equalsIgnoreCase(tableName);
    }
}
