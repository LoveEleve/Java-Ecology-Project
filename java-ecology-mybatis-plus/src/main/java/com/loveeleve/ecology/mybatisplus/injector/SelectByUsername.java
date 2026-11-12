package com.loveeleve.ecology.mybatisplus.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

public class SelectByUsername extends AbstractMethod {

    public SelectByUsername() {
        super("selectByUsername");
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        String sql = "SELECT " + sqlSelectColumns(tableInfo, false)
                + " FROM " + tableInfo.getTableName()
                + " WHERE username = #{username}";
        SqlSource sqlSource = super.createSqlSource(configuration, sql, Object.class);
        return this.addSelectMappedStatementForTable(mapperClass, methodName, sqlSource, tableInfo);
    }
}
