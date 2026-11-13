package com.loveeleve.ecology.xxljob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.xxl-job.enabled=false")
@AutoConfigureMockMvc
class XxlJobStatusControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusExposesExecutorConfigurationWithoutStartingIt() throws Exception {
        mockMvc.perform(get("/api/xxl-job/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.appname").value("java-ecology-xxl-job-executor"))
                .andExpect(jsonPath("$.port").value(9999));
    }
}
