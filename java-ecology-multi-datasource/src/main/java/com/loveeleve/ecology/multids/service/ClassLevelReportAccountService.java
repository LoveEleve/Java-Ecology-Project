package com.loveeleve.ecology.multids.service;

import com.loveeleve.ecology.multids.config.DataSource;
import com.loveeleve.ecology.multids.config.DataSourceKey;
import com.loveeleve.ecology.multids.model.Account;
import com.loveeleve.ecology.multids.repository.DynamicAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@DataSource(DataSourceKey.REPORT)
public class ClassLevelReportAccountService {

    private final DynamicAccountRepository repository;

    public ClassLevelReportAccountService(DynamicAccountRepository repository) {
        this.repository = repository;
    }

    public List<Account> accounts() {
        return repository.findAll();
    }
}
