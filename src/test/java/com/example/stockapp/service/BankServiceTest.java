package com.example.stockapp.service;

import com.example.stockapp.dto.SetStocksRequest;
import com.example.stockapp.dto.StockHoldings;
import com.example.stockapp.dto.StockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;


import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
 @ExtendWith(MockitoExtension.class)
class BankServiceTest {

     @Mock
     private StringRedisTemplate redis;

     @Mock
     private HashOperations<String, Object, Object> hashOps;

     @Mock
     private SetOperations<String, String> setOps;

     private BankService bankService;

    @BeforeEach
     void setUp(){
        lenient().when(redis.opsForHash()).thenReturn(hashOps);
        lenient().when(redis.opsForSet()).thenReturn(setOps);
        bankService = new BankService(redis);
    }

    @Test
     void setBankStocks_deletesExtension_writesNewEntries_andTracksNames(){
        SetStocksRequest request = new SetStocksRequest(List.of(
                new StockItem("AAPL", 10),
                new StockItem("GOOG", 5)
        ));

        bankService.setBankStocks(request);

        verify(redis).delete(BankService.BANK_STOCKS_HASH);
        verify(redis).delete(BankService.KNOWN_STOCKS_SET);
        verify(hashOps).putAll(eq(BankService.BANK_STOCKS_HASH), eq(Map.of(
                "AAPL", "10",
                "GOOG", "5"
        )));
        verify(setOps).add(BankService.KNOWN_STOCKS_SET, "AAPL", "GOOG");
    }

     @Test
     void getBankStocks_readsHash_andReturnsStockHoldings() {
         when(hashOps.entries(BankService.BANK_STOCKS_HASH))
                 .thenReturn(Map.of("AAPL", "10", "GOOG", "5"));
         StockHoldings result = bankService.getBankStocks();
         assertThat(result.stocks())
                 .extracting(StockItem::name, StockItem::quantity)
                 .containsExactlyInAnyOrder(
                         org.assertj.core.groups.Tuple.tuple("AAPL", 10),
                         org.assertj.core.groups.Tuple.tuple("GOOG", 5)
                 );
     }

     @Test
     void getBankStocks_emptyHash_returnsEmptyHoldings() {
         when(hashOps.entries(BankService.BANK_STOCKS_HASH))
                 .thenReturn(Map.of());
         StockHoldings result = bankService.getBankStocks();
         assertThat(result.stocks()).isEmpty();
     }
}
