package com.example.stockapp.service;

import com.example.stockapp.dto.TradeType;
import com.example.stockapp.exception.InsufficientStockException;
import com.example.stockapp.exception.StockNotKnownException;
import com.example.stockapp.model.AuditEntry;
import com.example.stockapp.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock
    private SetOperations<String,String> setOps;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private RedisScript<Long> tradeScript;

    @Mock
    private AuditLogRepository auditLogRepository;

    private TradeService tradeService;


    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForSet()).thenReturn(setOps);
        tradeService = new TradeService(redis, tradeScript, auditLogRepository);
    }

    @Test
    void trade_buy_success_movesStockFromBankToWallet_andAudits() {


        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);
        when(redis.execute(eq(tradeScript), anyList(), eq("AAPL"))).thenReturn(1L);

        tradeService.trade("w1", "AAPL", TradeType.BUY);

        verify(redis).execute(
                eq(tradeScript),
                eq(List.of(BankService.BANK_STOCKS_HASH, TradeService.walletKey("w1"))),
                eq("AAPL")
        );

        verify(setOps).add(TradeService.KNOWN_WALLET_SET, "w1");

        ArgumentCaptor<AuditEntry> entryCaptor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLogRepository).save(entryCaptor.capture());

        AuditEntry saved = entryCaptor.getValue();
        assertThat(saved.getType()).isEqualTo("buy");
        assertThat(saved.getWalletId()).isEqualTo("w1");
        assertThat(saved.getStockName()).isEqualTo("AAPL");
    }

    @Test
    void trade_sell_success_movesStockFromWalletToBank_andAudits() {
        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);
        when(redis.execute(eq(tradeScript), anyList(), eq("AAPL"))).thenReturn(1L);

        tradeService.trade("w1", "AAPL", TradeType.SELL);

        verify(redis).execute(
                eq(tradeScript),
                eq(List.of(TradeService.walletKey("w1"), BankService.BANK_STOCKS_HASH)),
                eq("AAPL")
        );

        ArgumentCaptor<AuditEntry> entryCaptor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLogRepository).save(entryCaptor.capture());

        assertThat(entryCaptor.getValue().getType()).isEqualTo("sell");
    }


    @Test
    void trade_unknownStock_throws_andDoesNothingElse() {
        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "ZZZ")).thenReturn(false);

        assertThatThrownBy(() -> tradeService.trade("w1", "ZZZ", TradeType.BUY))
                .isInstanceOf(StockNotKnownException.class)
                .hasMessageContaining("ZZZ");

        verify(redis, never()).execute(any(RedisScript.class), anyList(), any());
        verify(auditLogRepository, never()).save(any());
        verify(setOps, never()).add(eq(TradeService.KNOWN_WALLET_SET), any());
    }

    @Test
    void trade_isMemberReturnsNull_throwsStockNotKnown() {
        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(null);


        assertThatThrownBy(() -> tradeService.trade("w1", "AAPL", TradeType.BUY))
                .isInstanceOf(StockNotKnownException.class);
    }

    @Test
    void trade_buy_luaReturnsZero_throwsInsufficient_andDoesNotAudit() {
        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);
        when(redis.execute(eq(tradeScript), anyList(), eq("AAPL"))).thenReturn(0L);

        assertThatThrownBy(() -> tradeService.trade("w1", "AAPL", TradeType.BUY))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("AAPL");

        verify(auditLogRepository, never()).save(any());
        verify(setOps, never()).add(eq(TradeService.KNOWN_WALLET_SET), any());
    }

    @Test
    void trade_sell_luaReturnsZero_throwsInsufficient() {
        when(setOps.isMember(BankService.KNOWN_STOCKS_SET, "AAPL")).thenReturn(true);
        when(redis.execute(eq(tradeScript), anyList(), eq("AAPL"))).thenReturn(0L);


        assertThatThrownBy(() -> tradeService.trade("w1", "AAPL", TradeType.SELL))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("w1")
                .hasMessageContaining("AAPL");
    }

}
