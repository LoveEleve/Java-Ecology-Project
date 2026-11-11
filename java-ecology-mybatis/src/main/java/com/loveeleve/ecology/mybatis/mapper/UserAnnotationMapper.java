package com.loveeleve.ecology.mybatis.mapper;

import com.loveeleve.ecology.mybatis.entity.User;
import com.loveeleve.ecology.mybatis.enums.UserStatus;
import com.loveeleve.ecology.mybatis.typehandler.UserStatusTypeHandler;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface UserAnnotationMapper {

    @Select("""
            SELECT id, username, email, status, age, created_at, updated_at
            FROM ecology_user
            WHERE id = #{id}
            """)
    @Results(id = "userResult", value = {
            @Result(column = "id", property = "id", id = true),
            @Result(column = "username", property = "username"),
            @Result(column = "email", property = "email"),
            @Result(column = "status", property = "status", typeHandler = UserStatusTypeHandler.class),
            @Result(column = "age", property = "age"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "updated_at", property = "updatedAt")
    })
    User selectById(long id);

    @Select("SELECT COUNT(*) FROM ecology_user WHERE status = #{status, typeHandler=com.loveeleve.ecology.mybatis.typehandler.UserStatusTypeHandler}")
    long countByStatus(UserStatus status);

    @Insert("""
            INSERT INTO ecology_user (username, email, status, age, created_at, updated_at)
            VALUES (#{username}, #{email},
            #{status, typeHandler=com.loveeleve.ecology.mybatis.typehandler.UserStatusTypeHandler},
            #{age}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
