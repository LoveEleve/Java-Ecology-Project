package com.loveeleve.ecology.mybatis.mapper;

import com.loveeleve.ecology.mybatis.dto.UserQuery;
import com.loveeleve.ecology.mybatis.entity.User;
import com.loveeleve.ecology.mybatis.enums.UserStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    User selectById(@Param("id") long id);

    List<User> selectByQuery(@Param("query") UserQuery query,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    long countByQuery(@Param("query") UserQuery query);

    List<User> selectByStatus(@Param("status") UserStatus status);

    int insert(User user);

    int update(User user);

    int deleteById(@Param("id") long id);

    int insertBatch(@Param("users") List<User> users);
}
