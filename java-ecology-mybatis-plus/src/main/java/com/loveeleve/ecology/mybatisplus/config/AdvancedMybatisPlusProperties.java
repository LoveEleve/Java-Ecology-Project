package com.loveeleve.ecology.mybatisplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mybatis-plus.advanced")
public class AdvancedMybatisPlusProperties {

    private boolean tenantEnabled;
    private long tenantId;
    private String tenantColumn = "tenant_id";
    private boolean dataPermissionEnabled;
    private String dataPermissionSql = "dept_id = 1";
    private boolean dynamicTableEnabled;
    private String dynamicTableName = "ecology_user";
    private String dynamicTableSuffix = "_2026";

    public boolean isTenantEnabled() {
        return tenantEnabled;
    }

    public void setTenantEnabled(boolean tenantEnabled) {
        this.tenantEnabled = tenantEnabled;
    }

    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantColumn() {
        return tenantColumn;
    }

    public void setTenantColumn(String tenantColumn) {
        this.tenantColumn = tenantColumn;
    }

    public boolean isDataPermissionEnabled() {
        return dataPermissionEnabled;
    }

    public void setDataPermissionEnabled(boolean dataPermissionEnabled) {
        this.dataPermissionEnabled = dataPermissionEnabled;
    }

    public String getDataPermissionSql() {
        return dataPermissionSql;
    }

    public void setDataPermissionSql(String dataPermissionSql) {
        this.dataPermissionSql = dataPermissionSql;
    }

    public boolean isDynamicTableEnabled() {
        return dynamicTableEnabled;
    }

    public void setDynamicTableEnabled(boolean dynamicTableEnabled) {
        this.dynamicTableEnabled = dynamicTableEnabled;
    }

    public String getDynamicTableName() {
        return dynamicTableName;
    }

    public void setDynamicTableName(String dynamicTableName) {
        this.dynamicTableName = dynamicTableName;
    }

    public String getDynamicTableSuffix() {
        return dynamicTableSuffix;
    }

    public void setDynamicTableSuffix(String dynamicTableSuffix) {
        this.dynamicTableSuffix = dynamicTableSuffix;
    }
}
