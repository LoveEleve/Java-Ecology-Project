package com.loveeleve.ecology.mybatis;

import com.loveeleve.ecology.mybatis.dto.UserCreateRequest;
import com.loveeleve.ecology.mybatis.dto.UserQuery;
import com.loveeleve.ecology.mybatis.exception.UserNotFoundException;
import com.loveeleve.ecology.mybatis.mapper.UserMapper;
import com.loveeleve.ecology.mybatis.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void createBatchRollsBackWhenAnyRowFails() {
        String unique = "rollback-" + UUID.randomUUID();
        List<UserCreateRequest> requests = List.of(
                new UserCreateRequest(unique, unique + "@example.com", 20),
                new UserCreateRequest("alice", "duplicate@example.com", 21)
        );

        assertThatThrownBy(() -> userService.createBatch(requests))
                .isInstanceOf(DuplicateKeyException.class);

        UserQuery query = new UserQuery();
        query.setUsername(unique);
        assertThat(userMapper.countByQuery(query)).isZero();
    }

    @Test
    void missingUserRaisesNotFound() {
        assertThatThrownBy(() -> userService.get(999_999L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
