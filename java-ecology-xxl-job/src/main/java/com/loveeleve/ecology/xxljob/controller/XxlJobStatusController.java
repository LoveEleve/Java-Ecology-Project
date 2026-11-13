package com.loveeleve.ecology.xxljob.controller;

import com.loveeleve.ecology.xxljob.config.XxlJobProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/xxl-job")
public class XxlJobStatusController {

    private final XxlJobProperties properties;

    public XxlJobStatusController(XxlJobProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "enabled", properties.isEnabled(),
                "appname", properties.getAppname(),
                "adminAddresses", properties.getAdminAddresses(),
                "port", properties.getPort(),
                "logPath", properties.getLogPath());
    }
}
