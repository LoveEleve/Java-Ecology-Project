package com.loveeleve.ecology.multids.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.loveeleve.ecology.multids.config.DataSource;
import com.loveeleve.ecology.multids.config.DataSourceContextHolder;
import com.loveeleve.ecology.multids.config.DataSourceKey;
import com.loveeleve.ecology.multids.entity.AccountEntity;
import com.loveeleve.ecology.multids.mapper.AccountMapper;
import com.loveeleve.ecology.multids.model.Account;
import com.loveeleve.ecology.multids.repository.DynamicAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DynamicAccountService {

    private final DynamicAccountRepository repository;
    private final AccountMapper accountMapper;

    public DynamicAccountService(DynamicAccountRepository repository, AccountMapper accountMapper) {
        this.repository = repository;
        this.accountMapper = accountMapper;
    }

    @DataSource(DataSourceKey.MASTER)
    public List<Account> masterAccountsByAnnotation() {
        return repository.findAll();
    }

    @DataSource(DataSourceKey.REPORT)
    public List<Account> reportAccountsByAnnotation() {
        return repository.findAll();
    }

    @DataSource(DataSourceKey.REPORT)
    public List<Account> reportAccountsByMyBatisPlus() {
        return accountMapper.selectList(Wrappers.lambdaQuery(AccountEntity.class)
                        .orderByAsc(AccountEntity::getId))
                .stream()
                .map(entity -> new Account(entity.getId(), entity.getOwner(), entity.getBalance()))
                .toList();
    }

    @DataSource(DataSourceKey.REPORT)
    @Transactional(readOnly = true)
    public List<Account> reportAccountsInTransaction() {
        return repository.findAll();
    }

    @DataSource(DataSourceKey.REPORT)
    public void failAfterRouting() {
        throw new IllegalStateException("routing failure");
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
