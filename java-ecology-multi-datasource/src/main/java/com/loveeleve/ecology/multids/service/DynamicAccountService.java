package com.loveeleve.ecology.multids.service;

import com.loveeleve.ecology.multids.config.DataSource;
import com.loveeleve.ecology.multids.config.DataSourceContextHolder;
import com.loveeleve.ecology.multids.config.DataSourceKey;
import com.loveeleve.ecology.multids.model.Account;
import com.loveeleve.ecology.multids.repository.DynamicAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DynamicAccountService {

    private final DynamicAccountRepository repository;

    public DynamicAccountService(DynamicAccountRepository repository) {
        this.repository = repository;
    }

    @DataSource(DataSourceKey.MASTER)
    public List<Account> masterAccountsByAnnotation() {
        return repository.findAll();
    }

    @DataSource(DataSourceKey.REPORT)
    public List<Account> reportAccountsByAnnotation() {
        return repository.findAll();
    }

    public List<Account> accountsByKey(DataSourceKey key) {
        DataSourceKey previous = DataSourceContextHolder.get();
        DataSourceContextHolder.set(key);
        try {
            return repository.findAll();
        } finally {
            if (previous == null) {
                DataSourceContextHolder.clear();
            } else {
                DataSourceContextHolder.set(previous);
            }
        }
    }
}
