package com.loveeleve.ecology.multids;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MultiDataSourceApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesStaticAndDynamicRoutes() throws Exception {
        mockMvc.perform(get("/api/static/master/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].owner").value("master-alice"));

        mockMvc.perform(get("/api/dynamic/report/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].owner").value("report-carol"));

        mockMvc.perform(get("/api/dynamic/accounts").param("source", "REPORT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].owner").value("report-carol"));
    }
}
