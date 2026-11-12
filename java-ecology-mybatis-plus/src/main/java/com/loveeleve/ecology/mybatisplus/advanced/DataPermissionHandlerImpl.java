package com.loveeleve.ecology.mybatisplus.advanced;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.loveeleve.ecology.mybatisplus.config.AdvancedMybatisPlusProperties;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class DataPermissionHandlerImpl implements DataPermissionHandler {

    private final AdvancedMybatisPlusProperties properties;

    public DataPermissionHandlerImpl(AdvancedMybatisPlusProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        try {
            return CCJSqlParserUtil.parseCondExpression(properties.getDataPermissionSql());
        } catch (JSQLParserException ex) {
            throw new IllegalStateException("invalid data permission SQL", ex);
        }
    }
}
