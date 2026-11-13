package com.loveeleve.ecology.xxljob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.loveeleve.ecology.xxljob.config.XxlJobProperties;

@EnableConfigurationProperties(XxlJobProperties.class)
@SpringBootApplication
public class XxlJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxlJobApplication.class, args);
    }
}
