package com.loveeleve.ecology.mybatis;

import com.loveeleve.ecology.mybatis.dto.UserQuery;
import com.loveeleve.ecology.mybatis.entity.User;
import com.loveeleve.ecology.mybatis.enums.UserStatus;
import com.loveeleve.ecology.mybatis.mapper.UserMapper;
import com.loveeleve.ecology.mybatis.service.UserBatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MyBatisAdvancedTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserBatchService userBatchService;

    @Test
    void providerSqlSelectsByEmail() {
        assertThat(userMapper.selectByEmailProvider("alice@example.com"))
                .extracting(User::getUsername)
                .isEqualTo("alice");
    }

    @Test
    void batchExecutorInsertsAllRows() {
        String prefix = "batch-exec-" + UUID.randomUUID();
        List<User> users = List.of(newUser(prefix + "-1"), newUser(prefix + "-2"));
        userBatchService.batchInsert(users);

        UserQuery query = new UserQuery();
        query.setUsername(prefix);
        assertThat(userMapper.countByQuery(query)).isEqualTo(2);
    }

    private User newUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setAge(20);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
