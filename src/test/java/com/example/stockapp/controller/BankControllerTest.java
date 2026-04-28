package com.example.stockapp.controller;

import com.example.stockapp.dto.SetStocksRequest;
import com.example.stockapp.dto.StockHoldings;
import com.example.stockapp.dto.StockItem;
import com.example.stockapp.service.BankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(BankController.class)
class BankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BankService bankService;

    @Test
    void postStocks_validRequest_returns200_andDelegatesToService() throws Exception {
        SetStocksRequest request= new SetStocksRequest(List.of(
                new StockItem("AAPL", 10),
                new StockItem("GOOG", 5)
        ));

        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());

        verify(bankService).setBankStocks(any(SetStocksRequest.class));
    }

    @Test
    void postStocks_emptyStocksList_returns400() throws Exception{
        String body="{\"stocks\":[]";

        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postStocks_blankName_returns400() throws Exception {
        String body = "{\"stocks\":[{\"name\":\"\",\"quantity\":5}]}";

        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void posStocks_negativeQuantity_returns400() throws Exception {
        String body = "{\"stocks\":[{\"name\":\"AAPL\",\"quantity\":-1}]}";

        mockMvc.perform(post("/stocks").contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStocks_returnHoldingsAsJson() throws Exception {

        StockHoldings holdings= new StockHoldings(List.of(
                new StockItem("AAPL", 10),
                new StockItem("GOOG", 5)
        ));

        when(bankService.getBankStocks()).thenReturn(holdings);

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stocks[0].name").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(10))
                .andExpect(jsonPath("$.stocks[1].name").value("GOOG"));
    }
}
