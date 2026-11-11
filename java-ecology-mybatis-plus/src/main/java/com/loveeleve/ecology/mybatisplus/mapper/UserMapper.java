package com.loveeleve.ecology.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.loveeleve.ecology.mybatisplus.entity.User;
import com.loveeleve.ecology.mybatisplus.enums.UserStatus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT id, username, email, status, age, version, deleted, created_at, updated_at
            FROM ecology_user
            WHERE status = #{status} AND deleted = 0
            ORDER BY id
            """)
    List<User> selectByStatus(@Param("status") UserStatus status);
}
