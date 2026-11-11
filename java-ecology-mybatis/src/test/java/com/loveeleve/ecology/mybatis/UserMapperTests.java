package com.loveeleve.ecology.mybatis;

import com.loveeleve.ecology.mybatis.dto.UserQuery;
import com.loveeleve.ecology.mybatis.entity.User;
import com.loveeleve.ecology.mybatis.enums.UserStatus;
import com.loveeleve.ecology.mybatis.mapper.UserAnnotationMapper;
import com.loveeleve.ecology.mybatis.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserMapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAnnotationMapper userAnnotationMapper;

    @Test
    void xmlMapperCrud() {
        User user = newUser("crud-" + UUID.randomUUID(), "crud@example.com", UserStatus.ACTIVE, 30);
        assertThat(userMapper.insert(user)).isEqualTo(1);
        assertThat(user.getId()).isNotNull();

        User loaded = userMapper.selectById(user.getId());
        assertThat(loaded.getUsername()).isEqualTo(user.getUsername());
        assertThat(loaded.getStatus()).isEqualTo(UserStatus.ACTIVE);

        loaded.setAge(31);
        loaded.setUpdatedAt(LocalDateTime.now());
        assertThat(userMapper.update(loaded)).isEqualTo(1);
        assertThat(userMapper.selectById(user.getId()).getAge()).isEqualTo(31);

        assertThat(userMapper.deleteById(user.getId())).isEqualTo(1);
        assertThat(userMapper.selectById(user.getId())).isNull();
    }

    @Test
    void dynamicQueryAndPagination() {
        UserQuery query = new UserQuery();
        query.setUsername("ali");
        assertThat(userMapper.countByQuery(query)).isEqualTo(1);
        assertThat(userMapper.selectByQuery(query, 0, 10))
                .extracting(User::getUsername)
                .containsExactly("alice");

        UserQuery statusQuery = new UserQuery();
        statusQuery.setStatus(UserStatus.DISABLED);
        statusQuery.setMinAge(40);
        assertThat(userMapper.selectByQuery(statusQuery, 0, 10))
                .extracting(User::getUsername)
                .contains("carol");

        assertThat(userMapper.selectByQuery(new UserQuery(), 0, 2)).hasSize(2);
    }

    @Test
    void typeHandlerReadsEnumFromIntegerColumn() {
        assertThat(userMapper.selectByStatus(UserStatus.DISABLED))
                .extracting(User::getStatus)
                .containsOnly(UserStatus.DISABLED);
    }

    @Test
    void batchInsertAndAnnotationMapper() {
        String prefix = "batch-" + UUID.randomUUID();
        List<User> users = List.of(
                newUser(prefix + "-1", prefix + "-1@example.com", UserStatus.ACTIVE, 20),
                newUser(prefix + "-2", prefix + "-2@example.com", UserStatus.ACTIVE, 21)
        );
        assertThat(userMapper.insertBatch(users)).isEqualTo(2);

        UserQuery query = new UserQuery();
        query.setUsername(prefix);
        assertThat(userMapper.countByQuery(query)).isEqualTo(2);

        User annotationLoaded = userAnnotationMapper.selectById(1L);
        assertThat(annotationLoaded.getUsername()).isEqualTo("alice");
        assertThat(annotationLoaded.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(userAnnotationMapper.countByStatus(UserStatus.ACTIVE)).isGreaterThanOrEqualTo(3);

        User annotationInserted = newUser("annotation-" + UUID.randomUUID(), "annotation@example.com",
                UserStatus.ACTIVE, 22);
        assertThat(userAnnotationMapper.insert(annotationInserted)).isEqualTo(1);
        assertThat(annotationInserted.getId()).isNotNull();
    }

    private User newUser(String username, String email, UserStatus status, int age) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setStatus(status);
        user.setAge(age);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
