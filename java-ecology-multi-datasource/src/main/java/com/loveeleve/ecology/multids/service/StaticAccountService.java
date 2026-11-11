package com.loveeleve.ecology.multids.service;

import com.loveeleve.ecology.multids.model.Account;
import com.loveeleve.ecology.multids.repository.MasterAccountRepository;
import com.loveeleve.ecology.multids.repository.ReportAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaticAccountService {

    private final MasterAccountRepository masterRepository;
    private final ReportAccountRepository reportRepository;

    public StaticAccountService(MasterAccountRepository masterRepository,
                                ReportAccountRepository reportRepository) {
        this.masterRepository = masterRepository;
        this.reportRepository = reportRepository;
    }

    public List<Account> masterAccounts() {
        return masterRepository.findAll();
    }

    public List<Account> reportAccounts() {
        return reportRepository.findAll();
    }
}
