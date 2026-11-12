package com.loveeleve.ecology.nacos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NacosControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void localConfigWorksWithoutNacos() throws Exception {
        mockMvc.perform(get("/api/nacos/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("local-default"))
                .andExpect(jsonPath("$.data.group").value("ECO_ECOLOGY"));
    }

    @Test
    void discoveryReturnsEmptyWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/nacos/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void configOperationsFailClearlyWhenDisabled() throws Exception {
        mockMvc.perform(get("/api/nacos/remote-config").param("dataId", "demo.yaml"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NACOS_UNAVAILABLE"));

        mockMvc.perform(post("/api/nacos/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataId\":\"demo.yaml\",\"group\":\"ECO_ECOLOGY\",\"content\":\"a: 1\",\"type\":\"yaml\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NACOS_UNAVAILABLE"));
    }

    @Test
    void validatesPublishRequest() throws Exception {
        mockMvc.perform(post("/api/nacos/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataId\":\"\",\"group\":\"\",\"content\":\"\",\"type\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void healthEndpointIsUpWithoutNacos() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
