package com.loveeleve.ecology.mybatis.mapper;

import com.loveeleve.ecology.mybatis.entity.User;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@CacheNamespace(flushInterval = 60_000, size = 256, readWrite = true)
public interface UserCacheMapper {

    @Select("""
            SELECT id, username, email, status, age, created_at, updated_at
            FROM ecology_user
            WHERE id = #{id}
            """)
    User selectById(@Param("id") long id);
}
