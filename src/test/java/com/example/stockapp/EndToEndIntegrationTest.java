package com.example.stockapp;

import com.example.stockapp.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EndToEndIntegrationTest  extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void resetState(){
        Set<String> keys = redis.keys("*");
        if(keys != null && !keys.isEmpty()){
            redis.delete(keys);
        }
        auditLogRepository.deleteAll();
    }
    @Test
    void fullFlow_setStocks_buy_sell_thenReadEverything() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":2}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stocks[0].name").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(2));

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUY\"}"))
                .andExpect(status().isOk());


        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(get("/stocks"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(1));

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SELL\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(content().string("0"));

        mockMvc.perform(get("/stocks"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(2));

        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.log.length()").value(2))
                .andExpect(jsonPath("$.log[0].type").value("buy"))
                .andExpect(jsonPath("$.log[0].wallet_id").value("w1"))
                .andExpect(jsonPath("$.log[0].stock_name").value("AAPL"))
                .andExpect(jsonPath("$.log[1].type").value("sell"));
    }

    @Test
    void buy_whenBankHasZero_returns400_andLogIsEmpty() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":0}]}"))
                .andExpect(status().isOk());


        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUY\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/log"))
                .andExpect(jsonPath("$.log").isEmpty());
    }

    @Test
    void sell_walletNeverOwnedStock_returns400() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":5}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"SELL\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buy_unknownStock_returns404() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/ZZZ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUY\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWallet_unknown_returns404() throws Exception {
        mockMvc.perform(get("/wallets/ghost"))
                .andExpect(status().isNotFound());
    }


    @Test
    void setStocks_replacesPreviousValues() throws Exception {
        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":10}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stocks\":[{\"name\":\"GOOG\",\"quantity\":5}]}"))
                .andExpect(status().isOk());


        mockMvc.perform(get("/stocks"))
                .andExpect(jsonPath("$.stocks.length()").value(1))
                .andExpect(jsonPath("$.stocks[0].name").value("GOOG"));
    }
}
