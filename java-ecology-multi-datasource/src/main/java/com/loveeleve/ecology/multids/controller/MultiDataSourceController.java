package com.loveeleve.ecology.multids.controller;

import com.loveeleve.ecology.multids.config.DataSourceKey;
import com.loveeleve.ecology.multids.model.Account;
import com.loveeleve.ecology.multids.service.DynamicAccountService;
import com.loveeleve.ecology.multids.service.StaticAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MultiDataSourceController {

    private final StaticAccountService staticService;
    private final DynamicAccountService dynamicService;

    public MultiDataSourceController(StaticAccountService staticService, DynamicAccountService dynamicService) {
        this.staticService = staticService;
        this.dynamicService = dynamicService;
    }

    @GetMapping("/static/master/accounts")
    public List<Account> staticMaster() {
        return staticService.masterAccounts();
    }

    @GetMapping("/static/report/accounts")
    public List<Account> staticReport() {
        return staticService.reportAccounts();
    }

    @GetMapping("/dynamic/master/accounts")
    public List<Account> dynamicMaster() {
        return dynamicService.masterAccountsByAnnotation();
    }

    @GetMapping("/dynamic/report/accounts")
    public List<Account> dynamicReport() {
        return dynamicService.reportAccountsByAnnotation();
    }

    @GetMapping("/dynamic/accounts")
    public List<Account> dynamicByKey(@RequestParam DataSourceKey source) {
        return dynamicService.accountsByKey(source);
    }
}
