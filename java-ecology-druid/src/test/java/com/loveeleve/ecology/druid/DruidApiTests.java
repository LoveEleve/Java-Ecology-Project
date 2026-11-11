package com.loveeleve.ecology.druid;

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
class DruidApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndReadsUserThroughDruidDataSource() throws Exception {
        String username = "druid-" + System.nanoTime();
        String body = """
                {"username":"%s","email":"%s@example.com","age":30}
                """.formatted(username, username);

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(username))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.data.id")).longValue();

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));

        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber());

        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exposesDruidPoolStats() throws Exception {
        mockMvc.perform(get("/api/pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("ecology-druid-pool"))
                .andExpect(jsonPath("$.data.maxActive").value(20))
                .andExpect(jsonPath("$.data.activeCount").isNumber());
    }

    @Test
    void statViewIsDisabledByDefault() throws Exception {
        mockMvc.perform(get("/druid/index.html"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validatesRequestBody() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
