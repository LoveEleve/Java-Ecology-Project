package com.loveeleve.ecology.mybatisplus;

import com.loveeleve.ecology.mybatisplus.dto.PageResult;
import com.loveeleve.ecology.mybatisplus.dto.UserCreateRequest;
import com.loveeleve.ecology.mybatisplus.dto.UserResponse;
import com.loveeleve.ecology.mybatisplus.dto.UserUpdateRequest;
import com.loveeleve.ecology.mybatisplus.entity.User;
import com.loveeleve.ecology.mybatisplus.enums.UserStatus;
import com.loveeleve.ecology.mybatisplus.exception.OptimisticLockException;
import com.loveeleve.ecology.mybatisplus.exception.UserNotFoundException;
import com.loveeleve.ecology.mybatisplus.mapper.UserMapper;
import com.loveeleve.ecology.mybatisplus.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createFillsAuditFieldsAndPersistsEnumCode() {
        String username = "create-" + UUID.randomUUID();
        UserResponse created = userService.create(new UserCreateRequest(username, username + "@example.com", 20));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.version()).isZero();
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM ecology_user WHERE id = ?", Integer.class, created.id());
        assertThat(status).isEqualTo(1);

        UserResponse updated = userService.update(created.id(), new UserUpdateRequest(null, null, 21, null));
        assertThat(updated.age()).isEqualTo(21);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void lambdaQueryAndPaginationWorkWithLogicDeleteFilter() {
        String prefix = "page-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            userService.create(new UserCreateRequest(prefix + "-" + i, prefix + "-" + i + "@example.com", 20 + i));
        }

        PageResult<UserResponse> first = userService.query(prefix, null, UserStatus.ACTIVE, null, null, 1, 2);
        assertThat(first.total()).isEqualTo(3);
        assertThat(first.pages()).isEqualTo(2);
        assertThat(first.records()).hasSize(2);

        PageResult<UserResponse> second = userService.query(prefix, null, UserStatus.ACTIVE, null, null, 2, 2);
        assertThat(second.records()).hasSize(1);

        userService.delete(first.records().get(0).id());
        PageResult<UserResponse> afterDelete = userService.query(prefix, null, UserStatus.ACTIVE, null, null, 1, 10);
        assertThat(afterDelete.total()).isEqualTo(2);
    }

    @Test
    void logicDeleteMarksRowButKeepsItInDatabase() {
        String username = "logic-" + UUID.randomUUID();
        UserResponse created = userService.create(new UserCreateRequest(username, username + "@example.com", 22));

        userService.delete(created.id());

        assertThatThrownBy(() -> userService.get(created.id()))
                .isInstanceOf(UserNotFoundException.class);
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM ecology_user WHERE id = ?", Integer.class, created.id());
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void optimisticLockRejectsStaleUpdate() {
        String username = "optimistic-" + UUID.randomUUID();
        UserResponse created = userService.create(new UserCreateRequest(username, username + "@example.com", 23));

        User first = userMapper.selectById(created.id());
        User stale = userMapper.selectById(created.id());
        first.setAge(24);
        assertThat(userMapper.updateById(first)).isEqualTo(1);

        stale.setAge(25);
        assertThat(userMapper.updateById(stale)).isZero();
    }

    @Test
    void serviceRejectsWrongVersionAndBatchRollsBack() {
        String username = "version-" + UUID.randomUUID();
        UserResponse created = userService.create(new UserCreateRequest(username, username + "@example.com", 26));

        assertThatThrownBy(() -> userService.update(created.id(),
                new UserUpdateRequest(null, null, 27, created.version() + 10)))
                .isInstanceOf(OptimisticLockException.class);

        String unique = "batch-" + UUID.randomUUID();
        assertThatThrownBy(() -> userService.createBatch(List.of(
                new UserCreateRequest(unique, unique + "@example.com", 20),
                new UserCreateRequest("alice", "duplicate@example.com", 21))))
                .isInstanceOf(DuplicateKeyException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ecology_user WHERE username = ?", Integer.class, unique);
        assertThat(count).isZero();
    }

    @Test
    void customSqlInjectorExposesSelectByUsername() {
        assertThat(userMapper.selectByUsername("alice"))
                .extracting(User::getUsername)
                .isEqualTo("alice");
    }

    @Test
    void customSqlAfterLogicDeleteMustFilterDeletedRowsExplicitly() {
        UserResponse created = userService.create(new UserCreateRequest(
                "custom-" + UUID.randomUUID(), "custom@example.com", 30));

        assertThat(userService.selectByStatus(UserStatus.ACTIVE))
                .extracting(UserResponse::id)
                .contains(created.id());

        userService.delete(created.id());
        assertThat(userService.selectByStatus(UserStatus.ACTIVE))
                .extracting(UserResponse::id)
                .doesNotContain(created.id());
    }
}
