package com.example.stockapp.controller;

import com.example.stockapp.dto.StockItem;
import com.example.stockapp.dto.TradeType;
import com.example.stockapp.dto.WalletResponse;
import com.example.stockapp.service.WalletService;
import com.example.stockapp.exception.InsufficientStockException;
import com.example.stockapp.exception.StockNotKnownException;
import com.example.stockapp.exception.WalletNotKnownException;
import com.example.stockapp.service.TradeService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;


import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeService tradeService;

    @MockitoBean
    private WalletService walletService;

    @Test
    void rade_buy_returns200() throws Exception{
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BUY\"}"))
                .andExpect(status().isOk());

        verify(tradeService).trade("w1","AAPL", TradeType.BUY);
    }

    @Test
    void trade_sell_returns200() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"SELL\"}"))
                .andExpect(status().isOk());

        verify(tradeService).trade("w1","AAPL", TradeType.SELL);
    }

    @Test
    void trade_lowerCaseType_isAccepted() throws Exception{

        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"buy\"}"))
                .andExpect(status().isOk());

        verify(tradeService).trade("w1","AAPL", TradeType.BUY);
    }

    @Test
    void trad_invalidType_returns400() throws Exception {
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"YOLO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void trade_missingType_returns400() throws Exception{
        mockMvc.perform(post("/wallets/w1/stocks/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
    }

    @Test
    void trade_unknownStock_returns_404() throws Exception{
        doThrow(new StockNotKnownException("ZZZ"))
                .when(tradeService).trade(anyString(),eq("ZZZ"), any());

        mockMvc.perform(post("/wallets/w1/stocks/ZZZ")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BUY\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void trade_insufficientStock_returns400() throws Exception{
        doThrow(new InsufficientStockException("Bank has no 'AAPL' available"))
                .when(tradeService).trade(anyString(), anyString(), any());

        mockMvc.perform(post("/wallets/w1/stocks/ZZZ")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"BUY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWallet_unknown_returnsHoldings() throws Exception {
        WalletResponse response = new WalletResponse("w1", List.of(
                new StockItem("AAPL", 3)
        ));
        when(walletService.getWallet("w1")).thenReturn(response);

        mockMvc.perform(get("/wallets/w1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("w1"))
                .andExpect(jsonPath("$.stocks[0].name").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].quantity").value(3));
    }

    @Test
    void getWallet_unknown_returns404() throws Exception {
        when(walletService.getWallet("ghost"))
                .thenThrow(new WalletNotKnownException("ghost"));

        mockMvc.perform(get("/wallets/ghost"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStockQuantity_unknownWallet_returns404() throws Exception{
        when(walletService.getStockQuantity("ghost", "AAPL"))
                .thenThrow(new WalletNotKnownException("ghost"));

        mockMvc.perform(get("/wallets/ghost/stocks/AAPL"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStockQuantity_returnsBareNumber() throws Exception {
        when(walletService.getStockQuantity("w1","AAPL")).thenReturn(5L);

        mockMvc.perform(get("/wallets/w1/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void getStockQuantity_unknownStock_returns404() throws Exception {
        when(walletService.getStockQuantity("w1", "ZZZ"))
                .thenThrow(new StockNotKnownException("ZZZ"));

        mockMvc.perform(get("/wallets/w1/stocks/ZZZ"))
                .andExpect(status().isNotFound());
    }

}
