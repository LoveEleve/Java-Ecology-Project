package com.loveeleve.ecology.mybatisplus.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loveeleve.ecology.mybatisplus.dto.PageResult;
import com.loveeleve.ecology.mybatisplus.dto.UserCreateRequest;
import com.loveeleve.ecology.mybatisplus.dto.UserResponse;
import com.loveeleve.ecology.mybatisplus.dto.UserUpdateRequest;
import com.loveeleve.ecology.mybatisplus.entity.User;
import com.loveeleve.ecology.mybatisplus.enums.UserStatus;
import com.loveeleve.ecology.mybatisplus.exception.OptimisticLockException;
import com.loveeleve.ecology.mybatisplus.exception.UserNotFoundException;
import com.loveeleve.ecology.mybatisplus.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Transactional(readOnly = true)
    public UserResponse get(long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponse> query(String username, String email, UserStatus status,
                                          Integer minAge, Integer maxAge, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(1, Math.min(size, 100));
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StringUtils.hasText(username), User::getUsername, username)
                .like(StringUtils.hasText(email), User::getEmail, email)
                .eq(status != null, User::getStatus, status)
                .ge(minAge != null, User::getAge, minAge)
                .le(maxAge != null, User::getAge, maxAge)
                .orderByAsc(User::getId);

        Page<User> result = page(new Page<>(safePage, safeSize), wrapper);
        List<UserResponse> records = result.getRecords().stream().map(UserResponse::from).toList();
        return new PageResult<>(result.getTotal(), result.getPages(), result.getCurrent(), result.getSize(), records);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> selectByStatus(UserStatus status) {
        return baseMapper.selectByStatus(status).stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setStatus(UserStatus.ACTIVE);
        user.setAge(request.age());
        user.setVersion(0);
        user.setDeleted(0);
        save(user);
        return UserResponse.from(getById(user.getId()));
    }

    @Transactional
    public int createBatch(List<UserCreateRequest> requests) {
        List<User> users = requests.stream().map(request -> {
            User user = new User();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setStatus(UserStatus.ACTIVE);
            user.setAge(request.age());
            user.setVersion(0);
            user.setDeleted(0);
            return user;
        }).toList();
        saveBatch(users);
        return users.size();
    }

    @Transactional
    public UserResponse update(long id, UserUpdateRequest request) {
        User user = requireUser(id);
        if (request.version() != null && !request.version().equals(user.getVersion())) {
            throw new OptimisticLockException(id);
        }
        if (StringUtils.hasText(request.username())) {
            user.setUsername(request.username());
        }
        if (StringUtils.hasText(request.email())) {
            user.setEmail(request.email());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        if (!updateById(user)) {
            throw new OptimisticLockException(id);
        }
        return UserResponse.from(getById(id));
    }

    @Transactional
    public void delete(long id) {
        if (!removeById(id)) {
            throw new UserNotFoundException(id);
        }
    }

    private User requireUser(long id) {
        User user = getById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }
}
