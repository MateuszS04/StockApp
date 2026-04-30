package com.example.stockapp.service;

import com.example.stockapp.dto.StockItem;
import com.example.stockapp.dto.WalletResponse;
import com.example.stockapp.exception.StockNotKnownException;
import com.example.stockapp.exception.WalletNotKnownException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @Mock
    private SetOperations<String, String> setOps;

    private WalletService walletService;

    @BeforeEach
    void setUp(){
        lenient().when(redis.opsForHash()).thenReturn(hashOps);
        lenient().when(redis.opsForSet()).thenReturn(setOps);
        walletService = new WalletService(redis);

    }
    
    @Test
    void getWallet_know_returnsHoldings(){
        when(setOps.isMember(TradeService.KNOWN_WALLET_SET,"w1")).thenReturn(true);
        when(hashOps.entries(TradeService.walletKey("w1")))
                .thenReturn(Map.of("AAPL","3","GOOG",1));
        
        WalletResponse response=walletService.getWallet("w1");
        
        assertThat(response.id()).isEqualTo("w1");
        assertThat(response.stocks()).extracting(StockItem::name, StockItem::quantity)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("AAPL", 3),
                        org.assertj.core.groups.Tuple.tuple("GOOG",1)
                );
    }

    @Test
    void getWallet_unknown_throws() {

        when(setOps.isMember(TradeService.KNOWN_WALLET_SET, "ghost")).thenReturn(false);

        assertThatThrownBy(() -> walletService.getWallet("ghost"))
                .isInstanceOf(WalletNotKnownException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void getWallet_isMemberReturnsNull_throws() {

        when(setOps.isMember(TradeService.KNOWN_WALLET_SET, "ghost")).thenReturn(null);

        assertThatThrownBy(() -> walletService.getWallet("ghost"))
                .isInstanceOf(WalletNotKnownException.class);
    }


    @Test
    void getStockQuantity_knownWalletAndStock_returnsValue() {

        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);

        when(setOps.isMember(TradeService.KNOWN_WALLET_SET, "w1")).thenReturn(true);

        when(hashOps.get(TradeService.walletKey("w1"), "AAPL")).thenReturn("7");

        long result = walletService.getStockQuantity("w1", "AAPL");
        assertThat(result).isEqualTo(7L);
    }

    @Test
    void getStockQuantity_unknownStock_throwsStockNotKnown() {

        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "ZZZ")).thenReturn(false);

        assertThatThrownBy(() -> walletService.getStockQuantity("w1", "ZZZ"))
                .isInstanceOf(StockNotKnownException.class)
                .hasMessageContaining("ZZZ");
    }

    @Test
    void getStockQuantity_unknownWallet_throwsWalletNotKnown() {

        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);

        when(setOps.isMember(TradeService.KNOWN_WALLET_SET, "ghost")).thenReturn(false);

        assertThatThrownBy(() -> walletService.getStockQuantity("ghost", "AAPL"))
                .isInstanceOf(WalletNotKnownException.class);
    }
    @Test
    void getStockQuantity_walletKnownButNeverHadStock_returnsZero() {

        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);

        when(setOps.isMember(TradeService.KNOWN_WALLET_SET, "w1")).thenReturn(true);

        when(hashOps.get(TradeService.walletKey("w1"), "AAPL")).thenReturn(null);

        long result = walletService.getStockQuantity("w1", "AAPL");
        assertThat(result).isZero();
    }
}
