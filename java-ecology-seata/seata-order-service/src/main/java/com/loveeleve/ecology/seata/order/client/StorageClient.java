package com.loveeleve.ecology.seata.order.client;

import com.loveeleve.ecology.seata.api.StorageDeductRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "seata-storage-service", url = "${services.storage.url:}")
public interface StorageClient {

    @PostMapping("/internal/storages/deduct")
    void deduct(@RequestBody StorageDeductRequest request);
}
