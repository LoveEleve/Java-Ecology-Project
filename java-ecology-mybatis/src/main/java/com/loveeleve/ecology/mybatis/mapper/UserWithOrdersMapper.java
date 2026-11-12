package com.loveeleve.ecology.mybatis.mapper;

import com.loveeleve.ecology.mybatis.model.UserWithOrders;
import org.apache.ibatis.annotations.Param;

public interface UserWithOrdersMapper {

    UserWithOrders selectUserWithOrders(@Param("id") long id);
}
