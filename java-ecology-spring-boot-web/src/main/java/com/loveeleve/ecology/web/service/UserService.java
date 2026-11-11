package com.loveeleve.ecology.web.service;

import com.loveeleve.ecology.web.dto.UserCreateRequest;
import com.loveeleve.ecology.web.dto.UserUpdateRequest;
import com.loveeleve.ecology.web.exception.BusinessException;
import com.loveeleve.ecology.web.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public UserService() {
        create(new UserCreateRequest("Alice", "alice@example.com", 28));
        create(new UserCreateRequest("Bob", "bob@example.com", 35));
        create(new UserCreateRequest("Carol", "carol@example.com", 42));
    }

    public User create(UserCreateRequest request) {
        long id = sequence.getAndIncrement();
        User user = new User(id, request.name(), request.email(), request.age(), Instant.now());
        users.put(id, user);
        return user;
    }

    public User get(long id) {
        User user = users.get(id);
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user " + id + " not found");
        }
        return user;
    }

    public User update(long id, UserUpdateRequest request) {
        User updated = users.computeIfPresent(id, (key, current) ->
                new User(current.id(), request.name(), request.email(), request.age(), current.createdAt()));
        if (updated == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user " + id + " not found");
        }
        return updated;
    }

    public void delete(long id) {
        if (users.remove(id) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user " + id + " not found");
        }
    }

    public List<User> list(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(1, Math.min(size, 100));
        List<User> all = new ArrayList<>(users.values());
        all.sort(Comparator.comparing(User::id));
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return all.subList(from, to);
    }
}
