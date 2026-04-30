package com.example.stockapp.controller;

import com.example.stockapp.dto.LogEntry;
import com.example.stockapp.dto.LogResponse;
import com.example.stockapp.service.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogController.class)
class LogControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private  LogService logService;

    @Test
    void getLog_returnsAllEntries_inOrder_withSnakeCaseKeys() throws Exception {
        LogResponse response=new LogResponse(List.of(
                new LogEntry("buy","w1","AAPL"),
                new LogEntry("sell", "w2", "GOOG")
        ));
        when(logService.getLog()).thenReturn(response);
        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log[0].type").value("buy"))
                .andExpect(jsonPath("$.log[0].wallet_id").value("w1"))
                .andExpect(jsonPath("$.log[0].stock_name").value("AAPL"))
                .andExpect(jsonPath("$.log[1].type").value("sell"))
                .andExpect(jsonPath("$.log[1].wallet_id").value("w2"))
                .andExpect(jsonPath("$.log[1].stock_name").value("GOOG"));
    }

    @Test
    void getLog_empty_returnsEmptyArray() throws Exception {
        when(logService.getLog()).thenReturn(new LogResponse(List.of()));

        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log").isArray())
                .andExpect(jsonPath("$.log").isEmpty());
    }
}
