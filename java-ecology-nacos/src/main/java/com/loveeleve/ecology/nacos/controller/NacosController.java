package com.loveeleve.ecology.nacos.controller;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.loveeleve.ecology.nacos.common.ApiResponse;
import com.loveeleve.ecology.nacos.config.NacosDemoProperties;
import com.loveeleve.ecology.nacos.dto.PublishConfigRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nacos")
public class NacosController {

    private final NacosDemoProperties properties;
    private final ObjectProvider<DiscoveryClient> discoveryClient;
    private final ObjectProvider<NacosConfigManager> configManager;

    public NacosController(NacosDemoProperties properties,
                           ObjectProvider<DiscoveryClient> discoveryClient,
                           ObjectProvider<NacosConfigManager> configManager) {
        this.properties = properties;
        this.discoveryClient = discoveryClient;
        this.configManager = configManager;
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, String>> localConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("message", properties.getMessage());
        config.put("group", properties.getGroup());
        return ApiResponse.ok(config);
    }

    @GetMapping("/remote-config")
    public ApiResponse<String> remoteConfig(@RequestParam String dataId,
                                            @RequestParam(required = false) String group) {
        NacosConfigManager manager = requireConfigManager();
        try {
            String content = manager.getConfigService()
                    .getConfig(dataId, group == null ? properties.getGroup() : group, 3000);
            return ApiResponse.ok(content);
        } catch (NacosException ex) {
            throw new IllegalStateException("failed to read Nacos config", ex);
        }
    }

    @PostMapping("/config")
    public ResponseEntity<ApiResponse<Boolean>> publish(@Valid @RequestBody PublishConfigRequest request) {
        NacosConfigManager manager = requireConfigManager();
        if (!properties.isPublishEnabled()) {
            throw new IllegalStateException("Nacos config publish is disabled; set ecology.nacos.publish-enabled=true");
        }
        try {
            boolean published = manager.getConfigService()
                    .publishConfig(request.dataId(), request.group(), request.content(), request.type());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(published));
        } catch (NacosException ex) {
            throw new IllegalStateException("failed to publish Nacos config", ex);
        }
    }

    @GetMapping("/services")
    public ApiResponse<List<String>> services() {
        DiscoveryClient client = discoveryClient.getIfAvailable();
        return ApiResponse.ok(client == null ? List.of() : client.getServices());
    }

    @GetMapping("/instances")
    public ApiResponse<List<Map<String, String>>> instances(@RequestParam String serviceId) {
        DiscoveryClient client = discoveryClient.getIfAvailable();
        if (client == null) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(client.getInstances(serviceId).stream().map(this::toMap).toList());
    }

    private NacosConfigManager requireConfigManager() {
        NacosConfigManager manager = configManager.getIfAvailable();
        if (manager == null) {
            throw new IllegalStateException("Nacos config is disabled; start with --spring.profiles.active=nacos");
        }
        return manager;
    }

    private Map<String, String> toMap(ServiceInstance instance) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("serviceId", instance.getServiceId());
        result.put("instanceId", instance.getInstanceId());
        result.put("host", instance.getHost());
        result.put("port", String.valueOf(instance.getPort()));
        result.put("uri", instance.getUri().toString());
        return result;
    }
}
