package com.loveeleve.ecology.seata.order.client;

import com.loveeleve.ecology.seata.api.AccountDebitRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "seata-account-service", url = "${services.account.url:}")
public interface AccountClient {

    @PostMapping("/internal/accounts/debit")
    void debit(@RequestBody AccountDebitRequest request);
}
