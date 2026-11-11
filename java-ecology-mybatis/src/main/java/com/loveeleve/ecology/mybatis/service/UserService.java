package com.loveeleve.ecology.mybatis.service;

import com.loveeleve.ecology.mybatis.dto.PageResult;
import com.loveeleve.ecology.mybatis.dto.UserCreateRequest;
import com.loveeleve.ecology.mybatis.dto.UserQuery;
import com.loveeleve.ecology.mybatis.dto.UserResponse;
import com.loveeleve.ecology.mybatis.dto.UserUpdateRequest;
import com.loveeleve.ecology.mybatis.entity.User;
import com.loveeleve.ecology.mybatis.enums.UserStatus;
import com.loveeleve.ecology.mybatis.exception.UserNotFoundException;
import com.loveeleve.ecology.mybatis.mapper.UserAnnotationMapper;
import com.loveeleve.ecology.mybatis.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserAnnotationMapper userAnnotationMapper;

    public UserService(UserMapper userMapper, UserAnnotationMapper userAnnotationMapper) {
        this.userMapper = userMapper;
        this.userAnnotationMapper = userAnnotationMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse get(long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getByAnnotationMapper(long id) {
        User user = userAnnotationMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponse> query(UserQuery query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = (safePage - 1) * safeSize;
        long total = userMapper.countByQuery(query);
        List<UserResponse> records = total == 0
                ? List.of()
                : userMapper.selectByQuery(query, offset, safeSize).stream().map(UserResponse::from).toList();
        return new PageResult<>(total, safePage, safeSize, records);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setStatus(UserStatus.ACTIVE);
        user.setAge(request.age());
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return UserResponse.from(user);
    }

    @Transactional
    public int createBatch(List<UserCreateRequest> requests) {
        LocalDateTime now = LocalDateTime.now();
        List<User> users = requests.stream().map(request -> {
            User user = new User();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setStatus(UserStatus.ACTIVE);
            user.setAge(request.age());
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            return user;
        }).toList();
        return userMapper.insertBatch(users);
    }

    @Transactional
    public UserResponse update(long id, UserUpdateRequest request) {
        User user = requireUser(id);
        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return UserResponse.from(user);
    }

    @Transactional
    public void delete(long id) {
        if (userMapper.deleteById(id) == 0) {
            throw new UserNotFoundException(id);
        }
    }

    private User requireUser(long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }
}
