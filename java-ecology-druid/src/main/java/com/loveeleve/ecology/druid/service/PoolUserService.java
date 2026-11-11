package com.loveeleve.ecology.druid.service;

import com.loveeleve.ecology.druid.dto.CreateUserRequest;
import com.loveeleve.ecology.druid.dto.UserResponse;
import com.loveeleve.ecology.druid.exception.UserNotFoundException;
import com.loveeleve.ecology.druid.repository.PoolUserRepository;
import org.springframework.stereotype.Service;

@Service
public class PoolUserService {

    private final PoolUserRepository repository;

    public PoolUserService(PoolUserRepository repository) {
        this.repository = repository;
    }

    public UserResponse create(CreateUserRequest request) {
        return UserResponse.from(repository.insert(request.username(), request.email(), request.age()));
    }

    public UserResponse get(long id) {
        return repository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public long count() {
        return repository.count();
    }
}
