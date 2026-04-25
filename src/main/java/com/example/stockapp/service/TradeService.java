package com.example.stockapp.service;

import com.example.stockapp.dto.TradeType;
import com.example.stockapp.exception.InsufficientStockException;
import com.example.stockapp.exception.StockNotKnownException;
import com.example.stockapp.model.AuditEntry;
import com.example.stockapp.repository.AuditLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeService {
    public static final String KNOWN_WALLET_SET="known_wallets";
    public static final String WALLET_STOCKS_HASH_PREFIX="wallet:";
    public static final String WALLET_STOCKS_HASH_SUFFIX=":stocks";

    private final StringRedisTemplate redis;
    private final RedisScript<Long> tradeScript;
    private final AuditLogRepository auditLogRepository;

    public TradeService(StringRedisTemplate redis,
                        RedisScript<Long> tradeScript,
                        AuditLogRepository auditLogRepository){
        this.redis=redis;
        this.tradeScript=tradeScript;
        this.auditLogRepository=auditLogRepository;
    }
    /**
     * Executes one buy or sell of single share.
     * Order of operations:
     *  1. Verify the stock name is known to the bank (if not 404)
     *  2. Run the atomic Lua script to move 1 share (return 0 if trade is insufficient -> 404)
     *  3. Track the wallet as known    (for GET ?wallets?{id} later)
     *  4. Record an audit row in Postgres
     *
     *
     *  steps 3 nad 4 run only if redis succeed
     *  audit log contains successful trades
     */

    @Transactional
    public void trade(String walletId, String stockName, TradeType type){
        Boolean isKnown= redis.opsForSet().isMember(BankService.KNOWN_STOCKS_SET,stockName);
        if (isKnown==null || !isKnown){
            throw new StockNotKnownException(stockName);
        }

        String walletKey=walletKey(walletId);
        String sourceKey= (type==TradeType.BUY)?BankService.BANK_STOCKS_HASH:walletKey;
        String destKey = (type==TradeType.BUY)? walletKey:BankService.BANK_STOCKS_HASH;

        Long result= redis.execute(tradeScript, List.of(sourceKey,destKey),stockName);

        if(result != 1L){
            String message=(type==TradeType.BUY)
                    ?"Bank has no '" + stockName + "'avaible"
                    : "Wallet '" + walletId +"'has no '"+stockName +"' to sell";
            throw new InsufficientStockException(message);
        }

        redis.opsForSet().add(KNOWN_WALLET_SET,walletId);

        auditLogRepository.save(new AuditEntry(type.name().toLowerCase(), walletId, stockName));

    }

    private String walletKey(String walletId) {
        return WALLET_STOCKS_HASH_PREFIX + walletId + WALLET_STOCKS_HASH_SUFFIX;
    }
}
